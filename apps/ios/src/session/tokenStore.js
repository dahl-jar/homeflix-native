import { setItemAsync, getItemAsync, deleteItemAsync } from 'expo-secure-store';

const KEY = 'homeflix-session';

/** The access token is a credential; it lives in the device keychain only. */
export async function saveSession({ serverUrl, userId, accessToken }) {
    await setItemAsync(KEY, JSON.stringify({ serverUrl, userId, accessToken }));
}

export async function loadSession() {
    const raw = await getItemAsync(KEY);
    return raw ? JSON.parse(raw) : null;
}

export async function clearSession() {
    await deleteItemAsync(KEY);
}
