import assert from 'node:assert/strict';
import { test } from 'node:test';

import { watchPlaybackProgress } from '../playbackProgressWatcher.js';

function deferred() {
    let resolve;
    const promise = new Promise((next) => {
        resolve = next;
    });
    return { promise, resolve };
}

function entry(sequence, status, sourceAttempt) {
    return {
        Sequence: sequence,
        StageId: 'analysis',
        Label: 'Analyzing source',
        Order: 20,
        Status: status,
        SourceAttempt: sourceAttempt,
        SourceCount: 3,
        Reason: status === 'failed' ? 'source_preparation_failed' : null
    };
}

test('should replay failure before the next dynamic candidate stage', async () => {
    const secondPoll = deferred();
    const events = [];
    const frames = [];
    let calls = 0;
    const client = {
        async get() {
            calls += 1;
            if (calls === 1) return { Events: [entry(1, 'active', 1)] };
            if (calls === 2) {
                await secondPoll.promise;
                return { Events: [entry(2, 'failed', 1), entry(3, 'active', 2)] };
            }
            return { Events: [] };
        }
    };
    const watcher = watchPlaybackProgress({
        client,
        pipelineId: 'pipeline-one',
        attemptId: 'pipeline-one-a1',
        onProgress: (event) => events.push({
            status: event.status,
            sourceAttempt: event.sourceAttempt
        }),
        waitForFailureTransition: async () => events.push('failure-hold'),
        waitForFrame: async () => frames.push('frame'),
        waitForPoll: () => new Promise((resolve) => setTimeout(resolve, 0))
    });

    await new Promise((resolve) => setTimeout(resolve, 0));
    secondPoll.resolve();
    while (events.length < 3) {
        await new Promise((resolve) => setTimeout(resolve, 0));
    }
    await watcher.stop();

    assert.deepEqual(events, [
        { status: 'active', sourceAttempt: 1 },
        { status: 'failed', sourceAttempt: 1 },
        'failure-hold',
        { status: 'active', sourceAttempt: 2 }
    ]);
    assert.equal(frames.length, 3);
});

test('should perform a final drain and ignore progress transport failure', async () => {
    const pollWait = deferred();
    const events = [];
    let calls = 0;
    const client = {
        async get() {
            calls += 1;
            if (calls === 1) throw new Error('progress unavailable');
            return { Events: [entry(4, 'active', 2)] };
        }
    };
    const watcher = watchPlaybackProgress({
        client,
        pipelineId: 'pipeline-one',
        attemptId: 'pipeline-one-a1',
        onProgress: (event) => events.push(event),
        waitForFrame: async () => {},
        waitForPoll: () => pollWait.promise
    });

    await new Promise((resolve) => setTimeout(resolve, 0));
    const stopping = watcher.stop();
    pollWait.resolve();
    await stopping;

    assert.equal(events.length, 1);
    assert.equal(events[0].sequence, 4);
});
