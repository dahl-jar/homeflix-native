import { router } from 'expo-router';

export const playerLauncher = {
    play(item, origin, { mediaSourceId = null } = {}) {
        const mode = origin === 'restart'
            ? 'restart'
            : item.UserData?.PlaybackPositionTicks > 0 ? 'resume' : 'play';
        const params = { itemId: item.Id, mode, origin };
        if (mediaSourceId) params.mediaSourceId = mediaSourceId;
        router.push({ pathname: '/player/[itemId]', params });
    }
};
