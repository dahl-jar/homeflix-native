const MAX_STREAMING_BITRATE = 120_000_000;
const IOS_H264_PROFILES = 'high|main|baseline|constrained baseline';

const IOS_CODEC_PROFILES = [{
    Type: 'Video',
    Codec: 'h264',
    Conditions: [{
        Condition: 'EqualsAny',
        Property: 'VideoProfile',
        Value: IOS_H264_PROFILES,
        IsRequired: false
    }]
}];

const PLATFORM_CAPABILITIES = {
    ios: {
        name: 'Homeflix iOS',
        directContainers: 'mp4,m4v,mov',
        directVideoCodecs: 'h264,hevc,mpeg4',
        directAudioCodecs: 'aac,mp3,ac3,eac3,alac',
        transcodeVideoCodecs: 'h264,hevc'
    },
    android: {
        name: 'Homeflix Android',
        directContainers: 'mp4,m4v,mov,mkv,webm',
        directVideoCodecs: 'h264,hevc,mpeg4,vp8,vp9,av1',
        directAudioCodecs: 'aac,mp3,ac3,eac3,opus,vorbis,flac',
        transcodeVideoCodecs: 'h264,hevc'
    }
};

function platformCapabilities(platform) {
    const capabilities = PLATFORM_CAPABILITIES[platform];
    if (!capabilities) throw new Error(`unsupported playback platform: ${platform}`);
    return capabilities;
}

export function createNativeDeviceProfile(platform) {
    const capabilities = platformCapabilities(platform);

    return {
        Name: capabilities.name,
        MaxStreamingBitrate: MAX_STREAMING_BITRATE,
        MaxStaticBitrate: MAX_STREAMING_BITRATE,
        DirectPlayProfiles: [{
            Type: 'Video',
            Container: capabilities.directContainers,
            VideoCodec: capabilities.directVideoCodecs,
            AudioCodec: capabilities.directAudioCodecs
        }],
        TranscodingProfiles: [{
            Type: 'Video',
            Context: 'Streaming',
            Protocol: 'hls',
            Container: 'mp4',
            VideoCodec: capabilities.transcodeVideoCodecs,
            AudioCodec: 'aac,ac3,eac3',
            MinSegments: platform === 'ios' ? '2' : '1',
            BreakOnNonKeyFrames: true
        }],
        CodecProfiles: platform === 'ios' ? IOS_CODEC_PROFILES : [],
        SubtitleProfiles: [
            { Format: 'vtt', Method: 'Hls' },
            { Format: 'srt', Method: 'External' },
            { Format: 'ass', Method: 'Encode' },
            { Format: 'ssa', Method: 'Encode' },
            { Format: 'pgs', Method: 'Encode' }
        ]
    };
}
