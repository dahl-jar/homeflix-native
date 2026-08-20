import { test } from 'node:test';
import assert from 'node:assert/strict';

import { createClient, ApiError } from '../client.js';

const okResponse = (body) => ({
    ok: true,
    status: 200,
    json: async () => body
});

test('should attach the MediaBrowser identity and token header', async () => {
    const seen = [];
    const client = createClient({
        baseUrl: 'http://server',
        token: 'token-one',
        fetchFn: async (url, options) => {
            seen.push({ url, options });
            return okResponse({});
        }
    });

    await client.get('/Users/Me');

    const header = seen[0].options.headers.Authorization;
    assert.match(header, /^MediaBrowser Client="Homeflix", Device=".+", DeviceId=".+", Version=".+"/);
    assert.match(header, /Token="token-one"$/);
});

test('should send the identity header without a token before sign-in', async () => {
    const seen = [];
    const client = createClient({
        baseUrl: 'http://server',
        token: '',
        fetchFn: async (url, options) => {
            seen.push(options.headers.Authorization);
            return okResponse({});
        }
    });

    await client.get('/Users/Public');

    assert.match(seen[0], /^MediaBrowser Client="Homeflix"/);
    assert.doesNotMatch(seen[0], /Token=/);
});

test('should throw ApiError carrying the status on non-2xx', async () => {
    const client = createClient({
        baseUrl: 'http://server',
        token: 'token-one',
        fetchFn: async () => ({ ok: false, status: 401, json: async () => ({}) })
    });

    await assert.rejects(client.get('/Users/Me'), (error) => {
        assert.ok(error instanceof ApiError);
        assert.equal(error.status, 401);
        return true;
    });
});

test('should serialize query params onto the url', async () => {
    const seen = [];
    const client = createClient({
        baseUrl: 'http://server',
        token: 'token-one',
        fetchFn: async (url) => {
            seen.push(url);
            return okResponse({});
        }
    });

    await client.get('/Items', { searchTerm: 'matrix', limit: 20 });

    assert.equal(seen[0], 'http://server/Items?searchTerm=matrix&limit=20');
});
