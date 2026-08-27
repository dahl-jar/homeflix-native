const BILLBOARD_LIMIT = 8;

export function billboardItems(recommendations, resolvedById) {
    return [...recommendations]
        .sort((a, b) => a.Rank - b.Rank)
        .map((rec) => resolvedById[rec.ItemId])
        .filter((item) => item != null)
        .slice(0, BILLBOARD_LIMIT);
}
