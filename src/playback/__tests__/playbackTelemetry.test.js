import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createPlaybackPipeline } from '../playbackPipeline.js';
import { createPlaybackTelemetry } from '../playbackTelemetry.js';

function pipeline() {
    return createPlaybackPipeline({
        item: { Id: 'item-one', Name: 'Movie' },
        now: () => 100,
        createId: () => 'pipeline-one'
    });
}

test('should deliver sanitized events in sequence order', async () => {
    const calls = [];
    const current = pipeline();
    current.startAttempt('source-one');
    const telemetry = createPlaybackTelemetry({
        client: {
            async postNoContent(path, body) {
                calls.push({ path, body });
            }
        },
        pipeline: current,
        wait: async () => {}
    });

    telemetry.log('source_selected', { stage: 'preflight', videoUrl: 'https://secret' });
    telemetry.log('playback_started', { videoCurrentTime: 12 });
    await telemetry.flush();

    assert.deepEqual(calls.map(({ body }) => body.sequence), [1, 2]);
    assert.deepEqual(calls.map(({ path }) => path), [
        '/ClientLog/PlaybackPipeline',
        '/ClientLog/PlaybackPipeline'
    ]);
    assert.doesNotMatch(JSON.stringify(calls), /https:\/\/secret|videoUrl/);
});

test('should retry delivery three times before dropping an event', async () => {
    let attempts = 0;
    const waits = [];
    const current = pipeline();
    const telemetry = createPlaybackTelemetry({
        client: {
            async postNoContent() {
                attempts += 1;
                throw new Error('offline');
            }
        },
        pipeline: current,
        wait: async (delay) => waits.push(delay),
        retryMs: 25
    });

    telemetry.log('pipeline_started');
    await telemetry.flush();

    assert.equal(attempts, 3);
    assert.deepEqual(waits, [25, 25]);
    assert.equal(telemetry.droppedCount, 1);
});

test('should include dropped delivery count on the next event', async () => {
    let fail = true;
    const calls = [];
    const current = pipeline();
    const telemetry = createPlaybackTelemetry({
        client: {
            async postNoContent(path, body) {
                if (fail) throw new Error('offline');
                calls.push({ path, body });
            }
        },
        pipeline: current,
        wait: async () => {}
    });

    telemetry.log('pipeline_started');
    await telemetry.flush();
    fail = false;
    telemetry.log('playback_started');
    await telemetry.flush();

    assert.equal(calls[0].body.telemetryDroppedCount, 1);
});

test('should reject new events when the bounded queue is full', async () => {
    let releaseFirst;
    const firstPost = new Promise((resolve) => {
        releaseFirst = resolve;
    });
    let calls = 0;
    const telemetry = createPlaybackTelemetry({
        client: {
            async postNoContent() {
                calls += 1;
                if (calls === 1) await firstPost;
            }
        },
        pipeline: pipeline(),
        wait: async () => {},
        queueLimit: 2
    });

    assert.equal(telemetry.log('pipeline_started'), true);
    assert.equal(telemetry.log('source_selected'), true);
    assert.equal(telemetry.log('playback_started'), false);
    releaseFirst();
    await telemetry.flush();

    assert.equal(telemetry.droppedCount, 1);
});
