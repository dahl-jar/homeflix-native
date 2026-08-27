import { useCallback } from 'react';

import { useEpisodeMenu } from '../episodes/useEpisodeMenu.js';
import { useNextEpisode } from '../episodes/useNextEpisode.js';
import { useSkipSegments } from '../skip-segments/useSkipSegments.js';

import { PlaybackScreen } from './PlaybackScreen.js';
import { usePlaybackSession } from './usePlaybackSession.js';

export function PlaybackController({ active, item, onAdvance, onExit, ...sessionOptions }) {
    const playback = usePlaybackSession({ ...sessionOptions, active, item });
    const { seekTo, stop } = playback;
    const advance = useCallback(async (nextItem) => {
        await stop();
        onAdvance(nextItem);
    }, [onAdvance, stop]);
    const skip = useSkipSegments({
        client: sessionOptions.client,
        itemId: item.Id,
        positionSeconds: playback.snapshot.positionSeconds,
        seekTo
    });
    const nextEpisode = useNextEpisode({
        client: sessionOptions.client,
        item,
        activeSegment: skip.activeSegment,
        playbackStatus: playback.snapshot.status,
        userId: sessionOptions.userId,
        onAdvance: advance
    });
    const episodeMenu = useEpisodeMenu({
        client: sessionOptions.client,
        item,
        userId: sessionOptions.userId,
        onSelect: advance
    });
    const exit = useCallback(async () => {
        await stop();
        onExit();
    }, [onExit, stop]);

    return (
        <PlaybackScreen
            episodeMenu={episodeMenu}
            item={item}
            playback={playback}
            skip={skip}
            nextEpisode={nextEpisode}
            onExit={exit}
            serverUrl={sessionOptions.serverUrl}
        />
    );
}
