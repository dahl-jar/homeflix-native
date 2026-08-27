import assert from 'node:assert/strict';
import { test } from 'node:test';

import { runtimeText, chips } from '../format.js';

const TICKS_PER_MINUTE = 600000000;

test('should format runtime ticks as hours and minutes', () => {
    assert.equal(runtimeText(142 * TICKS_PER_MINUTE), '2h 22m');
});

test('should format sub-hour runtimes as minutes only', () => {
    assert.equal(runtimeText(45 * TICKS_PER_MINUTE), '45m');
});

test('should build chips from year, runtime and official rating', () => {
    const item = {
        ProductionYear: 1994,
        RunTimeTicks: 142 * TICKS_PER_MINUTE,
        OfficialRating: 'NO-15'
    };

    assert.deepEqual(chips(item), ['1994', '2h 22m', 'NO-15']);
});

test('should omit chips for missing fields', () => {
    assert.deepEqual(chips({ ProductionYear: 2001 }), ['2001']);
});
