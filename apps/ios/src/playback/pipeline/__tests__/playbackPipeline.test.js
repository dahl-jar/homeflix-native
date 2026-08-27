import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createPlaybackPipeline } from '../playbackPipeline.js';

test('should create bounded pipeline and attempt identifiers', () => {
    const ids = ['pipeline-one'];
    const pipeline = createPlaybackPipeline({
        item: { Id: 'item-one', Name: 'Movie', Path: '/provider/secret' },
        now: () => 100,
        createId: () => ids.shift()
    });

    const attempt = pipeline.startAttempt('source-one');

    assert.equal(pipeline.pipelineId, 'pipeline-one');
    assert.deepEqual(attempt, {
        attempt: 1,
        attemptId: 'pipeline-one-a1',
        mediaSourceId: 'source-one'
    });
    assert.doesNotMatch(JSON.stringify(pipeline), /provider|secret|Path/);
});

test('should increment event sequence and elapsed time', () => {
    let time = 1_000;
    const pipeline = createPlaybackPipeline({
        item: { Id: 'item-one', Name: 'Episode' },
        now: () => time,
        createId: () => 'pipeline-one'
    });
    pipeline.startAttempt('source-one');
    time = 1_250;

    const first = pipeline.nextEvent('source_selected', { stage: 'preflight' });
    time = 1_600;
    const second = pipeline.nextEvent('playback_started');

    assert.equal(first.sequence, 1);
    assert.equal(first.elapsedMs, 250);
    assert.equal(first.attemptId, 'pipeline-one-a1');
    assert.equal(first.mediaSourceId, 'source-one');
    assert.equal(second.sequence, 2);
    assert.equal(second.elapsedMs, 600);
});

test('should bind the server-selected source to an existing attempt', () => {
    const pipeline = createPlaybackPipeline({
        item: { Id: 'item-one', Name: 'Episode' },
        now: () => 1_000,
        createId: () => 'pipeline-one'
    });
    const attempt = pipeline.startAttempt(null);

    const selected = pipeline.selectAttemptSource('source-two');
    const event = pipeline.nextEvent('source_selected');

    assert.equal(selected.attemptId, attempt.attemptId);
    assert.equal(selected.mediaSourceId, 'source-two');
    assert.equal(event.attemptId, attempt.attemptId);
    assert.equal(event.mediaSourceId, 'source-two');
});

test('should reject identifiers outside the server correlation format', () => {
    assert.throws(
        () => createPlaybackPipeline({
            item: { Id: 'item-one', Name: 'Movie' },
            now: () => 0,
            createId: () => 'pipeline/with/query?token=one'
        }),
        /pipeline id/
    );
});
