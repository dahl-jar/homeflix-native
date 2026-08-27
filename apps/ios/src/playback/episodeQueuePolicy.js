export function selectFollowingEpisode(items, currentItemId) {
    const currentIndex = items.findIndex((item) => item.Id === currentItemId);
    if (currentIndex < 0) return null;
    return items.slice(currentIndex + 1).find((item) =>
        item.Type === 'Episode' && item.IsMissing !== true
    ) ?? null;
}
