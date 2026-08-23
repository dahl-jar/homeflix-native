const DETAIL_SEASON_FIELDS = 'ItemCounts,PrimaryImageAspectRatio,CanDelete';

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

export function searchItems(
    client,
    userId,
    { term, startIndex = 0, limit = 40, localOnly = false, signal }
) {
    return client.get(
        '/Items',
        {
            userId,
            searchTerm: localOnly ? `local:${term}` : term,
            recursive: true,
            includeItemTypes: 'Movie,Series',
            startIndex,
            limit
        },
        { signal }
    );
}

export function fetchSimilar(client, userId, itemId, limit = 12) {
    return client.get(`/Items/${itemId}/Similar`, { userId, limit });
}

export function fetchItem(client, userId, itemId) {
    return client.get(`/Users/${userId}/Items/${itemId}`);
}

export function fetchDetailItem(client, userId, itemId) {
    return client.get(`/Users/${userId}/Items/${itemId}`, {
        includeMediaSources: false,
        includeMediaStreams: false,
        waitForSeriesTree: false
    });
}

export function fetchItemsByIds(client, userId, itemIds) {
    if (itemIds.length === 0) return Promise.resolve([]);

    return client.get('/Items', {
        userId,
        ids: itemIds.join(','),
        enableImages: true,
        enableUserData: true,
        enableTotalRecordCount: false,
        fields: 'Overview'
    }).then((result) => result.Items);
}

export function fetchSeasons(client, userId, seriesId) {
    return client.get(`/Shows/${seriesId}/Seasons`, {
        userId,
        fields: DETAIL_SEASON_FIELDS
    });
}

export function fetchEpisodes(client, userId, seriesId, seasonId) {
    return client.get(`/Shows/${seriesId}/Episodes`, { userId, seasonId, fields: 'Overview' });
}
