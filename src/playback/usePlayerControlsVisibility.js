import { useCallback, useEffect, useRef, useState } from 'react';

const AUTO_HIDE_DELAY_MS = 3_500;

export function usePlayerControlsVisibility(playbackStatus) {
    const [visibility, setVisibility] = useState({ hidden: false, revision: 0 });
    const timerRef = useRef(null);

    const clearTimer = useCallback(() => {
        if (timerRef.current) clearTimeout(timerRef.current);
        timerRef.current = null;
    }, []);

    const show = useCallback(() => {
        clearTimer();
        setVisibility((current) => ({ hidden: false, revision: current.revision + 1 }));
    }, [clearTimer]);

    useEffect(() => {
        clearTimer();
        if (playbackStatus !== 'playing' || visibility.hidden) return undefined;
        timerRef.current = setTimeout(() => {
            setVisibility((current) => ({ ...current, hidden: true }));
        }, AUTO_HIDE_DELAY_MS);
        return clearTimer;
    }, [clearTimer, playbackStatus, visibility.hidden, visibility.revision]);

    const visible = playbackStatus !== 'playing' || !visibility.hidden;

    const toggle = useCallback(() => {
        if (visible) {
            clearTimer();
            setVisibility((current) => ({ ...current, hidden: true }));
        } else {
            show();
        }
    }, [clearTimer, show, visible]);

    return { show, toggle, visible };
}
