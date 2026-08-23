const DEFAULT_POLICY = {
    enableDirectPlay: true,
    enableDirectStream: true,
    allowVideoStreamCopy: true,
    allowAudioStreamCopy: true
};

const IOS_DISCOVERY_POLICY = {
    ...DEFAULT_POLICY,
    enableDirectStream: false
};

const IOS_HLS_POLICY = {
    enableDirectPlay: false,
    enableDirectStream: false,
    allowVideoStreamCopy: true,
    allowAudioStreamCopy: true
};

export function createPlaybackRequestPolicy(platform, mediaSource = null) {
    if (platform === 'android') return DEFAULT_POLICY;
    if (platform !== 'ios') throw new Error(`unsupported playback platform: ${platform}`);
    return mediaSource?.SupportsDirectPlay === false
        ? IOS_HLS_POLICY
        : IOS_DISCOVERY_POLICY;
}
