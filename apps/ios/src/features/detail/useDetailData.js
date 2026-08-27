import { useEffect, useState } from 'react';

import {
    fetchEpisodes,
    fetchDetailItem,
    fetchSeasons,
    fetchSimilar
} from '../../api/items.js';

import { defaultSeasonIndex } from './seasons.js';

export function useDetailData({ client, itemId, userId }) {
    const [itemState, setItemState] = useState({ itemId: null, value: null });
    const [similarState, setSimilarState] = useState({ itemId: null, items: [] });
    const [seasonState, setSeasonState] = useState({ itemId: null, items: [], selectedIndex: 0 });
    const [episodeState, setEpisodeState] = useState({ seasonId: null, items: [] });

    useEffect(() => {
        if (!client || !userId || !itemId) return;
        fetchDetailItem(client, userId, itemId)
            .then((value) => setItemState({ itemId, value }))
            .catch(() => setItemState({ itemId, value: null }));
        fetchSimilar(client, userId, itemId)
            .then((result) => setSimilarState({ itemId, items: result.Items }))
            .catch(() => setSimilarState({ itemId, items: [] }));
    }, [client, itemId, userId]);

    const item = itemState.itemId === itemId ? itemState.value : null;

    useEffect(() => {
        if (!client || !userId || !item || item.Type !== 'Series') return;
        fetchSeasons(client, userId, item.Id)
            .then((result) => setSeasonState({
                itemId: item.Id,
                items: result.Items,
                selectedIndex: defaultSeasonIndex(result.Items)
            }))
            .catch(() => setSeasonState({ itemId: item.Id, items: [], selectedIndex: 0 }));
    }, [client, item, userId]);

    const seasons = seasonState.itemId === item?.Id ? seasonState.items : [];
    const seasonIndex = seasonState.itemId === item?.Id ? seasonState.selectedIndex : 0;
    const selectedSeason = seasons[seasonIndex] ?? null;

    useEffect(() => {
        if (!client || !userId || !item || !selectedSeason) return;
        fetchEpisodes(client, userId, item.Id, selectedSeason.Id)
            .then((result) => setEpisodeState({ seasonId: selectedSeason.Id, items: result.Items }))
            .catch(() => setEpisodeState({ seasonId: selectedSeason.Id, items: [] }));
    }, [client, item, selectedSeason, userId]);

    const selectSeason = (selectedIndex) => {
        setSeasonState((current) => current.itemId === item?.Id
            ? { ...current, selectedIndex }
            : current);
    };

    return {
        item,
        similar: similarState.itemId === itemId ? similarState.items : [],
        seasons,
        seasonIndex,
        selectSeason,
        episodes: episodeState.seasonId === selectedSeason?.Id ? episodeState.items : []
    };
}
