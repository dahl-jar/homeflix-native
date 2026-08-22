export async function fetchSeriesPlaybackEpisode(client, userId, seriesId) {
    const result = await client.get('/Shows/NextUp', {
        userId,
        seriesId,
        limit: 1,
        enableResumable: true,
        enableRewatching: true,
        enableUserData: true,
        enableTotalRecordCount: false
    });
    return result.Items?.[0] ?? null;
}
