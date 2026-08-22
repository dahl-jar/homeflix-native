const TICKS_PER_SECOND = 10_000_000;
const SUPPORTED_TYPES = new Set(['Intro', 'Recap', 'Outro']);

export function normalizeMediaSegments(segments) {
    return (segments ?? [])
        .filter((segment) =>
            SUPPORTED_TYPES.has(segment.Type)
            && typeof segment.Id === 'string'
            && Number.isFinite(segment.StartTicks)
            && Number.isFinite(segment.EndTicks)
            && segment.StartTicks >= 0
            && segment.EndTicks - segment.StartTicks >= TICKS_PER_SECOND
        )
        .map((segment) => ({
            id: segment.Id,
            type: segment.Type,
            startTicks: segment.StartTicks,
            endTicks: segment.EndTicks
        }))
        .sort((left, right) => left.startTicks - right.startTicks);
}

export function findActiveSkipSegment(segments, positionTicks, dismissedIds) {
    return segments.find((segment) =>
        !dismissedIds.has(segment.id)
        && positionTicks >= segment.startTicks
        && positionTicks < segment.endTicks
    ) ?? null;
}
