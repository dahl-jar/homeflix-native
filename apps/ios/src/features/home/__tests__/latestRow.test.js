import assert from 'node:assert/strict';
import { test } from 'node:test';

import { dropStreamRows } from '../latestRow.js';

test('should drop leaked stream rows with http paths', () => {
    const items = [
        { Id: 'movie-one', Path: 'library://movies/movie-one' },
        { Id: 'stream-row', Path: 'https://media.example.com/items/remote-row' },
        { Id: 'movie-two', Path: undefined }
    ];

    assert.deepEqual(
        dropStreamRows(items).map((item) => item.Id),
        ['movie-one', 'movie-two']
    );
});
