import { test } from 'node:test';
import assert from 'node:assert/strict';

import { defaultSeasonIndex } from '../seasons.js';

test('should default to season one past a leading specials season', () => {
    const seasons = [
        { Name: 'Specials', IndexNumber: 0 },
        { Name: 'Season 01', IndexNumber: 1 },
        { Name: 'Season 02', IndexNumber: 2 }
    ];

    assert.equal(defaultSeasonIndex(seasons), 1);
});

test('should default to the first season when no specials exist', () => {
    assert.equal(defaultSeasonIndex([{ Name: 'Season 01', IndexNumber: 1 }]), 0);
});

test('should fall back to the first entry when only specials exist', () => {
    assert.equal(defaultSeasonIndex([{ Name: 'Specials', IndexNumber: 0 }]), 0);
});
