import { test } from 'node:test';
import assert from 'node:assert/strict';

import { sourceOptions, AUTO_SOURCE_KEY } from '../sources.js';

const GIB = 1024 * 1024 * 1024;

test('should prepend the auto entry and map sources onto picker options', () => {
    const options = sourceOptions([
        { Id: 'source-one', Name: 'Movie 2160p BluRay', Size: 2 * GIB },
        { Id: 'source-two', Name: 'Movie 1080p WEB', Size: null }
    ]);

    assert.equal(options[0].key, AUTO_SOURCE_KEY);
    assert.equal(options[1].key, 'source-one');
    assert.equal(options[1].label, 'Movie 2160p BluRay  ·  2.0 GB');
    assert.equal(options[2].label, 'Movie 1080p WEB');
});

test('should collapse the placeholder-only list to just auto', () => {
    const options = sourceOptions([{ Id: 'item-id', Name: 'The Movie', Size: null }], 'The Movie');

    assert.equal(options.length, 1);
    assert.equal(options[0].key, AUTO_SOURCE_KEY);
});
