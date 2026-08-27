import assert from 'node:assert/strict';
import test from 'node:test';

import {
    fetchDetailItem,
    fetchItem,
    fetchItemsByIds,
    fetchSeasons,
    searchItems
} from '../items.js';

test('should request detail metadata without sources or a series tree wait', async () => {
    const calls = [];
    const client = {
        get(path, query) {
            calls.push({ path, query });
            return Promise.resolve({ Id: 'item-one' });
        }
    };

    await fetchDetailItem(client, 'user-id', 'item-one');

    assert.deepEqual(calls, [{
        path: '/Users/user-id/Items/item-one',
        query: {
            includeMediaSources: false,
            includeMediaStreams: false,
            waitForSeriesTree: false
        }
    }]);
});

test('should request season cards without media sources', async () => {
    const calls = [];
    const client = {
        get(path, query) {
            calls.push({ path, query });
            return Promise.resolve({ Items: [] });
        }
    };

    await fetchSeasons(client, 'user-id', 'series-one');

    assert.deepEqual(calls, [{
        path: '/Shows/series-one/Seasons',
        query: {
            userId: 'user-id',
            fields: 'ItemCounts,PrimaryImageAspectRatio,CanDelete'
        }
    }]);
});

test('should preserve complete item requests for non-detail callers', async () => {
    const calls = [];
    const client = {
        get(path, query) {
            calls.push({ path, query });
            return Promise.resolve({ Id: 'item-one' });
        }
    };

    await fetchItem(client, 'user-id', 'item-one');

    assert.deepEqual(calls, [{
        path: '/Users/user-id/Items/item-one',
        query: undefined
    }]);
});

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

test('should send a local search prefix with paging and cancellation', async () => {
    const calls = [];
    const controller = new AbortController();
    const client = {
        get(path, query, options) {
            calls.push({ path, query, options });
            return Promise.resolve({ Items: [] });
        }
    };

    await searchItems(client, 'user-id', {
        term: 'xmen',
        startIndex: 18,
        limit: 18,
        localOnly: true,
        signal: controller.signal
    });

    assert.deepEqual(calls, [{
        path: '/Items',
        query: {
            userId: 'user-id',
            searchTerm: 'local:xmen',
            recursive: true,
            includeItemTypes: 'Movie,Series',
            startIndex: 18,
            limit: 18
        },
        options: { signal: controller.signal }
    }]);
});

test('should send an authoritative search term with paging', async () => {
    const calls = [];
    const client = {
        get(path, query, options) {
            calls.push({ path, query, options });
            return Promise.resolve({ Items: [] });
        }
    };

    await searchItems(client, 'user-id', {
        term: 'xmen',
        startIndex: 0,
        limit: 18,
        localOnly: false
    });

    assert.equal(calls[0].query.searchTerm, 'xmen');
    assert.equal(calls[0].query.startIndex, 0);
    assert.deepEqual(calls[0].options, { signal: undefined });
});
