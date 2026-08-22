import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { PlaybackPipelineOverlay } from './pipeline/PlaybackPipelineOverlay.js';
import { PlaybackInteractiveLayer } from './PlaybackInteractiveLayer.js';
import { PlaybackVideo } from './PlaybackVideo.js';
import { nextVideoContentFit } from './playerControlsModel.js';
import { usePlayerControlsVisibility } from './usePlayerControlsVisibility.js';

export function PlaybackScreen({ episodeMenu, item, playback, skip, nextEpisode, onExit, serverUrl }) {
    const [locked, setLocked] = useState(false);
    const [contentFit, setContentFit] = useState('contain');
    const pipeline = playback.snapshot.pipeline;
    const controls = usePlayerControlsVisibility(playback.snapshot.status);

    return (
        <View style={styles.screen}>
            <StatusBar hidden />
            <PlaybackVideo
                contentFit={contentFit}
                pipeline={pipeline}
                player={playback.player}
            />
            <PlaybackInteractiveLayer
                contentFit={contentFit}
                controls={controls}
                episodeMenu={episodeMenu}
                item={item}
                locked={locked}
                nextEpisode={nextEpisode}
                onExit={onExit}
                onToggleContentFit={() => setContentFit(nextVideoContentFit)}
                onToggleLock={() => setLocked((current) => !current)}
                pipeline={pipeline}
                playback={playback}
                serverUrl={serverUrl}
                skip={skip}
            />
            <PlaybackPipelineOverlay
                item={item}
                onExit={onExit}
                progress={pipeline}
                serverUrl={serverUrl}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: '#000000'
    }
});
