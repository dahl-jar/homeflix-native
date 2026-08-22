import assert from 'node:assert/strict';
import { test } from 'node:test';

import { normalizeMediaSegments, findActiveSkipSegment } from '../mediaSegmentPolicy.js';

const TICKS_PER_SECOND = 10_000_000;

test('should normalize supported segments in playback order', () => {
    const segments = normalizeMediaSegments([
        { Id: 'outro', Type: 'Outro', StartTicks: 100 * TICKS_PER_SECOND, EndTicks: 120 * TICKS_PER_SECOND },
        { Id: 'intro', Type: 'Intro', StartTicks: 10 * TICKS_PER_SECOND, EndTicks: 20 * TICKS_PER_SECOND },
        { Id: 'unknown', Type: 'Commercial', StartTicks: 30, EndTicks: 40 }
    ]);

    assert.deepEqual(segments.map(({ id, type }) => ({ id, type })), [
        { id: 'intro', type: 'Intro' },
        { id: 'outro', type: 'Outro' }
    ]);
});

test('should remove reversed and sub-second ranges', () => {
    assert.deepEqual(normalizeMediaSegments([
        { Id: 'reversed', Type: 'Intro', StartTicks: 20, EndTicks: 10 },
        { Id: 'tiny', Type: 'Recap', StartTicks: 0, EndTicks: TICKS_PER_SECOND - 1 }
    ]), []);
});

test('should find the active segment until its end boundary', () => {
    const [segment] = normalizeMediaSegments([
        { Id: 'intro', Type: 'Intro', StartTicks: 10, EndTicks: TICKS_PER_SECOND + 10 }
    ]);

    assert.equal(findActiveSkipSegment([segment], 10, new Set()).id, 'intro');
    assert.equal(findActiveSkipSegment([segment], TICKS_PER_SECOND + 10, new Set()), null);
});

test('should hide a dismissed segment', () => {
    const [segment] = normalizeMediaSegments([
        { Id: 'intro', Type: 'Intro', StartTicks: 0, EndTicks: TICKS_PER_SECOND }
    ]);

    assert.equal(findActiveSkipSegment([segment], 1, new Set(['intro'])), null);
});
