import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createPlaybackRuntimeLease } from '../playbackRuntimeLease.js';
import { createPlaybackRuntimeRegistry } from '../playbackRuntimeRegistry.js';

function runtimeFactory(events) {
    let nextId = 0;
    return (options) => {
        const id = ++nextId;
        const itemId = options.negotiationOptions.item.Id;
        events.push(`create:${id}:${itemId}`);
        return {
            emit(snapshot) {
                options.onSnapshot(snapshot);
            },
            getSnapshot() {
                return { status: 'playing', itemId };
            },
            async start() {
                events.push(`start:${id}`);
            },
            async stop() {
                events.push(`stop:${id}`);
            }
        };
    };
}

function playbackOptions(itemId, player = {}) {
    return {
        player,
        negotiationOptions: {
            client: {},
            item: { Id: itemId },
            platform: 'ios',
            preferredMediaSourceId: null,
            serverUrl: 'http://server',
            startTimeTicks: 0,
            userId: 'user-one'
        }
    };
}

test('should reuse an active runtime when playback inputs are unchanged', async () => {
    const events = [];
    const snapshots = [];
    const lease = createPlaybackRuntimeLease({
        createRuntime: runtimeFactory(events),
        registry: createPlaybackRuntimeRegistry()
    });
    const options = playbackOptions('item-one');
    const first = lease.acquire(options, (snapshot) => snapshots.push(`first:${snapshot.status}`));
    await Promise.resolve();

    const cleanup = lease.release(first);
    const second = lease.acquire(options, (snapshot) => snapshots.push(`second:${snapshot.status}`));
    first.emit({ status: 'paused' });
    await cleanup;

    assert.equal(second, first);
    assert.deepEqual(events, ['create:1:item-one', 'start:1']);
    assert.deepEqual(snapshots, ['first:playing', 'second:playing', 'second:paused']);
});

test('should reuse an active runtime after the same item metadata refreshes', async () => {
    const events = [];
    const player = {};
    const lease = createPlaybackRuntimeLease({
        createRuntime: runtimeFactory(events),
        registry: createPlaybackRuntimeRegistry()
    });
    const firstOptions = playbackOptions('item-one', player);
    const first = lease.acquire(firstOptions, () => {});
    await Promise.resolve();

    const refreshedOptions = playbackOptions('item-one', player);
    refreshedOptions.negotiationOptions.startTimeTicks = 900_000_000;
    const cleanup = lease.release(first);
    const second = lease.acquire(refreshedOptions, () => {});
    await cleanup;

    assert.equal(second, first);
    assert.deepEqual(events, ['create:1:item-one', 'start:1']);
});

test('should replace the runtime when the playback item changes', async () => {
    const events = [];
    const player = {};
    const lease = createPlaybackRuntimeLease({
        createRuntime: runtimeFactory(events),
        registry: createPlaybackRuntimeRegistry()
    });
    const first = lease.acquire(playbackOptions('item-one', player), () => {});
    await Promise.resolve();

    const second = lease.acquire(playbackOptions('item-two', player), () => {});
    await Promise.resolve();
    await Promise.resolve();

    assert.notEqual(second, first);
    assert.deepEqual(events, [
        'create:1:item-one',
        'start:1',
        'create:2:item-two',
        'stop:1',
        'start:2'
    ]);
});
