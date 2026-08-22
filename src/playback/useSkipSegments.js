import { useCallback, useEffect, useMemo, useState } from 'react';

import { findActiveSkipSegment, normalizeMediaSegments } from './mediaSegmentPolicy.js';
import { fetchMediaSegments } from './mediaSegmentsApi.js';

const SEGMENT_TYPES = ['Intro', 'Recap', 'Outro'];
const TICKS_PER_SECOND = 10_000_000;

export function useSkipSegments({ client, itemId, positionSeconds, seekTo }) {
    const [segmentState, setSegmentState] = useState({ itemId: null, segments: [] });
    const [dismissedState, setDismissedState] = useState({ itemId: null, ids: new Set() });

    useEffect(() => {
        if (!client || !itemId) return;
        fetchMediaSegments(client, itemId, SEGMENT_TYPES)
            .then((segments) => setSegmentState({ itemId, segments: normalizeMediaSegments(segments) }))
            .catch(() => setSegmentState({ itemId, segments: [] }));
    }, [client, itemId]);

    const segments = useMemo(
        () => segmentState.itemId === itemId ? segmentState.segments : [],
        [itemId, segmentState]
    );
    const dismissedIds = useMemo(
        () => dismissedState.itemId === itemId ? dismissedState.ids : new Set(),
        [dismissedState, itemId]
    );
    const activeSegment = useMemo(() => findActiveSkipSegment(
        segments,
        positionSeconds * TICKS_PER_SECOND,
        dismissedIds
    ), [dismissedIds, positionSeconds, segments]);

    const dismiss = useCallback(() => {
        if (!activeSegment) return;
        setDismissedState((current) => {
            const ids = current.itemId === itemId ? new Set(current.ids) : new Set();
            ids.add(activeSegment.id);
            return { itemId, ids };
        });
    }, [activeSegment, itemId]);

    const skip = useCallback(() => {
        if (!activeSegment) return;
        seekTo(activeSegment.endTicks / TICKS_PER_SECOND);
        dismiss();
    }, [activeSegment, dismiss, seekTo]);

    return { activeSegment, dismiss, skip };
}
