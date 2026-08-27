import { watchPlaybackProgress } from '../pipeline/playbackProgressWatcher.js';

import { resolvePlaybackAttempt } from './playbackApi.ts';
import type {
    MediaSource,
    PlaybackAttempt,
    PlaybackInfo,
    PlaybackOptions,
    PlaybackPipeline,
    PlaybackRequest,
    PlaybackResolution,
    PipelineProgressEvent,
    ProgressWatcher
} from './playbackTypes.ts';

const defaultProgressWatcher = watchPlaybackProgress as unknown as ProgressWatcher;

function singleSource(mediaSources: MediaSource[] | undefined) {
    return mediaSources?.length === 1 ? mediaSources[0] : null;
}

function optionalText(value: unknown) {
    return typeof value === 'string' ? value : undefined;
}

function optionalNumber(value: unknown) {
    return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}

function integer(value: unknown): value is number {
    return typeof value === 'number' && Number.isInteger(value);
}

function resolvedAttempt(playbackInfo: PlaybackInfo): PlaybackResolution | null {
    const mediaSource = singleSource(playbackInfo.MediaSources);
    const pipelineHandle = playbackInfo.PlaybackPipelineHandle;
    const pipelineDecision = playbackInfo.PlaybackPipelineDecision;
    const audioStreamIndex = playbackInfo.PlaybackPipelineAudioStreamIndex;
    const subtitleStreamIndex = playbackInfo.PlaybackPipelineSubtitleStreamIndex;
    if (
        !mediaSource
        || typeof pipelineHandle !== 'string'
        || pipelineHandle.length === 0
        || typeof pipelineDecision !== 'string'
        || pipelineDecision.length === 0
        || !integer(audioStreamIndex)
        || !integer(subtitleStreamIndex)
    ) return null;
    return {
        mediaSource,
        pipelineHandle,
        pipelineDecision,
        audioStreamIndex,
        subtitleStreamIndex,
        sourceCount: optionalNumber(playbackInfo.PlaybackPipelineSourceCount) ?? 1,
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
}: {
    attempt: PlaybackAttempt;
    options: PlaybackOptions;
    pipeline: PlaybackPipeline;
    request: PlaybackRequest;
    onProgress: (event: PipelineProgressEvent) => void;
}): Promise<PlaybackResolution | null> {
    const watchProgress = options.watchProgress ?? defaultProgressWatcher;
    const watcher = watchProgress({
        client: options.client,
        pipelineId: pipeline.pipelineId,
        attemptId: attempt.attemptId,
        onProgress
    });
    let playbackInfo: PlaybackInfo;
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
