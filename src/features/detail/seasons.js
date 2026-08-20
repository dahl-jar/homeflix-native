/** The season tab selected on open: the first real season, never Specials. */
export function defaultSeasonIndex(seasons) {
    const firstReal = seasons.findIndex((season) => (season.IndexNumber ?? 0) >= 1);
    return firstReal === -1 ? 0 : firstReal;
}
