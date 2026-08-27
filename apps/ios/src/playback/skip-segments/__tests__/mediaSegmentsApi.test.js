import assert from 'node:assert/strict';
import { test } from 'node:test';

import { fetchMediaSegments } from '../mediaSegmentsApi.js';

test('should fetch only skip-related media segments', async () => {
    const calls = [];
    const client = {
        async get(path, params) {
            calls.push({ path, params });
            return { Items: [{ Id: 'intro' }] };
        }
    };

    const result = await fetchMediaSegments(client, 'item-one', ['Intro', 'Recap', 'Outro']);

    assert.deepEqual(result, [{ Id: 'intro' }]);
    assert.deepEqual(calls[0], {
        path: '/MediaSegments/item-one',
        params: { includeSegmentTypes: 'Intro,Recap,Outro' }
    });
});
