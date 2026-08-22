import assert from 'node:assert/strict';
import test from 'node:test';

import { pipelineStageLayout } from '../pipelineStageLayout.js';

test('should fit five dynamic stages inside a portrait viewport', () => {
    assert.deepEqual(pipelineStageLayout(400, 5), {
        centered: true,
        stageWidth: 80
    });
});

test('should center a short dynamic pipeline without stretching its stages', () => {
    assert.deepEqual(pipelineStageLayout(400, 1), {
        centered: true,
        stageWidth: 140
    });
});

test('should contain larger future pipelines in the horizontal scroller', () => {
    assert.deepEqual(pipelineStageLayout(400, 8), {
        centered: false,
        stageWidth: 64
    });
});
