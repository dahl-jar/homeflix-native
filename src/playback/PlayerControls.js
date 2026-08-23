import { StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '../theme/tokens.js';

import { PlaybackTimeline } from './PlaybackTimeline.js';
import { PlayerBottomActions } from './PlayerBottomActions.js';
import { PlayerIconButton } from './PlayerIconButton.js';

export function PlayerControls({
    contentFit,
    episodeMenu,
    item,
    locked,
    nextEpisode,
    playback,
    serverUrl,
    visible,
    onExit,
    onInteract,
    onMenuOpenChange,
    onToggleContentFit,
    onToggleLock
}) {
    const insets = useSafeAreaInsets();
    const { height, width } = useWindowDimensions();
    const landscape = width > height;
    if (!visible) return null;
    if (locked) {
        return (
            <View pointerEvents="box-none" style={StyleSheet.absoluteFill}>
                <View style={[styles.locked, { paddingTop: landscape ? 12 : Math.max(insets.top, 18) }]}>
                    <PlayerIconButton accessibilityLabel="Unlock controls" icon="lock-closed" onPress={onToggleLock} />
                </View>
            </View>
        );
    }
    const playing = playback.snapshot.status === 'playing';
    const run = (action) => (...args) => {
        onInteract();
        action(...args);
    };

    return (
        <View pointerEvents="box-none" style={StyleSheet.absoluteFill}>
            <View style={styles.scrim} pointerEvents="none" />
            <View style={[
                styles.top,
                {
                    paddingTop: landscape ? 12 : Math.max(insets.top, 18),
                    paddingLeft: Math.max(insets.left, 14),
                    paddingRight: Math.max(insets.right, 14)
                }
            ]}>
                <PlayerIconButton accessibilityLabel="Close player" icon="chevron-back" onPress={onExit} />
                <Text numberOfLines={1} style={styles.title}>{item.Name}</Text>
                <PlayerIconButton
                    accessibilityLabel={contentFit === 'cover' ? 'Fit video' : 'Fill screen'}
                    family="material"
                    icon={contentFit === 'cover' ? 'fullscreen-exit' : 'fullscreen'}
                    onPress={run(onToggleContentFit)}
                />
                <PlayerIconButton accessibilityLabel="Lock controls" icon="lock-open-outline" onPress={onToggleLock} />
            </View>
            <View style={[styles.center, landscape ? styles.centerLandscape : styles.centerPortrait]}>
                <PlayerIconButton
                    accessibilityLabel="Rewind 10 seconds"
                    family="material"
                    icon="replay-10"
                    size={44}
                    onPress={run(() => playback.seekBy(-10))}
                />
                <PlayerIconButton
                    accessibilityLabel={playing ? 'Pause' : 'Play'}
                    icon={playing ? 'pause' : 'play'}
                    onPress={run(playing ? playback.pause : playback.play)}
                    prominent
                />
                <PlayerIconButton
                    accessibilityLabel="Forward 10 seconds"
                    family="material"
                    icon="forward-10"
                    size={44}
                    onPress={run(() => playback.seekBy(10))}
                />
            </View>
            <View style={[
                styles.bottom,
                {
                    left: Math.max(insets.left, landscape ? 20 : 16),
                    right: Math.max(insets.right, landscape ? 20 : 16),
                    paddingBottom: Math.max(insets.bottom, landscape ? 12 : 24)
                }
            ]}>
                <PlaybackTimeline
                    durationSeconds={playback.snapshot.durationSeconds}
                    positionSeconds={playback.snapshot.positionSeconds}
                    onSeek={run(playback.seekTo)}
                />
                <PlayerBottomActions
                    episodeMenu={episodeMenu}
                    nextEpisode={nextEpisode}
                    onInteract={onInteract}
                    onMenuOpenChange={onMenuOpenChange}
                    playback={playback}
                    serverUrl={serverUrl}
                />
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    scrim: {
        ...StyleSheet.absoluteFillObject,
        backgroundColor: 'rgba(0, 0, 0, 0.34)'
    },
    top: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        paddingBottom: 18
    },
    locked: {
        position: 'absolute',
        top: 0,
        left: 14
    },
    title: {
        flex: 1,
        color: colors.text,
        fontSize: 17,
        fontWeight: '700'
    },
    center: {
        position: 'absolute',
        left: 0,
        right: 0,
        top: '50%',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        transform: [{ translateY: -35 }]
    },
    centerLandscape: {
        gap: 120
    },
    centerPortrait: {
        gap: 48
    },
    bottom: {
        position: 'absolute',
        bottom: 0
    }
});
