import { test } from 'node:test';
import assert from 'node:assert/strict';

import { SORT_OPTIONS, RATING_OPTIONS, STATUS_OPTIONS, buildLibraryQuery, decadesFromYears } from '../filters.js';

const sortByKey = Object.fromEntries(SORT_OPTIONS.map((option) => [option.key, option]));
const defaultSelection = { sort: sortByKey.az };

test('should map each sort option onto sortBy and sortOrder', () => {
    assert.deepEqual(
        buildLibraryQuery({ sort: sortByKey.az }),
        { sortBy: 'SortName', sortOrder: 'Ascending' }
    );
    assert.deepEqual(
        buildLibraryQuery({ sort: sortByKey.recent }),
        { sortBy: 'DateCreated', sortOrder: 'Descending' }
    );
    assert.deepEqual(
        buildLibraryQuery({ sort: sortByKey.rating }),
        { sortBy: 'CommunityRating', sortOrder: 'Descending' }
    );
});

test('should include the genre name only when a genre is selected', () => {
    assert.equal('genres' in buildLibraryQuery(defaultSelection), false);
    assert.equal(
        buildLibraryQuery({ ...defaultSelection, genre: 'K-Drama' }).genres,
        'K-Drama'
    );
});

test('should derive decade options from the library years, newest first', () => {
    const options = decadesFromYears([1994, 1999, 2003, 2021, 1957]);

    assert.deepEqual(options.map((option) => option.label), ['2020s', '2000s', '1990s', '1950s']);
});

test('should expand a decade into its ten years', () => {
    const nineties = decadesFromYears([1994]).find((option) => option.label === '1990s');

    assert.equal(
        buildLibraryQuery({ ...defaultSelection, decade: nineties }).years,
        '1990,1991,1992,1993,1994,1995,1996,1997,1998,1999'
    );
    assert.equal('years' in buildLibraryQuery(defaultSelection), false);
});

test('should map a rating floor onto minCommunityRating', () => {
    const eightPlus = RATING_OPTIONS.find((option) => option.label === '8+');

    assert.equal(
        buildLibraryQuery({ ...defaultSelection, rating: eightPlus }).minCommunityRating,
        8
    );
    assert.equal('minCommunityRating' in buildLibraryQuery(defaultSelection), false);
});

test('should map watched status onto isPlayed', () => {
    const unwatched = STATUS_OPTIONS.find((option) => option.label === 'Unwatched');
    const watched = STATUS_OPTIONS.find((option) => option.label === 'Watched');

    assert.equal(buildLibraryQuery({ ...defaultSelection, status: unwatched }).isPlayed, false);
    assert.equal(buildLibraryQuery({ ...defaultSelection, status: watched }).isPlayed, true);
    assert.equal('isPlayed' in buildLibraryQuery(defaultSelection), false);
});
