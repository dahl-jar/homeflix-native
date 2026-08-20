import { test } from 'node:test';
import assert from 'node:assert/strict';

import { SORT_OPTIONS, buildLibraryQuery } from '../filters.js';

test('should map each sort option onto sortBy and sortOrder', () => {
    const byKey = Object.fromEntries(SORT_OPTIONS.map((option) => [option.key, option]));

    assert.deepEqual(
        buildLibraryQuery({ sort: byKey.az }),
        { sortBy: 'SortName', sortOrder: 'Ascending' }
    );
    assert.deepEqual(
        buildLibraryQuery({ sort: byKey.recent }),
        { sortBy: 'DateCreated', sortOrder: 'Descending' }
    );
    assert.deepEqual(
        buildLibraryQuery({ sort: byKey.rating }),
        { sortBy: 'CommunityRating', sortOrder: 'Descending' }
    );
});

test('should include the genre id only when a genre is selected', () => {
    const sort = SORT_OPTIONS[0];

    assert.equal('genreIds' in buildLibraryQuery({ sort }), false);
    assert.equal(
        buildLibraryQuery({ sort, genreId: 'genre-one' }).genreIds,
        'genre-one'
    );
});
