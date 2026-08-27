import assert from 'node:assert/strict';
import { test } from 'node:test';

import { exitPlaybackRoute } from '../playerNavigation.js';

test('should restore portrait before returning to the previous screen', async () => {
    const calls = [];
    const router = {
        canGoBack: () => true,
        back: () => calls.push('back'),
        replace: (route) => calls.push(route)
    };

    await exitPlaybackRoute(router, async () => calls.push('portrait'));

    assert.deepEqual(calls, ['portrait', 'back']);
});

test('should restore portrait before replacing a root player route with home', async () => {
    const calls = [];
    const router = {
        canGoBack: () => false,
        back: () => calls.push('back'),
        replace: (route) => calls.push(route)
    };

    await exitPlaybackRoute(router, async () => calls.push('portrait'));

    assert.deepEqual(calls, ['portrait', '/(tabs)/home']);
});
