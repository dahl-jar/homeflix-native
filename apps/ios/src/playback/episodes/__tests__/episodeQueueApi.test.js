import assert from 'node:assert/strict';
import { test } from 'node:test';

import { fetchFollowingEpisodeCandidates, fetchSeriesEpisodes } from '../episodeQueueApi.js';

test('should fetch the current and following playable episode in server order', async () => {
    const calls = [];
    const client = {
        async get(path, params) {
            calls.push({ path, params });
            return { Items: [{ Id: 'current' }, { Id: 'next' }] };
        }
    };

    const result = await fetchFollowingEpisodeCandidates(client, {
        userId: 'user-one',
        seriesId: 'series-one',
        itemId: 'current'
    });

    assert.deepEqual(result, [{ Id: 'current' }, { Id: 'next' }]);
    assert.deepEqual(calls[0], {
        path: '/Shows/series-one/Episodes',
        params: {
            userId: 'user-one',
            startItemId: 'current',
            limit: 2,
            isMissing: false,
            enableImages: true,
            enableUserData: true,
            enableTotalRecordCount: false,
            fields: 'Overview,PrimaryImageAspectRatio'
        }
    });
});

test('should fetch playable series episodes for the in-player menu', async () => {
    const calls = [];
    const client = {
        async get(path, params) {
            calls.push({ path, params });
            return { Items: [{ Id: 'episode-one' }] };
        }
    };

    const result = await fetchSeriesEpisodes(client, {
        userId: 'user-one',
        seriesId: 'series-one',
        itemId: 'episode-one'
    });

    assert.deepEqual(result, [{ Id: 'episode-one' }]);
    assert.deepEqual(calls[0], {
        path: '/Shows/series-one/Episodes',
        params: {
            userId: 'user-one',
            startItemId: 'episode-one',
            isMissing: false,
            enableImages: true,
            enableUserData: true,
            enableTotalRecordCount: false,
            fields: 'Overview,PrimaryImageAspectRatio'
        }
    });
});
