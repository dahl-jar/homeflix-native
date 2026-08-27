export function defaultSeasonIndex(seasons) {
    const firstReal = seasons.findIndex((season) => (season.IndexNumber ?? 0) >= 1);
    return firstReal === -1 ? 0 : firstReal;
}
