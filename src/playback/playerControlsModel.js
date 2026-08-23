function finiteSeconds(seconds) {
    return Number.isFinite(seconds) && seconds > 0 ? Math.floor(seconds) : 0;
}

export function formatPlaybackTime(seconds) {
    const total = finiteSeconds(seconds);
    const hours = Math.floor(total / 3_600);
    const minutes = Math.floor((total % 3_600) / 60);
    const remainingSeconds = total % 60;
    const minuteText = hours > 0 ? String(minutes).padStart(2, '0') : String(minutes);
    const secondText = String(remainingSeconds).padStart(2, '0');
    return hours > 0 ? `${hours}:${minuteText}:${secondText}` : `${minuteText}:${secondText}`;
}

export function seekPositionFromPress(locationX, width, durationSeconds) {
    if (width <= 0 || durationSeconds <= 0) return 0;
    const progress = Math.min(1, Math.max(0, locationX / width));
    return progress * durationSeconds;
}

export function nextVideoContentFit(contentFit) {
    return contentFit === 'cover' ? 'contain' : 'cover';
}

export function shouldScheduleAutoHide({ playbackStatus, hidden, pinned }) {
    return playbackStatus === 'playing' && !hidden && !pinned;
}
