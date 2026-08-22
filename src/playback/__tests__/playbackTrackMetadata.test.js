import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    mergeNativeTrackMetadata,
    playbackTrackMetadata
} from '../playbackTrackMetadata.js';

test('should expose selectable server tracks with clean labels', () => {
    const metadata = playbackTrackMetadata({
        MediaStreams: [
            { Type: 'Audio', Index: 1, Language: 'jpn', Channels: 2, DisplayTitle: 'Japanese FLAC' },
            { Type: 'Audio', Index: 3, Language: 'eng', Channels: 6, DisplayTitle: 'English AAC 5.1' },
            { Type: 'Subtitle', Index: 7, Language: 'eng', DisplayTitle: 'English ASS' }
        ]
    }, 3, 7);

    assert.deepEqual(metadata.audioTracks.map(({ label }) => label), [
        'Japanese · Stereo',
        'English · 5.1'
    ]);
    assert.equal(metadata.selectedAudioTrack.label, 'English · 5.1');
    assert.equal(metadata.selectedAudioTrack.language, 'eng');
    assert.equal(metadata.selectedAudioTrack.serverResolved, true);
    assert.equal(metadata.selectedSubtitleTrack.label, 'English');
});

test('should hide commentary tracks from both selectors', () => {
    const metadata = playbackTrackMetadata({
        MediaStreams: [
            { Type: 'Audio', Index: 1, Language: 'jpn', DisplayTitle: 'Japanese' },
            { Type: 'Audio', Index: 2, Language: 'eng', DisplayTitle: 'English Commentary' },
            { Type: 'Subtitle', Index: 3, Language: 'eng', DisplayTitle: 'English' },
            { Type: 'Subtitle', Index: 4, Language: 'eng', DisplayTitle: 'English Commentary' }
        ]
    }, 1, 3);

    assert.deepEqual(metadata.audioTracks.map(({ streamIndex }) => streamIndex), [1]);
    assert.deepEqual(metadata.subtitleTracks.map(({ streamIndex }) => streamIndex), [3]);
});

test('should hide signs-or-songs subtitle tracks', () => {
    const metadata = playbackTrackMetadata({
        MediaStreams: [
            { Type: 'Subtitle', Index: 3, Language: 'eng', DisplayTitle: 'English Signs & Songs', IsForced: true },
            { Type: 'Subtitle', Index: 4, Language: 'eng', DisplayTitle: 'English' }
        ]
    }, 1, 4);

    assert.deepEqual(metadata.subtitleTracks.map(({ streamIndex }) => streamIndex), [4]);
});

test('should keep a forced subtitle without signs or commentary', () => {
    const metadata = playbackTrackMetadata({
        MediaStreams: [
            { Type: 'Subtitle', Index: 5, Language: 'eng', DisplayTitle: 'English Forced', IsForced: true }
        ]
    }, 1, 5);

    assert.equal(metadata.subtitleTracks[0].label, 'English · Forced');
    assert.equal(metadata.selectedSubtitleTrack.streamIndex, 5);
});

test('should distinguish duplicate subtitle languages by delivery', () => {
    const metadata = playbackTrackMetadata({
        MediaStreams: [
            { Type: 'Subtitle', Index: 3, Language: 'eng', IsExternal: false },
            { Type: 'Subtitle', Index: 4, Language: 'eng', IsExternal: true }
        ]
    }, 1, 4);

    assert.deepEqual(metadata.subtitleTracks.map(({ label }) => label), [
        'English · Embedded',
        'English · External'
    ]);
});

test('should preserve server metadata when native HLS exposes no tracks', () => {
    const audioTrack = { label: 'English AAC 5.1', serverResolved: true };
    const current = {
        audioTracks: [audioTrack],
        subtitleTracks: [],
        selectedAudioTrack: audioTrack,
        selectedSubtitleTrack: null
    };

    assert.deepEqual(mergeNativeTrackMetadata(current, {
        audioTracks: [],
        subtitleTracks: [],
        selectedAudioTrack: null,
        selectedSubtitleTrack: null
    }), current);
});

test('should apply presentation policy to native player tracks', () => {
    const nativeAudio = { id: 'audio-one', label: 'Japanese AAC', language: 'jpn' };
    const nativeSubtitle = { id: 'subtitle-one', label: 'English ASS', language: 'eng' };
    const result = mergeNativeTrackMetadata({
        audioTracks: [{ label: 'English', serverResolved: true }],
        subtitleTracks: [],
        selectedAudioTrack: null,
        selectedSubtitleTrack: null
    }, {
        audioTracks: [
            nativeAudio,
            { id: 'audio-two', label: 'English Commentary', language: 'eng' }
        ],
        subtitleTracks: [
            nativeSubtitle,
            { id: 'subtitle-two', label: 'English Signs & Songs', language: 'eng' }
        ],
        selectedAudioTrack: nativeAudio,
        selectedSubtitleTrack: nativeSubtitle
    });

    assert.deepEqual(result.audioTracks.map(({ label }) => label), ['Japanese']);
    assert.deepEqual(result.subtitleTracks.map(({ label }) => label), ['English']);
    assert.equal(result.selectedAudioTrack.id, 'audio-one');
    assert.equal(result.selectedSubtitleTrack.id, 'subtitle-one');
});
