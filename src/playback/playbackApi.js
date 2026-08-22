function playbackBody(request) {
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

export function resolvePlaybackAttempt(client, request) {
    const override = request.trackOverride;
    return client.post(`/Items/${request.itemId}/PlaybackInfo`, {
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

export function releasePlaybackSource(client, request) {
    return client.post(`/Items/${request.itemId}/PlaybackInfo`, {
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

export function reportPlaybackStart(client, payload) {
    return client.postNoContent('/Sessions/Playing', payload);
}

export function reportPlaybackProgress(client, payload) {
    return client.postNoContent('/Sessions/Playing/Progress', payload);
}

export function reportPlaybackStop(client, payload) {
    return client.postNoContent('/Sessions/Playing/Stopped', payload);
}
