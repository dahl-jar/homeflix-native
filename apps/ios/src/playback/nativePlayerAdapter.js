import { sanitizeTelemetryFields } from './telemetrySanitizer.js';

const TIME_UPDATE_INTERVAL_SECONDS = 1;
const MINIMUM_PLAYBACK_ADVANCE_SECONDS = 0.1;

function nativeError(error) {
    return sanitizeTelemetryFields({
        errorType: 'native_player',
        errorName: 'PlayerError',
        errorMessage: error?.message ?? 'native player failed'
    });
}

function pauseForDisposal(player) {
    try {
        const result = player.pause();
        if (typeof result?.catch === 'function') void result.catch(() => {});
    } catch {}
}

function playerValue(player, property, fallback) {
    try {
        return player[property];
    } catch {
        return fallback;
    }
}

export function bindNativePlayer(player, callbacks) {
    let disposed = false;
    let lastSnapshot = {
        positionSeconds: 0,
        durationSeconds: 0,
        isPaused: true
    };
    player.timeUpdateEventInterval = TIME_UPDATE_INTERVAL_SECONDS;
    const subscriptions = [
        player.addListener('statusChange', ({ status, error }) => {
            if (status === 'readyToPlay') {
                const durationSeconds = playerValue(
                    player,
                    'duration',
                    lastSnapshot.durationSeconds
                );
                lastSnapshot = { ...lastSnapshot, durationSeconds };
                callbacks.onReady?.({ durationSeconds });
            } else if (status === 'error') {
                callbacks.onError?.(nativeError(error));
            }
        }),
        player.addListener('playingChange', ({ isPlaying }) => {
            lastSnapshot = { ...lastSnapshot, isPaused: !isPlaying };
            callbacks.onPlayingChange?.(isPlaying);
        }),
        player.addListener('timeUpdate', ({ currentTime, bufferedPosition }) => {
            const playbackAdvanced = currentTime
                > lastSnapshot.positionSeconds + MINIMUM_PLAYBACK_ADVANCE_SECONDS;
            const durationSeconds = playerValue(
                player,
                'duration',
                lastSnapshot.durationSeconds
            );
            lastSnapshot = {
                ...lastSnapshot,
                positionSeconds: currentTime,
                durationSeconds
            };
            callbacks.onTimeUpdate?.({
                positionSeconds: currentTime,
                durationSeconds,
                bufferedSeconds: bufferedPosition,
                playbackAdvanced
            });
        }),
        player.addListener('sourceLoad', (payload) => {
            lastSnapshot = { ...lastSnapshot, durationSeconds: payload.duration };
            callbacks.onSourceLoad?.({
                durationSeconds: payload.duration,
                audioTracks: payload.availableAudioTracks,
                subtitleTracks: payload.availableSubtitleTracks,
                selectedAudioTrack: playerValue(player, 'audioTrack', null),
                selectedSubtitleTrack: playerValue(player, 'subtitleTrack', null)
            });
        }),
        player.addListener('audioTrackChange', ({ audioTrack }) => {
            callbacks.onAudioTrackChange?.(audioTrack);
        }),
        player.addListener('subtitleTrackChange', ({ subtitleTrack }) => {
            callbacks.onSubtitleTrackChange?.(subtitleTrack);
        }),
        player.addListener('playToEnd', () => callbacks.onEnded?.())
    ];

    return {
        async load(source, startSeconds = 0) {
            if (disposed) return;
            await player.replaceAsync(source);
            if (disposed) return;
            player.currentTime = startSeconds;
            player.play();
            lastSnapshot = {
                ...lastSnapshot,
                positionSeconds: startSeconds,
                isPaused: false
            };
        },
        play() {
            player.play();
            lastSnapshot = { ...lastSnapshot, isPaused: false };
        },
        pause() {
            player.pause();
            lastSnapshot = { ...lastSnapshot, isPaused: true };
        },
        seekBy(seconds) {
            player.seekBy(seconds);
            lastSnapshot = {
                ...lastSnapshot,
                positionSeconds: playerValue(
                    player,
                    'currentTime',
                    lastSnapshot.positionSeconds + seconds
                )
            };
        },
        seekTo(seconds) {
            player.currentTime = seconds;
            lastSnapshot = { ...lastSnapshot, positionSeconds: seconds };
        },
        selectAudioTrack(track) {
            player.audioTrack = track;
        },
        selectSubtitleTrack(track) {
            player.subtitleTrack = track;
        },
        snapshot() {
            lastSnapshot = {
                positionSeconds: playerValue(
                    player,
                    'currentTime',
                    lastSnapshot.positionSeconds
                ),
                durationSeconds: playerValue(
                    player,
                    'duration',
                    lastSnapshot.durationSeconds
                ),
                isPaused: !playerValue(player, 'playing', !lastSnapshot.isPaused)
            };
            return { ...lastSnapshot };
        },
        dispose() {
            if (disposed) return;
            disposed = true;
            pauseForDisposal(player);
            subscriptions.forEach((subscription) => subscription.remove());
        }
    };
}
