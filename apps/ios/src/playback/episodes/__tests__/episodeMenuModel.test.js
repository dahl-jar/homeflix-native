import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createEpisodeMenuEntries } from '../episodeMenuModel.js';

test('should format playable episodes in server order', () => {
    const episodes = [
        { Id: 'before', Type: 'Episode', ParentIndexNumber: 1, IndexNumber: 1, Name: 'First' },
        { Id: 'one', Type: 'Episode', ParentIndexNumber: 1, IndexNumber: 2, Name: 'Second' },
        { Id: 'missing', Type: 'Episode', ParentIndexNumber: 1, IndexNumber: 3, Name: 'Missing', IsMissing: true },
        { Id: 'movie', Type: 'Movie', Name: 'Movie' },
        { Id: 'special', Type: 'Episode', ParentIndexNumber: 0, IndexNumber: 1, Name: 'Special' }
    ];
    const entries = createEpisodeMenuEntries(episodes, episodes[1]);

    assert.deepEqual(entries, [
        { current: true, episode: episodes[1], key: 'one', label: 'S1:E2 · Second' },
        { current: false, episode: episodes[4], key: 'special', label: 'Special 1 · Special' }
    ]);
});

test('should retain a useful label when episode numbers are unavailable', () => {
    const episode = { Id: 'unknown', Type: 'Episode', Name: 'Preview' };
    assert.deepEqual(createEpisodeMenuEntries([episode], episode), [{
        current: true,
        episode,
        key: 'unknown',
        label: 'Preview'
    }]);
});

test('should remove earlier episodes when the current item has a different id', () => {
    const episodes = [
        { Id: 'before', Type: 'Episode', ParentIndexNumber: 1, IndexNumber: 1, Name: 'First' },
        { Id: 'current-copy', Type: 'Episode', ParentIndexNumber: 1, IndexNumber: 2, Name: 'Second' },
        { Id: 'after', Type: 'Episode', ParentIndexNumber: 1, IndexNumber: 3, Name: 'Third' }
    ];

    const entries = createEpisodeMenuEntries(episodes, {
        Id: 'current-route-item',
        ParentIndexNumber: 1,
        IndexNumber: 2
    });

    assert.deepEqual(entries.map((entry) => [entry.key, entry.current]), [
        ['current-copy', true],
        ['after', false]
    ]);
});

test('should prepend an unlisted special and keep only later seasons', () => {
    const current = {
        Id: 'special-current',
        Type: 'Episode',
        ParentIndexNumber: 0,
        IndexNumber: 1,
        Name: 'Special'
    };
    const entries = createEpisodeMenuEntries([
        { Id: 'regular-one', Type: 'Episode', ParentIndexNumber: 1, IndexNumber: 1, Name: 'First' },
        { Id: 'regular-two', Type: 'Episode', ParentIndexNumber: 1, IndexNumber: 2, Name: 'Second' }
    ], current);

    assert.deepEqual(entries.map((entry) => [entry.key, entry.current]), [
        ['special-current', true],
        ['regular-one', false],
        ['regular-two', false]
    ]);
});
