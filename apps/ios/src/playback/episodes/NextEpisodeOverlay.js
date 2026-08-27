import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, radius } from '../../theme/tokens.js';

export function NextEpisodeOverlay({ episode, remainingSeconds, onCancel, onPlayNext }) {
    if (!episode) return null;
    return (
        <View style={styles.panel}>
            <Text numberOfLines={1} style={styles.title}>{episode.Name}</Text>
            <Text style={styles.countdown}>Next episode in {remainingSeconds}</Text>
            <View style={styles.actions}>
                <Pressable style={styles.secondary} onPress={onCancel}>
                    <Text style={styles.secondaryText}>Keep watching</Text>
                </Pressable>
                <Pressable style={styles.primary} onPress={onPlayNext}>
                    <Text style={styles.primaryText}>Play next</Text>
                </Pressable>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    panel: {
        position: 'absolute',
        left: 20,
        right: 20,
        bottom: 48,
        borderRadius: radius.card,
        backgroundColor: colors.bgRaised,
        borderColor: colors.glassBorder,
        borderWidth: 1,
        padding: 16
    },
    title: {
        color: colors.text,
        fontSize: 17,
        fontWeight: '700'
    },
    countdown: {
        color: colors.textDim,
        fontSize: 13,
        marginTop: 4
    },
    actions: {
        flexDirection: 'row',
        justifyContent: 'flex-end',
        gap: 10,
        marginTop: 14
    },
    secondary: {
        paddingHorizontal: 14,
        paddingVertical: 10
    },
    secondaryText: {
        color: colors.text
    },
    primary: {
        borderRadius: radius.button,
        backgroundColor: '#ffffff',
        paddingHorizontal: 16,
        paddingVertical: 10
    },
    primaryText: {
        color: '#141414',
        fontWeight: '700'
    }
});
