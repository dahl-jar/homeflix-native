function releasedUrl(serverUrl, transcodingUrl) {
    if (!transcodingUrl) throw new Error('released source has no transcoding url');
    return new URL(transcodingUrl, serverUrl).toString();
}

function directUrl({
    serverUrl,
    itemId,
    mediaSource,
    playbackInfo,
    tracks,
    pipelineId,
    attemptId
}) {
    const url = new URL(`/Videos/${itemId}/stream`, serverUrl);
    url.searchParams.set('Static', 'true');
    url.searchParams.set('MediaSourceId', mediaSource.Id);
    url.searchParams.set('PlaySessionId', playbackInfo.PlaySessionId);
    url.searchParams.set('AudioStreamIndex', String(tracks.audioStreamIndex));
    url.searchParams.set('SubtitleStreamIndex', String(tracks.subtitleStreamIndex));
    url.searchParams.set('PlaybackPipelineId', pipelineId);
    url.searchParams.set('PlaybackAttemptId', attemptId);
    return url.toString();
}

export function createVideoSource(options) {
    const uri = options.playMethod === 'DirectPlay'
        ? directUrl(options)
        : releasedUrl(options.serverUrl, options.mediaSource.TranscodingUrl);
    const source = {
        uri,
        headers: options.mediaHeaders
    };
    if (/\.m3u8(?:$|\?)/i.test(uri) || options.mediaSource.TranscodingSubProtocol === 'hls') {
        source.contentType = 'hls';
    }
    return { source, playMethod: options.playMethod };
}
