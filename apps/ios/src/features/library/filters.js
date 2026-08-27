const YEARS_PER_DECADE = 10;

export const SORT_OPTIONS = [
    { key: 'rating', label: 'Top Rated', sortBy: 'CommunityRating', sortOrder: 'Descending' },
    { key: 'az', label: 'A–Z', sortBy: 'SortName', sortOrder: 'Ascending' },
    { key: 'recent', label: 'Recently Added', sortBy: 'DateCreated', sortOrder: 'Descending' }
];

export function decadesFromYears(years) {
    const starts = [...new Set(years.map((year) => Math.floor(year / YEARS_PER_DECADE) * YEARS_PER_DECADE))];
    return starts
        .sort((a, b) => b - a)
        .map((start) => ({ key: String(start), label: `${start}s`, start }));
}

export const RATING_OPTIONS = [
    { key: '7', label: '7+', min: 7 },
    { key: '8', label: '8+', min: 8 },
    { key: '9', label: '9+', min: 9 }
];

export const STATUS_OPTIONS = [
    { key: 'unwatched', label: 'Unwatched', isPlayed: false },
    { key: 'watched', label: 'Watched', isPlayed: true }
];

export function buildLibraryQuery({ sort, genre, decade, rating, status }) {
    const query = { sortBy: sort.sortBy, sortOrder: sort.sortOrder };
    if (genre) query.genres = genre;
    if (decade) {
        query.years = Array.from({ length: YEARS_PER_DECADE }, (_, i) => decade.start + i).join(',');
    }
    if (rating) query.minCommunityRating = rating.min;
    if (status) query.isPlayed = status.isPlayed;
    return query;
}
