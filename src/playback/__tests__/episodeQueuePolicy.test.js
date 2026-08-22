import assert from 'node:assert/strict';
import { test } from 'node:test';

import { selectFollowingEpisode } from '../episodeQueuePolicy.js';

test('should select the episode after the current item', () => {
    const next = selectFollowingEpisode([
        { Id: 'current', Type: 'Episode' },
        { Id: 'next', Type: 'Episode' }
    ], 'current');

    assert.equal(next.Id, 'next');
});

test('should continue into the next season in server order', () => {
    const next = selectFollowingEpisode([
        { Id: 'season-one-last', Type: 'Episode', ParentIndexNumber: 1, IndexNumber: 10 },
        { Id: 'season-two-first', Type: 'Episode', ParentIndexNumber: 2, IndexNumber: 1 }
    ], 'season-one-last');

    assert.equal(next.Id, 'season-two-first');
});

test('should skip missing episode records', () => {
    const next = selectFollowingEpisode([
        { Id: 'current', Type: 'Episode' },
        { Id: 'missing', Type: 'Episode', IsMissing: true },
        { Id: 'next', Type: 'Episode' }
    ], 'current');

    assert.equal(next.Id, 'next');
});

test('should return null at the end of a series', () => {
    assert.equal(selectFollowingEpisode([{ Id: 'last', Type: 'Episode' }], 'last'), null);
});
