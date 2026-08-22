import assert from 'node:assert/strict';
import { test } from 'node:test';

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

test('should serialize query params on post requests', async () => {
    const seen = [];
    const client = createClient({
        baseUrl: 'http://server',
        token: 'token-one',
        fetchFn: async (url, options) => {
            seen.push({ url, options });
            return okResponse({ result: 'ok' });
        }
    });

    await client.post('/Items/item-one/PlaybackInfo', { UserId: 'user-one' }, { mediaSourceId: 'source-one' });

    assert.equal(seen[0].url, 'http://server/Items/item-one/PlaybackInfo?mediaSourceId=source-one');
    assert.equal(seen[0].options.body, JSON.stringify({ UserId: 'user-one' }));
});

test('should accept a no-content response without parsing json', async () => {
    let parsed = false;
    const client = createClient({
        baseUrl: 'http://server',
        token: 'token-one',
        fetchFn: async () => ({
            ok: true,
            status: 204,
            json: async () => {
                parsed = true;
            }
        })
    });

    const result = await client.postNoContent('/Sessions/Playing', { ItemId: 'item-one' });

    assert.equal(result, undefined);
    assert.equal(parsed, false);
});

test('should expose authenticated media headers without content type', () => {
    const client = createClient({ baseUrl: 'http://server', token: 'token-one' });

    assert.deepEqual(Object.keys(client.mediaHeaders), ['Authorization']);
    assert.match(client.mediaHeaders.Authorization, /Token="token-one"$/);
});

test('should fetch authenticated text without JSON parsing', async () => {
    const client = createClient({
        baseUrl: 'http://server',
        token: 'token-one',
        fetchFn: async () => ({
            ok: true,
            status: 200,
            text: async () => 'subtitle text'
        })
    });

    assert.equal(await client.getText('/Videos/item/source/Subtitles/2/0/Stream.srt'), 'subtitle text');
});
