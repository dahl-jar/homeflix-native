export const SORT_OPTIONS = [
    { key: 'az', label: 'A–Z', sortBy: 'SortName', sortOrder: 'Ascending' },
    { key: 'recent', label: 'Recently Added', sortBy: 'DateCreated', sortOrder: 'Descending' },
    { key: 'rating', label: 'Top Rated', sortBy: 'CommunityRating', sortOrder: 'Descending' }
];

/** Translates the selected sort and genre chips into /Items query params. */
export function buildLibraryQuery({ sort, genreId }) {
    const query = { sortBy: sort.sortBy, sortOrder: sort.sortOrder };
    if (genreId) query.genreIds = genreId;
    return query;
}
