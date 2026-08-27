import { Pressable, StyleSheet } from 'react-native';

import { NextEpisodeOverlay } from '../episodes/NextEpisodeOverlay.js';
import { SkipSegmentButton } from '../skip-segments/SkipSegmentButton.js';

import { PlayerControls } from './PlayerControls.js';

export function PlaybackInteractiveLayer({
    contentFit,
    controls,
    episodeMenu,
    item,
    locked,
    nextEpisode,
    onExit,
    onToggleContentFit,
    onToggleLock,
    pipeline,
    playback,
    serverUrl,
    skip
}) {
    if (pipeline.visible) return null;
    return (
        <>
            <Pressable
                accessibilityElementsHidden
                onPress={controls.toggle}
                style={StyleSheet.absoluteFill}
            />
            <PlayerControls
                contentFit={contentFit}
                episodeMenu={episodeMenu}
                item={item}
                locked={locked}
                nextEpisode={nextEpisode}
                onExit={onExit}
                onInteract={controls.show}
                onMenuOpenChange={controls.setPinned}
                onToggleContentFit={onToggleContentFit}
                onToggleLock={onToggleLock}
                playback={playback}
                serverUrl={serverUrl}
                visible={controls.visible}
            />
            {!locked && !nextEpisode.active ? (
                <SkipSegmentButton segment={skip.activeSegment} onPress={skip.skip} />
            ) : null}
            {!locked && nextEpisode.active ? (
                <NextEpisodeOverlay
                    episode={nextEpisode.nextEpisode}
                    onCancel={nextEpisode.cancel}
                    onPlayNext={nextEpisode.playNext}
                    remainingSeconds={nextEpisode.remainingSeconds}
                />
            ) : null}
        </>
    );
}
