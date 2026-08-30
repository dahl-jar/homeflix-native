import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    matchingNativeSubtitleTrack,
    mergeNativeTrackMetadata,
    playbackTrackMetadata
} from '../playbackTrackMetadata.js';

test('should expose selectable server tracks with clean labels', () => {
    const metadata = playbackTrackMetadata({
        MediaStreams: [
            { Type: 'Audio', Index: 1, Language: 'jpn', Channels: 2, DisplayTitle: 'Japanese FLAC' },
            { Type: 'Audio', Index: 3, Language: 'eng', Channels: 6, DisplayTitle: 'English AAC 5.1' },
            { Type: 'Subtitle', Index: 7, Language: 'eng', DisplayTitle: 'English ASS', IsDefault: true }
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
    assert.equal(metadata.selectedSubtitleTrack.isDefault, true);
    assert.equal(metadata.selectedSubtitleTrack.isForced, false);
    assert.equal(metadata.selectedSubtitleTrack.name, 'English ASS');
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
    assert.equal(metadata.subtitleTracks[0].isForced, true);
    assert.equal(metadata.subtitleTracks[0].isDefault, false);
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

test('should preserve selected server subtitle', () => {
    const selectedSubtitleTrack = {
        label: 'English · Embedded',
        language: 'eng',
        streamIndex: 4,
        serverResolved: true,
        isDefault: true
    };
    const current = {
        audioTracks: [],
        subtitleTracks: [selectedSubtitleTrack],
        selectedAudioTrack: null,
        selectedSubtitleTrack
    };

    const result = mergeNativeTrackMetadata(current, {
        audioTracks: [],
        subtitleTracks: [
            { label: 'English', language: 'en', isDefault: true },
            { label: 'German', language: 'de', isDefault: false }
        ],
        selectedAudioTrack: null,
        selectedSubtitleTrack: null
    });

    assert.equal(result.subtitleTracks, current.subtitleTracks);
    assert.equal(result.selectedSubtitleTrack, selectedSubtitleTrack);
});

test('should match native language default', () => {
    const englishSigns = {
        label: 'English Signs & Songs',
        language: 'en-US',
        isDefault: true
    };
    const englishSecondary = { label: 'English 1', language: 'en-US', isDefault: false };
    const germanDefault = { label: 'Deutsch', language: 'de', isDefault: true };
    const englishDefault = { label: 'English 2', language: 'en-US', isDefault: true };

    const result = matchingNativeSubtitleTrack({
        label: 'English · Embedded',
        language: 'eng',
        serverResolved: true,
        isDefault: true
    }, [englishSigns, englishSecondary, germanDefault, englishDefault]);

    assert.equal(result, englishDefault);
});

test('should match native nondefault subtitle', () => {
    const englishDefault = { label: 'English 1', language: 'en', isDefault: true };
    const englishSecondary = { label: 'English 2', language: 'en', isDefault: false };

    const result = matchingNativeSubtitleTrack({
        label: 'English · External',
        language: 'eng',
        serverResolved: true,
        isDefault: false
    }, [englishDefault, englishSecondary]);

    assert.equal(result, englishSecondary);
});

test('should match native forced subtitle', () => {
    const english = { label: 'English', language: 'en', isForced: false };
    const englishForced = { label: 'English Forced', language: 'en', isForced: true };

    const result = matchingNativeSubtitleTrack({
        label: 'English · Forced',
        language: 'eng',
        serverResolved: true,
        isForced: true
    }, [english, englishForced]);

    assert.equal(result, englishForced);
});

test('should match native subtitle name', () => {
    const embedded = { label: 'English', language: 'en', name: 'English Embedded' };
    const external = { label: 'English', language: 'en', name: 'english external' };

    const result = matchingNativeSubtitleTrack({
        label: 'English · External',
        language: 'eng',
        name: ' English External ',
        serverResolved: true
    }, [embedded, external]);

    assert.equal(result, external);
});

test('should reject ambiguous native subtitles', () => {
    const englishOne = { label: 'English 1', language: 'en' };
    const englishTwo = { label: 'English 2', language: 'en' };

    const result = matchingNativeSubtitleTrack({
        label: 'English · Embedded',
        language: 'eng',
        serverResolved: true
    }, [englishOne, englishTwo]);

    assert.equal(result, null);
});

test('should use sole language fallback', () => {
    const english = { label: 'English', language: 'en' };

    const result = matchingNativeSubtitleTrack({
        label: 'English · Forced',
        language: 'eng',
        serverResolved: true,
        isDefault: true,
        isForced: true
    }, [english]);

    assert.equal(result, english);
});

test('should preserve native subtitle without discovery', () => {
    const subtitleTrack = { label: 'English', language: 'en' };
    const current = {
        audioTracks: [],
        subtitleTracks: [subtitleTrack],
        selectedAudioTrack: null,
        selectedSubtitleTrack: subtitleTrack
    };

    const result = mergeNativeTrackMetadata(current, {
        audioTracks: [],
        subtitleTracks: [],
        selectedAudioTrack: null,
        selectedSubtitleTrack: null
    });

    assert.equal(result.subtitleTracks, current.subtitleTracks);
    assert.equal(result.selectedSubtitleTrack, subtitleTrack);
});
