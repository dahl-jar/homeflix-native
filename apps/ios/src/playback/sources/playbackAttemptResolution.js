import { watchPlaybackProgress } from '../pipeline/playbackProgressWatcher.js';

import { resolvePlaybackAttempt } from './playbackApi.js';

function singleSource(mediaSources) {
    return mediaSources?.length === 1 ? mediaSources[0] : null;
}

function optionalText(value) {
    return typeof value === 'string' ? value : undefined;
}

function optionalNumber(value) {
    return Number.isFinite(value) ? value : undefined;
}

function hasCompleteResolution({
    source,
    pipelineHandle,
    pipelineDecision,
    audioStreamIndex,
    subtitleStreamIndex
}) {
    return Boolean(source)
        && Boolean(pipelineHandle)
        && Number.isInteger(audioStreamIndex)
        && Number.isInteger(subtitleStreamIndex)
        && typeof pipelineDecision === 'string'
        && pipelineDecision.length > 0;
}

function resolvedAttempt(playbackInfo) {
    const source = singleSource(playbackInfo.MediaSources);
    const pipelineHandle = playbackInfo.PlaybackPipelineHandle;
    const audioStreamIndex = playbackInfo.PlaybackPipelineAudioStreamIndex;
    const subtitleStreamIndex = playbackInfo.PlaybackPipelineSubtitleStreamIndex;
    const pipelineDecision = playbackInfo.PlaybackPipelineDecision;
    if (!hasCompleteResolution({
        source,
        pipelineHandle,
        pipelineDecision,
        audioStreamIndex,
        subtitleStreamIndex
    })) return null;
    return {
        mediaSource: source,
        pipelineHandle,
        pipelineDecision,
        audioStreamIndex,
        subtitleStreamIndex,
        sourceCount: playbackInfo.PlaybackPipelineSourceCount ?? 1,
        videoDelivery: optionalText(playbackInfo.PlaybackPipelineVideoDelivery),
        audioDelivery: optionalText(playbackInfo.PlaybackPipelineAudioDelivery),
        sourceWidth: optionalNumber(playbackInfo.PlaybackPipelineSourceWidth),
        sourceHeight: optionalNumber(playbackInfo.PlaybackPipelineSourceHeight)
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
