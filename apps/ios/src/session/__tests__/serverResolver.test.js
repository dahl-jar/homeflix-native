import assert from 'node:assert/strict';
import { test } from 'node:test';

import { parseServerCandidates } from '../serverResolver.js';

test('should parse configured server candidates', () => {
    const candidates = parseServerCandidates(
        ' https://media.example.com/, http://backup.example.test/// '
    );

    assert.deepEqual(candidates, [
        'https://media.example.com',
        'http://backup.example.test'
    ]);
});

test('should omit empty configured server candidates', () => {
    assert.deepEqual(parseServerCandidates(' ,  , '), []);
    assert.deepEqual(parseServerCandidates(undefined), []);
});
