import { createVideoSource } from '../video/videoSource.js';

import { releasePlaybackSource } from './playbackApi.ts';
import { playbackMethod } from './playbackMethod.ts';
import { createPlaybackRequestPolicy } from './playbackRequestPolicy.ts';
import type {
    PlaybackAttempt,
    PlaybackInfo,
    PlaybackOptions,
    PlaybackPipeline,
    PlaybackRequest,
    PlaybackResolution,
    ReleasePlaybackRequest,
    ReleasedPlayback,
    VideoSource
} from './playbackTypes.ts';

function sourceById(playbackInfo: PlaybackInfo, mediaSourceId: string) {
    return playbackInfo.MediaSources?.find((source) => source.Id === mediaSourceId) ?? null;
}

export async function releaseResolvedPlayback({
    attempt,
    options,
    pipeline,
    request,
    resolution
}: {
    attempt: PlaybackAttempt;
    options: PlaybackOptions;
    pipeline: PlaybackPipeline;
    request: PlaybackRequest;
    resolution: PlaybackResolution;
}): Promise<ReleasedPlayback> {
    const releaseRequest: ReleasePlaybackRequest = {
        ...request,
        ...createPlaybackRequestPolicy(options.platform, resolution.mediaSource),
        mediaSourceId: resolution.mediaSource.Id,
        pipelineHandle: resolution.pipelineHandle,
        pipelineDecision: resolution.pipelineDecision,
        audioStreamIndex: resolution.audioStreamIndex,
        subtitleStreamIndex: resolution.subtitleStreamIndex
    };
    const playbackInfo = await releasePlaybackSource(options.client, releaseRequest);
    const mediaSource = sourceById(playbackInfo, resolution.mediaSource.Id);
    if (!mediaSource || !playbackInfo.PlaySessionId) {
        throw new Error('release did not return one playable source');
    }
    const playMethod = playbackMethod(mediaSource) ?? playbackMethod(resolution.mediaSource);
    if (!playMethod) throw new Error('released source has no playback method');
    const tracks = {
        audioStreamIndex: resolution.audioStreamIndex,
        subtitleStreamIndex: resolution.subtitleStreamIndex
    };
    const video: VideoSource = createVideoSource({
        serverUrl: options.serverUrl,
        itemId: options.item.Id,
        playbackInfo,
        mediaSource,
        tracks,
        mediaHeaders: options.client.mediaHeaders,
        pipelineId: pipeline.pipelineId,
        attemptId: attempt.attemptId,
        playMethod
    });
    return {
        mediaSource,
        playbackInfo: { ...playbackInfo, PlaySessionId: playbackInfo.PlaySessionId },
        playMethod,
        tracks,
        video
    };
}
