import { useEffect, useState } from 'react';

import { fetchDetailItem } from '../../api/items/items.js';
import { resolvePlayableItem } from '../sources/playbackItemResolver.js';

export function usePlaybackItem(client, userId, itemId) {
    const [state, setState] = useState({ itemId: null, item: null, failed: false });

    useEffect(() => {
        if (!client || !userId || !itemId) return;
        fetchDetailItem(client, userId, itemId)
            .then((item) => resolvePlayableItem(client, userId, item))
            .then((item) => setState({ itemId, item, failed: false }))
            .catch(() => setState({ itemId, item: null, failed: true }));
    }, [client, itemId, userId]);

    return state.itemId === itemId
        ? state
        : { itemId, item: null, failed: false };
}
