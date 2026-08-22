import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    formatPlaybackTime,
    nextVideoContentFit,
    seekPositionFromPress
} from '../playerControlsModel.js';

test('should format player time without leaking invalid values', () => {
    assert.equal(formatPlaybackTime(0), '0:00');
    assert.equal(formatPlaybackTime(65), '1:05');
    assert.equal(formatPlaybackTime(3_661), '1:01:01');
    assert.equal(formatPlaybackTime(Number.NaN), '0:00');
});

test('should convert a timeline press into a bounded seek position', () => {
    assert.equal(seekPositionFromPress(50, 200, 100), 25);
    assert.equal(seekPositionFromPress(-20, 200, 100), 0);
    assert.equal(seekPositionFromPress(250, 200, 100), 100);
    assert.equal(seekPositionFromPress(50, 0, 100), 0);
});

test('should toggle between fitting and filling the player surface', () => {
    assert.equal(nextVideoContentFit('contain'), 'cover');
    assert.equal(nextVideoContentFit('cover'), 'contain');
});
