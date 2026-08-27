import { fetchSeriesPlaybackEpisode } from './playbackItemApi.js';

export async function resolvePlayableItem(client, userId, item) {
    if (item.Type !== 'Series') return item;
    const episode = await fetchSeriesPlaybackEpisode(client, userId, item.Id);
    if (!episode) throw new Error('series has no playable episode');
    return episode;
}
