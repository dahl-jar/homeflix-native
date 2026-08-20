import { createContext, useContext, useEffect, useMemo, useState } from 'react';

import { createClient } from '../api/client.js';
import { fetchMe } from '../api/auth.js';
import { resolveServer } from './serverResolver.js';
import { saveSession, loadSession, clearSession } from './tokenStore.js';

const SessionContext = createContext(null);

const STATUS = {
    restoring: 'restoring',
    signedOut: 'signedOut',
    signedIn: 'signedIn',
    unreachable: 'unreachable'
};

export { STATUS as SESSION_STATUS };

export function SessionProvider({ children }) {
    const [status, setStatus] = useState(STATUS.restoring);
    const [serverUrl, setServerUrl] = useState(null);
    const [session, setSession] = useState(null);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            const resolved = await resolveServer();
            if (cancelled) return;
            if (!resolved) {
                setStatus(STATUS.unreachable);
                return;
            }
            setServerUrl(resolved);
            const saved = await loadSession();
            if (cancelled) return;
            if (!saved) {
                setStatus(STATUS.signedOut);
                return;
            }
            try {
                const client = createClient({ baseUrl: resolved, token: saved.accessToken });
                const me = await fetchMe(client);
                if (!cancelled) {
                    setSession({ userId: saved.userId, accessToken: saved.accessToken, user: me });
                    setStatus(STATUS.signedIn);
                }
            } catch {
                await clearSession();
                if (!cancelled) setStatus(STATUS.signedOut);
            }
        })();
        return () => {
            cancelled = true;
        };
    }, []);

    const value = useMemo(() => {
        const client =
            serverUrl && session
                ? createClient({ baseUrl: serverUrl, token: session.accessToken })
                : serverUrl
                    ? createClient({ baseUrl: serverUrl, token: '' })
                    : null;
        return {
            status,
            serverUrl,
            client,
            userId: session?.userId ?? null,
            user: session?.user ?? null,
            async signIn(authResult) {
                const next = {
                    serverUrl,
                    userId: authResult.User.Id,
                    accessToken: authResult.AccessToken
                };
                await saveSession(next);
                setSession({ userId: next.userId, accessToken: next.accessToken, user: authResult.User });
                setStatus(STATUS.signedIn);
            },
            async signOut() {
                await clearSession();
                setSession(null);
                setStatus(STATUS.signedOut);
            },
            retryResolve() {
                setStatus(STATUS.restoring);
                resolveServer().then((resolved) => {
                    setServerUrl(resolved);
                    setStatus(resolved ? STATUS.signedOut : STATUS.unreachable);
                });
            }
        };
    }, [status, serverUrl, session]);

    return <SessionContext value={value}>{children}</SessionContext>;
}

export function useSession() {
    const value = useContext(SessionContext);
    if (!value) throw new Error('useSession requires SessionProvider');
    return value;
}
