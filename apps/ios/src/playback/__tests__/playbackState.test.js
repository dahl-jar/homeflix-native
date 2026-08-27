import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createPlaybackState, reducePlaybackState } from '../playbackState.js';

test('should move through loading ready playing paused and ended', () => {
    let state = createPlaybackState();
    state = reducePlaybackState(state, { type: 'LOAD' });
    state = reducePlaybackState(state, { type: 'SOURCE_READY' });
    state = reducePlaybackState(state, { type: 'PLAYING' });
    state = reducePlaybackState(state, { type: 'PAUSED' });
    state = reducePlaybackState(state, { type: 'ENDED' });

    assert.equal(state.status, 'ended');
});

test('should retain position and duration updates while playing', () => {
    let state = { ...createPlaybackState(), status: 'playing' };

    state = reducePlaybackState(state, { type: 'TIME_UPDATED', positionSeconds: 42, durationSeconds: 90 });

    assert.equal(state.positionSeconds, 42);
    assert.equal(state.durationSeconds, 90);
});

test('should enter recovery and preserve the source attempt', () => {
    const state = reducePlaybackState(
        { ...createPlaybackState(), status: 'playing', attemptId: 'attempt-one' },
        { type: 'RECOVERING', reason: 'native_error' }
    );

    assert.equal(state.status, 'recovering');
    assert.equal(state.attemptId, 'attempt-one');
    assert.equal(state.reason, 'native_error');
});

test('should ignore an illegal playing event from idle', () => {
    const state = createPlaybackState();

    assert.equal(reducePlaybackState(state, { type: 'PLAYING' }), state);
});

test('should retain a terminal failure reason', () => {
    const state = reducePlaybackState(
        { ...createPlaybackState(), status: 'loading' },
        { type: 'FAILED', reason: 'no_compatible_source' }
    );

    assert.equal(state.status, 'failed');
    assert.equal(state.reason, 'no_compatible_source');
});
