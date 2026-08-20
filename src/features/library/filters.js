const YEARS_PER_DECADE = 10;
const NEWEST_DECADE = 2020;
const OLDEST_DECADE = 1950;

export const SORT_OPTIONS = [
    { key: 'az', label: 'A–Z', sortBy: 'SortName', sortOrder: 'Ascending' },
    { key: 'recent', label: 'Recently Added', sortBy: 'DateCreated', sortOrder: 'Descending' },
    { key: 'rating', label: 'Top Rated', sortBy: 'CommunityRating', sortOrder: 'Descending' }
];

export const DECADE_OPTIONS = Array.from(
    { length: (NEWEST_DECADE - OLDEST_DECADE) / YEARS_PER_DECADE + 1 },
    (_, i) => {
        const start = NEWEST_DECADE - i * YEARS_PER_DECADE;
        return { key: String(start), label: `${start}s`, start };
    }
);

export const RATING_OPTIONS = [
    { key: '7', label: '7+', min: 7 },
    { key: '8', label: '8+', min: 8 },
    { key: '9', label: '9+', min: 9 }
];

export const STATUS_OPTIONS = [
    { key: 'unwatched', label: 'Unwatched', isPlayed: false },
    { key: 'watched', label: 'Watched', isPlayed: true }
];

/** Translates the dropdown selections into server-side /Items query params. */
export function buildLibraryQuery({ sort, genreId, decade, rating, status }) {
    const query = { sortBy: sort.sortBy, sortOrder: sort.sortOrder };
    if (genreId) query.genreIds = genreId;
    if (decade) {
        query.years = Array.from({ length: YEARS_PER_DECADE }, (_, i) => decade.start + i).join(',');
    }
    if (rating) query.minCommunityRating = rating.min;
    if (status) query.isPlayed = status.isPlayed;
    return query;
}
