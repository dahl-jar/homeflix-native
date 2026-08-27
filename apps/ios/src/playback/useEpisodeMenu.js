import { useCallback, useState } from 'react';

import { createEpisodeMenuEntries, isSameEpisode } from './episodeMenuModel.js';
import { fetchSeriesEpisodes } from './episodeQueueApi.js';

const EMPTY_STATE = { seriesId: null, status: 'idle', items: [] };

export function useEpisodeMenu({ client, item, userId, onSelect }) {
    const [state, setState] = useState(EMPTY_STATE);
    const available = item.Type === 'Episode' && Boolean(item.SeriesId);
    const currentState = available && state.seriesId === item.SeriesId ? state : EMPTY_STATE;

    const load = useCallback(async () => {
        if (!available || currentState.status === 'loading' || currentState.status === 'ready') return;
        const seriesId = item.SeriesId;
        setState({ seriesId, status: 'loading', items: [] });
        try {
            const items = await fetchSeriesEpisodes(client, { userId, seriesId, itemId: item.Id });
            setState((current) => current.seriesId === seriesId
                ? { seriesId, status: 'ready', items }
                : current);
        } catch {
            setState((current) => current.seriesId === seriesId
                ? { seriesId, status: 'failed', items: [] }
                : current);
        }
    }, [available, client, currentState.status, item.Id, item.SeriesId, userId]);

    const select = useCallback((itemId) => {
        const selected = currentState.items.find((candidate) => candidate.Id === itemId);
        if (!selected || isSameEpisode(selected, item)) return;
        onSelect(selected);
    }, [currentState.items, item, onSelect]);

    return {
        available,
        entries: createEpisodeMenuEntries(currentState.items, item),
        load,
        select,
        seriesName: item.SeriesName,
        status: currentState.status
    };
}
