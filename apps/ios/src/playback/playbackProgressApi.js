export function getPlaybackProgress(client, request) {
    return client.get('/Playback/PipelineProgress', {
        pipelineId: request.pipelineId,
        attemptId: request.attemptId,
        afterSequence: request.afterSequence
    });
}
