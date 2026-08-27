import { resolvePlaybackAttempt } from './playbackApi.js';
import { watchPlaybackProgress } from './playbackProgressWatcher.js';

function resolvedAttempt(playbackInfo) {
    const source = playbackInfo.MediaSources?.length === 1
        ? playbackInfo.MediaSources[0]
        : null;
    const audioStreamIndex = playbackInfo.PlaybackPipelineAudioStreamIndex;
    const subtitleStreamIndex = playbackInfo.PlaybackPipelineSubtitleStreamIndex;
    const pipelineDecision = playbackInfo.PlaybackPipelineDecision;
    if (!source) return null;
    if (!playbackInfo.PlaybackPipelineHandle) return null;
    if (!Number.isInteger(audioStreamIndex)) return null;
    if (!Number.isInteger(subtitleStreamIndex)) return null;
    if (typeof pipelineDecision !== 'string' || pipelineDecision.length === 0) return null;
    return {
        mediaSource: source,
        pipelineHandle: playbackInfo.PlaybackPipelineHandle,
        pipelineDecision,
        audioStreamIndex,
        subtitleStreamIndex,
        sourceCount: playbackInfo.PlaybackPipelineSourceCount ?? 1,
        videoDelivery: typeof playbackInfo.PlaybackPipelineVideoDelivery === 'string'
            ? playbackInfo.PlaybackPipelineVideoDelivery
            : undefined,
        audioDelivery: typeof playbackInfo.PlaybackPipelineAudioDelivery === 'string'
            ? playbackInfo.PlaybackPipelineAudioDelivery
            : undefined,
        sourceWidth: Number.isFinite(playbackInfo.PlaybackPipelineSourceWidth)
            ? playbackInfo.PlaybackPipelineSourceWidth
            : undefined,
        sourceHeight: Number.isFinite(playbackInfo.PlaybackPipelineSourceHeight)
            ? playbackInfo.PlaybackPipelineSourceHeight
            : undefined
    };
}

export async function resolvePlaybackAttemptWithProgress({
    attempt,
    options,
    pipeline,
    request,
    onProgress
}) {
    const watchProgress = options.watchProgress ?? watchPlaybackProgress;
    const watcher = watchProgress({
        client: options.client,
        pipelineId: pipeline.pipelineId,
        attemptId: attempt.attemptId,
        onProgress
    });
    let playbackInfo;
    try {
        playbackInfo = await resolvePlaybackAttempt(options.client, request);
    } finally {
        await watcher.stop();
    }
    if (playbackInfo.ErrorCode || playbackInfo.MediaSources?.length === 0) return null;
    const resolution = resolvedAttempt(playbackInfo);
    if (!resolution) throw new Error('incomplete playback resolution');
    return resolution;
}
