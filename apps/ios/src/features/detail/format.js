const TICKS_PER_MINUTE = 600000000;
const MINUTES_PER_HOUR = 60;

export function runtimeText(runTimeTicks) {
    const totalMinutes = Math.round(runTimeTicks / TICKS_PER_MINUTE);
    const hours = Math.floor(totalMinutes / MINUTES_PER_HOUR);
    const minutes = totalMinutes % MINUTES_PER_HOUR;
    return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
}

export function chips(item) {
    const parts = [];
    if (item.ProductionYear) parts.push(String(item.ProductionYear));
    if (item.RunTimeTicks) parts.push(runtimeText(item.RunTimeTicks));
    if (item.OfficialRating) parts.push(item.OfficialRating);
    return parts;
}

export function starText(item) {
    return item.CommunityRating ? item.CommunityRating.toFixed(1) : null;
}
