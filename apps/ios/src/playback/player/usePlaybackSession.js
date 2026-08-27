import { useVideoPlayer } from 'expo-video';
import { useCallback, useEffect, useRef, useState } from 'react';

import { createPlaybackProgress } from '../pipeline/playbackProgress.js';
import { createPlaybackRuntime } from '../runtime/playbackRuntime.js';
import { createPlaybackRuntimeLease } from '../runtime/playbackRuntimeLease.js';
import { playbackRuntimeRegistry } from '../runtime/playbackRuntimeRegistry.js';

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

export function usePlaybackSession(options) {
    const {
        active = true,
        client,
        item,
        platform,
        preferredMediaSourceId,
        serverUrl,
        startTimeTicks,
        userId
    } = options;
    const player = useVideoPlayer(null, (instance) => {
        instance.staysActiveInBackground = false;
        instance.showNowPlayingNotification = false;
        instance.allowsExternalPlayback = true;
    });
    const runtimeRef = useRef(null);
    const leaseRef = useRef(null);
    const [snapshot, setSnapshot] = useState(INITIAL_SNAPSHOT);
    if (leaseRef.current == null) {
        leaseRef.current = createPlaybackRuntimeLease({
            createRuntime: createPlaybackRuntime,
            registry: playbackRuntimeRegistry
        });
    }

    useEffect(() => {
        if (!active) return undefined;
        const runtime = leaseRef.current.acquire({
            player,
            negotiationOptions: {
                client,
                item,
                platform,
                preferredMediaSourceId,
                serverUrl,
                startTimeTicks,
                userId
            }
        }, setSnapshot, createPlaybackRuntime);
        runtimeRef.current = runtime;

        return () => {
            if (runtimeRef.current === runtime) runtimeRef.current = null;
            void leaseRef.current.release(runtime);
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
            return runtime ? leaseRef.current.deactivate(runtime) : undefined;
        }, [])
    };
}
