import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createPlaybackRuntimeRegistry } from '../playbackRuntimeRegistry.js';

function fakeRuntime(name, events, startError = null) {
    return {
        async start() {
            events.push(`${name}:start`);
            if (startError) throw startError;
        },
        async stop() {
            events.push(`${name}:stop`);
        }
    };
}

test('should stop active playback before starting a replacement', async () => {
    const events = [];
    const registry = createPlaybackRuntimeRegistry();
    const first = fakeRuntime('first', events);
    const second = fakeRuntime('second', events);

    await registry.activate(first);
    await registry.activate(second);

    assert.deepEqual(events, ['first:start', 'first:stop', 'second:start']);
});

test('should leave replacement playback active during stale cleanup', async () => {
    const events = [];
    const registry = createPlaybackRuntimeRegistry();
    const first = fakeRuntime('first', events);
    const second = fakeRuntime('second', events);

    await registry.activate(first);
    await registry.activate(second);
    await registry.deactivate(first);

    assert.equal(events.filter((event) => event === 'second:stop').length, 0);
});

test('should activate playback after a previous start failure', async () => {
    const events = [];
    const registry = createPlaybackRuntimeRegistry();
    const failed = fakeRuntime('failed', events, new Error('start failed'));
    const next = fakeRuntime('next', events);

    await assert.rejects(registry.activate(failed), /start failed/);
    await registry.activate(next);

    assert.deepEqual(events, ['failed:start', 'next:start']);
});
