import { releasePlaybackSource } from './playbackApi.js';
import { playbackMethod } from './playbackMethod.js';
import { createPlaybackRequestPolicy } from './playbackRequestPolicy.js';
import { createVideoSource } from './videoSource.js';

function sourceById(playbackInfo, mediaSourceId) {
    return playbackInfo.MediaSources?.find((source) => source.Id === mediaSourceId) ?? null;
}

export async function releaseResolvedPlayback({
    attempt,
    options,
    pipeline,
    request,
    resolution
}) {
    const releaseRequest = {
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
    const video = createVideoSource({
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
        playbackInfo,
        playMethod,
        tracks,
        video
    };
}
