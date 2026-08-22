function episodeLabel(item) {
    if ((item.ParentIndexNumber ?? 0) === 0 && item.IndexNumber != null) {
        return `Special ${item.IndexNumber} · ${item.Name}`;
    }
    if (item.ParentIndexNumber != null && item.IndexNumber != null) {
        return `S${item.ParentIndexNumber}:E${item.IndexNumber} · ${item.Name}`;
    }
    return item.Name;
}

export function isSameEpisode(first, second) {
    if (!first || !second) return false;
    if (first.Id === second.Id) return true;
    if (first.ParentIndexNumber == null || second.ParentIndexNumber == null) return false;
    if (first.IndexNumber == null || second.IndexNumber == null) return false;
    return first.ParentIndexNumber === second.ParentIndexNumber
        && first.IndexNumber === second.IndexNumber;
}

function isLaterEpisode(candidate, current) {
    if (candidate.ParentIndexNumber == null || current.ParentIndexNumber == null) return false;
    if (candidate.IndexNumber == null || current.IndexNumber == null) return false;
    if (candidate.ParentIndexNumber !== current.ParentIndexNumber) {
        return candidate.ParentIndexNumber > current.ParentIndexNumber;
    }
    return candidate.IndexNumber > current.IndexNumber;
}

export function createEpisodeMenuEntries(items, currentItem) {
    const playable = items.filter((item) => item.Type === 'Episode' && item.IsMissing !== true);
    const currentIndex = playable.findIndex((item) => isSameEpisode(item, currentItem));
    const forward = currentIndex >= 0
        ? playable.slice(currentIndex)
        : currentItem?.Type === 'Episode'
            ? [currentItem, ...playable.filter((item) => isLaterEpisode(item, currentItem))]
            : [];
    return forward.map((item) => ({
        current: isSameEpisode(item, currentItem),
        episode: item,
        key: item.Id,
        label: episodeLabel(item)
    }));
}
