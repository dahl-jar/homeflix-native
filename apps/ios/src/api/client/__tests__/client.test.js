import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createClient, ApiError } from '../client.ts';

const identity = {
    deviceId: 'e9ce56b0-fc28-48f0-a407-146a89cf4d31',
    version: '2.4.1'
};

const clientOptions = (options = {}) => ({
    baseUrl: 'http://server',
    token: 'token-one',
    ...identity,
    ...options
});

const okResponse = (body) => ({
    ok: true,
    status: 200,
    json: async () => body
});

test('should attach authenticated MediaBrowser authorization', async () => {
    const seen = [];
    const client = createClient(clientOptions({
        fetchFn: async (url, options) => {
            seen.push({ url, options });
            return okResponse({});
        }
    }));

    await client.get('/Users/Me');

    const header = seen[0].options.headers.Authorization;
    assert.match(header, /DeviceId="e9ce56b0-fc28-48f0-a407-146a89cf4d31"/);
    assert.match(header, /Version="2.4.1"/);
    assert.match(header, /Token="token-one"$/);
});

test('should send the identity header without a token before sign-in', async () => {
    const seen = [];
    const client = createClient(clientOptions({
        token: '',
        fetchFn: async (url, options) => {
            seen.push(options.headers.Authorization);
            return okResponse({});
        }
    }));

    await client.get('/Users/Public');

    assert.match(seen[0], /^MediaBrowser Client="Homeflix"/);
    assert.doesNotMatch(seen[0], /Token=/);
});

test('should preserve a server failure in ApiError', async () => {
    const client = createClient(clientOptions({
        fetchFn: async () => ({
            ok: false,
            status: 401,
            text: async () => JSON.stringify({ Message: 'The supplied PIN is invalid.' })
        })
    }));

    await assert.rejects(client.get('/Users/Me'), (error) => {
        assert.ok(error instanceof ApiError);
        assert.equal(error.status, 401);
        assert.equal(error.detail, 'The supplied PIN is invalid.');
        assert.match(error.message, /The supplied PIN is invalid\./);
        return true;
    });
});

test('should bound plain-text server error detail', async () => {
    const client = createClient(clientOptions({
        fetchFn: async () => ({
            ok: false,
            status: 500,
            text: async () => 'x'.repeat(1_000)
        })
    }));

    await assert.rejects(client.postNoContent('/Sessions/Playing', {}), (error) => {
        assert.equal(error.detail.length, 500);
        return true;
    });
});

test('should discard JSON errors without a message field', async () => {
    const client = createClient(clientOptions({
        fetchFn: async () => ({
            ok: false,
            status: 500,
            text: async () => JSON.stringify({ token: 'server-secret' })
        })
    }));

    await assert.rejects(client.get('/Users/Me'), (error) => {
        assert.equal(error.detail, null);
        assert.doesNotMatch(error.message, /server-secret/);
        return true;
    });
});

test('should serialize query params onto the url', async () => {
    const seen = [];
    const client = createClient(clientOptions({
        fetchFn: async (url) => {
            seen.push(url);
            return okResponse({});
        }
    }));

    await client.get('/Items', { searchTerm: 'matrix', limit: 20 });

    assert.equal(seen[0], 'http://server/Items?searchTerm=matrix&limit=20');
});

test('should serialize query params on post requests', async () => {
    const seen = [];
    const client = createClient(clientOptions({
        fetchFn: async (url, options) => {
            seen.push({ url, options });
            return okResponse({ result: 'ok' });
        }
    }));

    await client.post('/Items/item-one/PlaybackInfo', { UserId: 'user-one' }, { mediaSourceId: 'source-one' });

    assert.equal(seen[0].url, 'http://server/Items/item-one/PlaybackInfo?mediaSourceId=source-one');
    assert.equal(seen[0].options.body, JSON.stringify({ UserId: 'user-one' }));
});

test('should accept a no-content response without parsing json', async () => {
    let parsed = false;
    const client = createClient(clientOptions({
        fetchFn: async () => ({
            ok: true,
            status: 204,
            json: async () => {
                parsed = true;
            }
        })
    }));

    const result = await client.postNoContent('/Sessions/Playing', { ItemId: 'item-one' });

    assert.equal(result, undefined);
    assert.equal(parsed, false);
});

test('should expose authenticated media headers without content type', () => {
    const client = createClient(clientOptions());

    assert.deepEqual(Object.keys(client.mediaHeaders), ['Authorization']);
    assert.match(client.mediaHeaders.Authorization, /Token="token-one"$/);
});

test('should fetch authenticated text without JSON parsing', async () => {
    const client = createClient(clientOptions({
        fetchFn: async () => ({
            ok: true,
            status: 200,
            text: async () => 'subtitle text'
        })
    }));

    assert.equal(await client.getText('/Videos/item/source/Subtitles/2/0/Stream.srt'), 'subtitle text');
});

test('should forward an abort signal on get requests', async () => {
    const seen = [];
    const controller = new AbortController();
    const client = createClient(clientOptions({
        fetchFn: async (_url, options) => {
            seen.push(options);
            return okResponse({});
        }
    }));

    await client.get('/Items', undefined, { signal: controller.signal });

    assert.equal(seen[0].signal, controller.signal);
});
