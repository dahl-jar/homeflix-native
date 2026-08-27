import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors } from '../../theme/tokens.js';

import { formatPlaybackTime, seekPositionFromPress } from './playerControlsModel.js';

export function PlaybackTimeline({ durationSeconds, positionSeconds, onSeek }) {
    const [width, setWidth] = useState(0);
    const progress = durationSeconds > 0
        ? Math.min(1, Math.max(0, positionSeconds / durationSeconds))
        : 0;

    return (
        <View style={styles.container}>
            <Pressable
                accessibilityLabel="Playback position"
                accessibilityRole="adjustable"
                onLayout={(event) => setWidth(event.nativeEvent.layout.width)}
                onPress={(event) => onSeek(seekPositionFromPress(
                    event.nativeEvent.locationX,
                    width,
                    durationSeconds
                ))}
                style={styles.touchTarget}
            >
                <View style={styles.track}>
                    <View style={[styles.progress, { width: `${progress * 100}%` }]} />
                    <View style={[styles.thumb, { left: `${progress * 100}%` }]} />
                </View>
            </Pressable>
            <Text style={styles.time}>
                {formatPlaybackTime(positionSeconds)} / {formatPlaybackTime(durationSeconds)}
            </Text>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1
    },
    touchTarget: {
        height: 30,
        justifyContent: 'center'
    },
    track: {
        height: 4,
        borderRadius: 2,
        backgroundColor: 'rgba(255, 255, 255, 0.35)'
    },
    progress: {
        height: 4,
        borderRadius: 2,
        backgroundColor: colors.accent
    },
    thumb: {
        position: 'absolute',
        top: -5,
        width: 14,
        height: 14,
        marginLeft: -7,
        borderRadius: 7,
        backgroundColor: colors.accent
    },
    time: {
        color: colors.text,
        fontSize: 12,
        marginTop: 2
    }
});
