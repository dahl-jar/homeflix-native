export async function fetchFollowingEpisodeCandidates(client, { userId, seriesId, itemId }) {
    const result = await client.get(`/Shows/${seriesId}/Episodes`, {
        userId,
        startItemId: itemId,
        limit: 2,
        isMissing: false,
        enableImages: true,
        enableUserData: true,
        enableTotalRecordCount: false,
        fields: 'Overview,PrimaryImageAspectRatio'
    });
    return result.Items ?? [];
}

export async function fetchSeriesEpisodes(client, { userId, seriesId, itemId }) {
    const result = await client.get(`/Shows/${seriesId}/Episodes`, {
        userId,
        startItemId: itemId,
        isMissing: false,
        enableImages: true,
        enableUserData: true,
        enableTotalRecordCount: false,
        fields: 'Overview,PrimaryImageAspectRatio'
    });
    return result.Items ?? [];
}
