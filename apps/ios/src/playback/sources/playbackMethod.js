export function playbackMethod(mediaSource) {
    if (mediaSource.SupportsDirectPlay) return 'DirectPlay';
    if (mediaSource.SupportsDirectStream) return 'DirectStream';
    if (mediaSource.SupportsTranscoding) return 'Transcode';
    return null;
}
