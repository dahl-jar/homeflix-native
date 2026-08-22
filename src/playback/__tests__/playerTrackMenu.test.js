import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    audioTrackEntries,
    selectedTrackKey,
    subtitleTrackEntries
} from '../playerTrackMenu.js';

const japanese = { label: 'Japanese FLAC' };
const english = { label: 'English AAC' };
const subtitle = { label: 'English ASS' };

test('should build a separate audio-only menu', () => {
    const snapshot = {
        audioTracks: [japanese, english],
        subtitleTracks: [subtitle],
        selectedAudioTrack: japanese,
        selectedSubtitleTrack: null
    };

    const entries = audioTrackEntries(snapshot);

    assert.deepEqual(entries.map(({ label }) => label), [
        'Japanese FLAC',
        'English AAC'
    ]);
    assert.equal(selectedTrackKey(snapshot, entries[0]), true);
});

test('should build a separate subtitle menu with off', () => {
    const snapshot = {
        audioTracks: [japanese],
        subtitleTracks: [subtitle],
        selectedAudioTrack: japanese,
        selectedSubtitleTrack: null
    };

    const entries = subtitleTrackEntries(snapshot);

    assert.deepEqual(entries.map(({ label }) => label), ['Off', 'English ASS']);
    assert.equal(selectedTrackKey(snapshot, entries[0]), true);
    assert.equal(selectedTrackKey(snapshot, entries[1]), false);
});
