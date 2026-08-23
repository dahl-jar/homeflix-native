import assert from 'node:assert/strict';
import test from 'node:test';

import { pipelineWaitReassurance } from '../pipelineWaitCopy.js';

test('should stay silent while a step is still fresh', () => {
    assert.equal(pipelineWaitReassurance(0), null);
    assert.equal(pipelineWaitReassurance(7999), null);
});

test('should reassure once a step runs slow', () => {
    assert.equal(pipelineWaitReassurance(8000), 'Still working on it');
    assert.equal(pipelineWaitReassurance(19999), 'Still working on it');
});

test('should explain a long-running step', () => {
    assert.equal(
        pipelineWaitReassurance(20000),
        'This can take a moment for large libraries'
    );
    assert.equal(
        pipelineWaitReassurance(120000),
        'This can take a moment for large libraries'
    );
});
