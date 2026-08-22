import { useEffect, useState } from 'react';

import { fetchItem } from '../api/items.js';

import { resolvePlayableItem } from './playbackItemResolver.js';

export function usePlaybackItem(client, userId, itemId) {
    const [state, setState] = useState({ itemId: null, item: null, failed: false });

    useEffect(() => {
        if (!client || !userId || !itemId) return;
        fetchItem(client, userId, itemId)
            .then((item) => resolvePlayableItem(client, userId, item))
            .then((item) => setState({ itemId, item, failed: false }))
            .catch(() => setState({ itemId, item: null, failed: true }));
    }, [client, itemId, userId]);

    return state.itemId === itemId
        ? state
        : { itemId, item: null, failed: false };
}
