import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createPlaybackRequestPolicy } from '../playbackRequestPolicy.js';

test('should discover iOS sources without progressive direct stream', () => {
    assert.deepEqual(createPlaybackRequestPolicy('ios'), {
        enableDirectPlay: true,
        enableDirectStream: false,
        allowVideoStreamCopy: true,
        allowAudioStreamCopy: true
    });
});

test('should force non-direct-play iOS sources through encoded HLS', () => {
    assert.deepEqual(createPlaybackRequestPolicy('ios', {
        IsRemote: false,
        SupportsDirectPlay: false
    }), {
        enableDirectPlay: false,
        enableDirectStream: false,
        allowVideoStreamCopy: false,
        allowAudioStreamCopy: false
    });
});

test('should preserve direct play for compatible local iOS sources', () => {
    assert.deepEqual(createPlaybackRequestPolicy('ios', {
        IsRemote: false,
        SupportsDirectPlay: true
    }), {
        enableDirectPlay: true,
        enableDirectStream: false,
        allowVideoStreamCopy: true,
        allowAudioStreamCopy: true
    });
});

test('should preserve Android direct and remux capabilities', () => {
    assert.deepEqual(createPlaybackRequestPolicy('android', { IsRemote: true }), {
        enableDirectPlay: true,
        enableDirectStream: true,
        allowVideoStreamCopy: true,
        allowAudioStreamCopy: true
    });
});
