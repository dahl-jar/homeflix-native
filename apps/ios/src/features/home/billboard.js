const BILLBOARD_LIMIT = 8;

/**
 * Orders recommendation rows by rank, resolves them to full items, drops
 * ids the item fetch could not resolve, and caps the slideshow.
 */
export function billboardItems(recommendations, resolvedById) {
    return [...recommendations]
        .sort((a, b) => a.Rank - b.Rank)
        .map((rec) => resolvedById[rec.ItemId])
        .filter((item) => item != null)
        .slice(0, BILLBOARD_LIMIT);
}
