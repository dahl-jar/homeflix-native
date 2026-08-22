import assert from 'node:assert/strict';
import { test } from 'node:test';

import { resolvePlayableItem } from '../playbackItemResolver.js';

test('should preserve an item that already has media', async () => {
    const movie = { Id: 'movie-one', Type: 'Movie' };
    const client = { get: async () => assert.fail('unexpected request') };

    assert.equal(await resolvePlayableItem(client, 'user-one', movie), movie);
});

test('should resolve a series to its resumable or next episode', async () => {
    const calls = [];
    const episode = { Id: 'episode-two', Type: 'Episode' };
    const client = {
        async get(path, query) {
            calls.push({ path, query });
            return { Items: [episode] };
        }
    };

    const result = await resolvePlayableItem(client, 'user-one', {
        Id: 'series-one',
        Type: 'Series'
    });

    assert.equal(result, episode);
    assert.deepEqual(calls, [{
        path: '/Shows/NextUp',
        query: {
            userId: 'user-one',
            seriesId: 'series-one',
            limit: 1,
            enableResumable: true,
            enableRewatching: true,
            enableUserData: true,
            enableTotalRecordCount: false
        }
    }]);
});

test('should reject a series without a playable episode', async () => {
    const client = { get: async () => ({ Items: [] }) };

    await assert.rejects(
        resolvePlayableItem(client, 'user-one', { Id: 'series-one', Type: 'Series' }),
        /series has no playable episode/
    );
});
