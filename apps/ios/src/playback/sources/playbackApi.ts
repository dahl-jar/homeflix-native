import type { ApiClient } from '../../api/client/client.ts';

import type {
    PlaybackBaseRequest,
    PlaybackInfo,
    PlaybackRequest,
    ReleasePlaybackRequest
} from './playbackTypes.ts';

function playbackBody(request: PlaybackBaseRequest) {
    return {
        UserId: request.userId,
        DeviceProfile: request.deviceProfile,
        StartTimeTicks: request.startTimeTicks,
        EnableDirectPlay: request.enableDirectPlay ?? true,
        EnableDirectStream: request.enableDirectStream ?? true,
        EnableTranscoding: true,
        AllowVideoStreamCopy: request.allowVideoStreamCopy ?? true,
        AllowAudioStreamCopy: request.allowAudioStreamCopy ?? true
    };
}

export function resolvePlaybackAttempt(client: ApiClient, request: PlaybackRequest) {
    const override = request.trackOverride;
    return client.post<PlaybackInfo>(`/Items/${request.itemId}/PlaybackInfo`, {
        ...playbackBody(request),
        PlaybackPipelineId: request.pipelineId,
        PlaybackAttemptId: request.attemptId,
        PlaybackPipelineResolve: true,
        PlaybackRejectedSourceIds: [...(request.rejectedSourceIds ?? [])],
        PlaybackPreferredMediaSourceId: request.preferredMediaSourceId ?? null,
        ...(override ? {
            PlaybackPipelineTrackOverride: true,
            AudioStreamIndex: override.audioStreamIndex,
            SubtitleStreamIndex: override.subtitleStreamIndex
        } : {})
    });
}

export function releasePlaybackSource(client: ApiClient, request: ReleasePlaybackRequest) {
    return client.post<PlaybackInfo>(`/Items/${request.itemId}/PlaybackInfo`, {
        ...playbackBody(request),
        MediaSourceId: request.mediaSourceId,
        PlaybackPipelineId: request.pipelineId,
        PlaybackAttemptId: request.attemptId,
        PlaybackPipelineHandle: request.pipelineHandle,
        PlaybackPipelineAccepted: true,
        PlaybackPipelineDecision: request.pipelineDecision,
        AudioStreamIndex: request.audioStreamIndex,
        SubtitleStreamIndex: request.subtitleStreamIndex
    });
}

export function reportPlaybackStart(client: ApiClient, payload: Record<string, unknown>) {
    return client.postNoContent('/Sessions/Playing', payload);
}

export function reportPlaybackProgress(client: ApiClient, payload: Record<string, unknown>) {
    return client.postNoContent('/Sessions/Playing/Progress', payload);
}

export function reportPlaybackStop(client: ApiClient, payload: Record<string, unknown>) {
    return client.postNoContent('/Sessions/Playing/Stopped', payload);
}
