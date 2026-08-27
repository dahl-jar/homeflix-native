import {
    reportPlaybackStart,
    reportPlaybackProgress,
    reportPlaybackStop
} from './playbackApi.js';

const DEFAULT_PROGRESS_INTERVAL_MS = 10_000;

function reportPayload(context, snapshot) {
    return {
        ItemId: context.itemId,
        MediaSourceId: context.mediaSourceId,
        PlaySessionId: context.playSessionId,
        PlaybackPipelineId: context.pipelineId,
        PlaybackAttemptId: context.attemptId,
        PlayMethod: context.playMethod,
        AudioStreamIndex: context.audioStreamIndex,
        SubtitleStreamIndex: context.subtitleStreamIndex,
        CanSeek: true,
        IsPaused: snapshot.isPaused,
        IsMuted: false,
        PositionTicks: snapshot.positionTicks
    };
}

export function createPlaybackReporter({
    client,
    context,
    telemetry = null,
    now = Date.now,
    intervalMs = DEFAULT_PROGRESS_INTERVAL_MS
}) {
    let started = false;
    let stopped = false;
    let lastProgressAt = 0;

    async function send(reason, request, payload) {
        try {
            await request(client, payload);
            return true;
        } catch {
            telemetry?.log('reporting_failed', { reason });
            return false;
        }
    }

    return {
        async start(snapshot) {
            if (started || stopped) return false;
            const sent = await send('playback_start', reportPlaybackStart, reportPayload(context, snapshot));
            if (sent) {
                started = true;
                lastProgressAt = now();
            }
            return sent;
        },
        async progress(snapshot, { force = false } = {}) {
            if (!started || stopped) return false;
            const currentTime = now();
            if (!force && currentTime - lastProgressAt < intervalMs) return false;
            const sent = await send('playback_progress', reportPlaybackProgress, reportPayload(context, snapshot));
            if (sent) lastProgressAt = currentTime;
            return sent;
        },
        async stop(snapshot) {
            if (stopped) return false;
            stopped = true;
            return send('playback_stop', reportPlaybackStop, {
                ...reportPayload(context, snapshot),
                Failed: snapshot.failed === true
            });
        }
    };
}
