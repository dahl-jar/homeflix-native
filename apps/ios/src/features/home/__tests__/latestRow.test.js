import assert from 'node:assert/strict';
import { test } from 'node:test';

import { dropHttpPathRows } from '../latestRow.js';

test('should drop rows with http paths', () => {
    const items = [
        { Id: 'movie-one', Path: 'library://movies/movie-one' },
        { Id: 'remote-row', Path: 'https://media.example.com/items/remote-row' },
        { Id: 'movie-two', Path: undefined }
    ];

    assert.deepEqual(
        dropHttpPathRows(items).map((item) => item.Id),
        ['movie-one', 'movie-two']
    );
});
