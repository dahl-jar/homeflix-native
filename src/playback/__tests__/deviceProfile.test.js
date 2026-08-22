import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createNativeDeviceProfile } from '../deviceProfile.js';

test('should advertise only Apple-native direct-play containers on iOS', () => {
    const profile = createNativeDeviceProfile('ios');
    const direct = profile.DirectPlayProfiles.find((entry) => entry.Type === 'Video');

    assert.equal(direct.Container, 'mp4,m4v,mov');
    assert.match(direct.VideoCodec, /h264/);
    assert.match(direct.VideoCodec, /hevc/);
    assert.doesNotMatch(direct.Container, /mkv|webm/);
});

test('should exclude unsupported H.264 profiles from iOS stream copy', () => {
    const profile = createNativeDeviceProfile('ios');
    const h264 = profile.CodecProfiles?.find((entry) => entry.Codec === 'h264');
    const videoProfiles = h264?.Conditions.find((condition) =>
        condition.Property === 'VideoProfile'
    );

    assert.equal(videoProfiles?.Value, 'high|main|baseline|constrained baseline');
    assert.doesNotMatch(videoProfiles.Value, /high 10/i);
});

test('should allow server HLS fallback on iOS', () => {
    const profile = createNativeDeviceProfile('ios');
    const transcode = profile.TranscodingProfiles.find((entry) => entry.Type === 'Video');

    assert.equal(transcode.Protocol, 'hls');
    assert.equal(transcode.Container, 'mp4');
    assert.equal(transcode.VideoCodec, 'h264,hevc');
    assert.equal(transcode.AudioCodec, 'aac,ac3,eac3');
    assert.equal(transcode.MinSegments, '2');
    assert.equal(transcode.BreakOnNonKeyFrames, true);
});

test('should deliver converted text subtitles through native HLS', () => {
    const profile = createNativeDeviceProfile('ios');
    const textSubtitle = profile.SubtitleProfiles[0];

    assert.deepEqual(textSubtitle, { Format: 'vtt', Method: 'Hls' });
});

test('should advertise Android Media3 direct-play containers separately', () => {
    const profile = createNativeDeviceProfile('android');
    const direct = profile.DirectPlayProfiles.find((entry) => entry.Type === 'Video');

    assert.match(direct.Container, /mkv/);
    assert.match(direct.Container, /webm/);
    assert.match(direct.VideoCodec, /vp9/);
});

test('should reject an unsupported platform profile', () => {
    assert.throws(() => createNativeDeviceProfile('web'), /unsupported playback platform/);
});
