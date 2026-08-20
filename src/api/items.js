export function fetchUserViews(client, userId) {
    return client.get('/UserViews', { userId });
}

/** The server merges Next Up into this row; one call returns finished Continue Watching. */
export function fetchResume(client, userId, limit = 12) {
    return client.get('/UserItems/Resume', {
        userId,
        limit,
        mediaTypes: 'Video',
        fields: 'Overview'
    });
}

export function fetchLatest(client, userId, parentId, limit = 24) {
    return client.get(`/Users/${userId}/Items/Latest`, {
        parentId,
        limit,
        groupItems: true,
        fields: 'Path'
    });
}

export function fetchLatestMovies(client, userId, parentId, limit = 16) {
    return client
        .get('/Items', {
            userId,
            parentId,
            recursive: true,
            includeItemTypes: 'Movie',
            sortBy: 'DateCreated',
            sortOrder: 'Descending',
            limit,
            fields: 'Path'
        })
        .then((result) => result.Items);
}

export function fetchLibraryPage(client, userId, { parentId, startIndex, limit, ...query }) {
    return client.get('/Items', {
        userId,
        parentId,
        recursive: true,
        includeItemTypes: 'Movie,Series',
        sortBy: 'SortName',
        startIndex,
        limit,
        fields: 'PrimaryImageAspectRatio',
        ...query
    });
}

export function fetchFilterOptions(client, userId, parentId) {
    return client
        .get('/Items/Filters', { userId, parentId, includeItemTypes: 'Movie,Series' })
        .then((result) => ({ genres: result.Genres ?? [], years: result.Years ?? [] }));
}

export function searchItems(client, userId, term, limit = 40) {
    return client.get('/Items', {
        userId,
        searchTerm: term,
        recursive: true,
        includeItemTypes: 'Movie,Series',
        limit
    });
}

export function fetchSimilar(client, userId, itemId, limit = 12) {
    return client.get(`/Items/${itemId}/Similar`, { userId, limit });
}

export function fetchSources(client, userId, itemId) {
    return client.post(`/Items/${itemId}/PlaybackInfo?userId=${userId}&IsPlayback=false`, {});
}

export function fetchItem(client, userId, itemId) {
    return client.get(`/Users/${userId}/Items/${itemId}`);
}

export function fetchSeasons(client, userId, seriesId) {
    return client.get(`/Shows/${seriesId}/Seasons`, { userId });
}

export function fetchEpisodes(client, userId, seriesId, seasonId) {
    return client.get(`/Shows/${seriesId}/Episodes`, { userId, seasonId, fields: 'Overview' });
}
