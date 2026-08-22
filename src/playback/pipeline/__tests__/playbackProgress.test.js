import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    createPlaybackProgress,
    transitionPlaybackProgress
} from '../playbackProgress.js';

function stage(id, label, order, status = 'active', fields = {}) {
    return {
        type: 'stage_progress',
        stageId: id,
        label,
        order,
        status,
        ...fields
    };
}

function statuses(progress) {
    return progress.stages.map(({ id, status }) => `${id}:${status}`);
}

test('should add an unknown server stage from its descriptor', () => {
    let progress = createPlaybackProgress();

    progress = transitionPlaybackProgress(
        progress,
        stage('provider-health', 'Checking provider health', 15)
    );
    progress = transitionPlaybackProgress(
        progress,
        stage('sources', 'Checking sources', 10, 'complete')
    );

    assert.deepEqual(progress.stages.map(({ id, label }) => ({ id, label })), [
        { id: 'sources', label: 'Checking sources' },
        { id: 'provider-health', label: 'Checking provider health' }
    ]);
    assert.deepEqual(statuses(progress), [
        'sources:complete',
        'provider-health:active'
    ]);
});

test('should retain a failed candidate before the next candidate starts', () => {
    let progress = transitionPlaybackProgress(
        createPlaybackProgress(),
        stage('analysis', 'Analyzing source', 20, 'active', { sourceAttempt: 1 })
    );
    progress = transitionPlaybackProgress(
        progress,
        stage('analysis', 'Analyzing source', 20, 'failed', {
            reason: 'source preparation failed',
            sourceAttempt: 1
        })
    );

    assert.equal(progress.stages[0].status, 'failed');
    assert.equal(progress.sourceAttempt, 1);
    assert.equal(progress.reason, 'source preparation failed');

    progress = transitionPlaybackProgress(
        progress,
        stage('analysis', 'Analyzing source', 20, 'active', { sourceAttempt: 2 })
    );

    assert.equal(progress.stages[0].status, 'active');
    assert.equal(progress.sourceAttempt, 2);
    assert.equal(progress.reason, null);
});

test('should keep video hidden until signed source release', () => {
    const released = transitionPlaybackProgress(
        createPlaybackProgress(),
        { type: 'release_completed' }
    );

    assert.equal(createPlaybackProgress().videoVisible, false);
    assert.equal(released.videoVisible, true);
});

test('should keep the overlay until native playback starts', () => {
    let progress = transitionPlaybackProgress(
        createPlaybackProgress(),
        stage('player', 'Starting player', 1000)
    );
    progress = transitionPlaybackProgress(progress, { type: 'release_completed' });
    const playing = transitionPlaybackProgress(progress, { type: 'playing' });

    assert.equal(progress.visible, true);
    assert.equal(playing.visible, false);
    assert.deepEqual(statuses(playing), ['player:complete']);
});

test('should reset dynamic stages for a source retry', () => {
    const active = transitionPlaybackProgress(
        createPlaybackProgress(),
        stage('analysis', 'Analyzing source', 20)
    );
    const retry = transitionPlaybackProgress(active, { type: 'retry' });

    assert.equal(retry.attempt, 2);
    assert.equal(retry.videoVisible, false);
    assert.equal(retry.visible, true);
    assert.deepEqual(retry.stages, []);
});

test('should retain a terminal failure without inventing a stage', () => {
    const failed = transitionPlaybackProgress(
        createPlaybackProgress(),
        { type: 'failed', reason: 'no compatible playback source' }
    );

    assert.equal(failed.visible, true);
    assert.equal(failed.videoVisible, false);
    assert.equal(failed.reason, 'no compatible playback source');
    assert.deepEqual(failed.stages, []);
});
