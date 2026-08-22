import { useVideoPlayer } from 'expo-video';
import { useCallback, useEffect, useRef, useState } from 'react';

import { createPlaybackProgress } from './pipeline/playbackProgress.js';
import { createPlaybackRuntime } from './playbackRuntime.js';
import { playbackRuntimeRegistry } from './playbackRuntimeRegistry.js';

const INITIAL_SNAPSHOT = {
    status: 'idle',
    positionSeconds: 0,
    durationSeconds: 0,
    bufferedSeconds: 0,
    audioTracks: [],
    subtitleTracks: [],
    selectedAudioTrack: null,
    selectedSubtitleTrack: null,
    pipeline: createPlaybackProgress(),
    reason: null
};

export function usePlaybackSession({
    active = true,
    client,
    item,
    platform,
    preferredMediaSourceId,
    serverUrl,
    startTimeTicks,
    userId
}) {
    const player = useVideoPlayer(null, (instance) => {
        instance.staysActiveInBackground = false;
        instance.showNowPlayingNotification = false;
        instance.allowsExternalPlayback = true;
    });
    const runtimeRef = useRef(null);
    const [snapshot, setSnapshot] = useState(INITIAL_SNAPSHOT);

    useEffect(() => {
        if (!active) return undefined;
        let mounted = true;
        const runtime = createPlaybackRuntime({
            player,
            negotiationOptions: {
                client,
                item,
                platform,
                preferredMediaSourceId,
                serverUrl,
                startTimeTicks,
                userId
            },
            onSnapshot: (next) => {
                if (mounted) setSnapshot(next);
            }
        });
        runtimeRef.current = runtime;
        void playbackRuntimeRegistry.activate(runtime).catch(() => {});

        return () => {
            mounted = false;
            if (runtimeRef.current === runtime) runtimeRef.current = null;
            void playbackRuntimeRegistry.deactivate(runtime);
        };
    }, [
        active,
        client,
        item,
        platform,
        preferredMediaSourceId,
        serverUrl,
        startTimeTicks,
        userId,
        player
    ]);

    return {
        player,
        snapshot,
        play: useCallback(() => runtimeRef.current?.play(), []),
        pause: useCallback(() => runtimeRef.current?.pause(), []),
        seekBy: useCallback((seconds) => runtimeRef.current?.seekBy(seconds), []),
        seekTo: useCallback((seconds) => runtimeRef.current?.seekTo(seconds), []),
        selectAudioTrack: useCallback((track) => runtimeRef.current?.selectAudioTrack(track), []),
        selectSubtitleTrack: useCallback((track) => runtimeRef.current?.selectSubtitleTrack(track), []),
        stop: useCallback(() => {
            const runtime = runtimeRef.current;
            return runtime ? playbackRuntimeRegistry.deactivate(runtime) : undefined;
        }, [])
    };
}
