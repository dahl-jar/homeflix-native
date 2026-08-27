import type {
    MediaSource,
    PlaybackPlatform,
    PlaybackRequestPolicy
} from './playbackTypes.ts';

const DEFAULT_POLICY: PlaybackRequestPolicy = {
    enableDirectPlay: true,
    enableDirectStream: true,
    allowVideoStreamCopy: true,
    allowAudioStreamCopy: true
};

const IOS_DISCOVERY_POLICY: PlaybackRequestPolicy = {
    ...DEFAULT_POLICY,
    enableDirectStream: false
};

const IOS_HLS_POLICY: PlaybackRequestPolicy = {
    enableDirectPlay: false,
    enableDirectStream: false,
    allowVideoStreamCopy: true,
    allowAudioStreamCopy: true
};

export function createPlaybackRequestPolicy(
    platform: PlaybackPlatform,
    mediaSource: MediaSource | null = null
): PlaybackRequestPolicy {
    if (platform === 'android') return DEFAULT_POLICY;
    if (platform !== 'ios') throw new Error(`unsupported playback platform: ${platform}`);
    return mediaSource?.SupportsDirectPlay === false
        ? IOS_HLS_POLICY
        : IOS_DISCOVERY_POLICY;
}
