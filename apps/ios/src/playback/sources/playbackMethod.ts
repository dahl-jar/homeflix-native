import type { MediaSource, PlayMethod } from './playbackTypes.ts';

export function playbackMethod(mediaSource: MediaSource): PlayMethod | null {
    if (mediaSource.SupportsDirectPlay) return 'DirectPlay';
    if (mediaSource.SupportsDirectStream) return 'DirectStream';
    if (mediaSource.SupportsTranscoding) return 'Transcode';
    return null;
}
