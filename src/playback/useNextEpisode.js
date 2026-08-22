import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { fetchFollowingEpisodeCandidates } from './episodeQueueApi.js';
import { selectFollowingEpisode } from './episodeQueuePolicy.js';

const COUNTDOWN_SECONDS = 10;

export function useNextEpisode({ client, item, activeSegment, playbackStatus, userId, onAdvance }) {
    const [nextState, setNextState] = useState({ itemId: null, item: null });
    const [countdownState, setCountdownState] = useState({ key: null, remaining: COUNTDOWN_SECONDS, cancelled: false });
    const advancedKeyRef = useRef(null);

    useEffect(() => {
        if (!client || !userId || item.Type !== 'Episode' || !item.SeriesId) return;
        fetchFollowingEpisodeCandidates(client, {
            userId,
            seriesId: item.SeriesId,
            itemId: item.Id
        })
            .then((items) => setNextState({
                itemId: item.Id,
                item: selectFollowingEpisode(items, item.Id)
            }))
            .catch(() => setNextState({ itemId: item.Id, item: null }));
    }, [client, item, userId]);

    const nextEpisode = nextState.itemId === item.Id ? nextState.item : null;
    const triggered = activeSegment?.type === 'Outro' || playbackStatus === 'ended';
    const countdownKey = triggered && nextEpisode
        ? `${item.Id}:${activeSegment?.id ?? 'ended'}`
        : null;
    const countdown = useMemo(() => countdownState.key === countdownKey
        ? countdownState
        : { key: countdownKey, remaining: COUNTDOWN_SECONDS, cancelled: false }, [
        countdownKey,
        countdownState
    ]);

    useEffect(() => {
        if (!countdownKey || countdown.cancelled || countdown.remaining <= 0) return undefined;
        const timer = setInterval(() => {
            setCountdownState((current) => {
                const active = current.key === countdownKey
                    ? current
                    : { key: countdownKey, remaining: COUNTDOWN_SECONDS, cancelled: false };
                return { ...active, remaining: Math.max(0, active.remaining - 1) };
            });
        }, 1_000);
        return () => clearInterval(timer);
    }, [countdown.cancelled, countdown.remaining, countdownKey]);

    useEffect(() => {
        if (!countdownKey || !nextEpisode || countdown.cancelled || countdown.remaining > 0) return;
        if (advancedKeyRef.current === countdownKey) return;
        advancedKeyRef.current = countdownKey;
        onAdvance(nextEpisode);
    }, [countdown.cancelled, countdown.remaining, countdownKey, nextEpisode, onAdvance]);

    const cancel = useCallback(() => {
        if (!countdownKey) return;
        setCountdownState({ ...countdown, cancelled: true });
    }, [countdown, countdownKey]);

    const advanceKey = countdownKey ?? (nextEpisode ? item.Id : null);
    const playNext = useCallback(() => {
        if (!advanceKey || !nextEpisode || advancedKeyRef.current === advanceKey) return;
        advancedKeyRef.current = advanceKey;
        onAdvance(nextEpisode);
    }, [advanceKey, nextEpisode, onAdvance]);

    return {
        active: Boolean(countdownKey) && !countdown.cancelled,
        cancel,
        nextEpisode,
        playNext,
        remainingSeconds: countdown.remaining
    };
}
