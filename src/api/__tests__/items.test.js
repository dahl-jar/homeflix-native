import test from 'node:test';
import assert from 'node:assert/strict';

import { fetchItemsByIds } from '../items.js';

test('should fetch item ids in one request', async () => {
    const calls = [];
    const client = {
        get(path, query) {
            calls.push({ path, query });
            return Promise.resolve({ Items: [{ Id: 'first' }, { Id: 'second' }] });
        }
    };

    const result = await fetchItemsByIds(client, 'user-id', ['first', 'second']);

    assert.deepEqual(result, [{ Id: 'first' }, { Id: 'second' }]);
    assert.deepEqual(calls, [{
        path: '/Items',
        query: {
            userId: 'user-id',
            ids: 'first,second',
            enableImages: true,
            enableUserData: true,
            enableTotalRecordCount: false,
            fields: 'Overview'
        }
    }]);
});

test('should skip the request when no ids are provided', async () => {
    const client = {
        get() {
            throw new Error('request should not run');
        }
    };

    const result = await fetchItemsByIds(client, 'user-id', []);

    assert.deepEqual(result, []);
});
