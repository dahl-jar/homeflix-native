import assert from 'node:assert/strict';
import { test } from 'node:test';

import { selectSearchItem } from '../selectSearchItem.js';

test('should navigate with the canonical item id returned by the server', async () => {
    const routes = [];
    const client = {
        get: async () => ({ Id: 'canonical-item' })
    };

    await selectSearchItem({
        client,
        userId: 'user-id',
        itemId: 'synthetic-item',
        navigate: (route) => routes.push(route)
    });

    assert.deepEqual(routes, ['/details/canonical-item']);
});

test('should request metadata only before opening a search result', async () => {
    const requests = [];
    const client = {
        get: async (path, params) => {
            requests.push({ path, params });
            return { Id: 'canonical-item' };
        }
    };

    await selectSearchItem({
        client,
        userId: 'user-id',
        itemId: 'synthetic-item',
        navigate: () => {}
    });

    assert.deepEqual(requests, [{
        path: '/Users/user-id/Items/synthetic-item',
        params: {
            includeMediaSources: false,
            includeMediaStreams: false,
            waitForSeriesTree: false
        }
    }]);
});

test('should keep search open when materialization fails', async () => {
    const routes = [];
    const client = {
        get: async () => {
            throw new Error('unavailable');
        }
    };

    await assert.rejects(selectSearchItem({
        client,
        userId: 'user-id',
        itemId: 'synthetic-item',
        navigate: (route) => routes.push(route)
    }));

    assert.deepEqual(routes, []);
});

test('should reject a materialized item without a canonical id', async () => {
    const routes = [];
    const client = {
        get: async () => ({ Name: 'Missing id' })
    };

    await assert.rejects(selectSearchItem({
        client,
        userId: 'user-id',
        itemId: 'synthetic-item',
        navigate: (route) => routes.push(route)
    }), /canonical id/);

    assert.deepEqual(routes, []);
});
