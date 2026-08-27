import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { PlaybackInteractiveLayer } from '../controls/PlaybackInteractiveLayer.js';
import { nextVideoContentFit } from '../controls/playerControlsModel.js';
import { usePlayerControlsVisibility } from '../controls/usePlayerControlsVisibility.js';
import { PlaybackPipelineOverlay } from '../pipeline/PlaybackPipelineOverlay.js';

import { PlaybackVideo } from './PlaybackVideo.js';

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
