import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { primaryUrl } from '../api/imageUrl.js';
import { BackdropImage } from '../components/BackdropImage.js';
import { CardFallback } from '../components/CardFallback.js';
import { runtimeText } from '../features/detail/format.js';
import { colors, radius } from '../theme/tokens.js';

function playbackProgress(episode) {
    if (!episode.RunTimeTicks) return 0;
    const position = episode.UserData?.PlaybackPositionTicks ?? 0;
    return Math.min(1, Math.max(0, position / episode.RunTimeTicks));
}

export function EpisodePickerRow({ compact, entry, selected, serverUrl, onPress }) {
    const { episode } = entry;
    const imageUrl = primaryUrl(serverUrl, episode, compact ? 320 : 400);
    const progress = playbackProgress(episode);

    return (
        <Pressable
            accessibilityLabel={`Play ${entry.label}`}
            accessibilityRole="button"
            accessibilityState={{ selected }}
            onPress={onPress}
            style={({ pressed }) => [
                styles.row,
                compact ? styles.rowCompact : styles.rowRegular,
                selected && styles.rowSelected,
                pressed && styles.rowPressed
            ]}
        >
            <View style={[styles.imageWrap, compact ? styles.imageCompact : styles.imageRegular]}>
                <BackdropImage uri={imageUrl} style={styles.image} />
                {!imageUrl ? <CardFallback label={entry.label} numberOfLines={2} paddingHorizontal={10} /> : null}
                {selected ? (
                    <View style={styles.playingBadge}>
                        <Ionicons color={colors.text} name="play" size={18} />
                    </View>
                ) : null}
                {progress > 0 ? (
                    <View style={styles.progressTrack}>
                        <View style={[styles.progress, { width: `${progress * 100}%` }]} />
                    </View>
                ) : null}
            </View>
            <View style={styles.meta}>
                <Text numberOfLines={1} style={styles.title}>{entry.label}</Text>
                {episode.RunTimeTicks ? <Text style={styles.runtime}>{runtimeText(episode.RunTimeTicks)}</Text> : null}
                {episode.Overview ? (
                    <Text numberOfLines={compact ? 2 : 3} style={styles.overview}>{episode.Overview}</Text>
                ) : null}
            </View>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    row: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        borderRadius: radius.card,
        borderWidth: 1,
        borderColor: 'transparent',
        padding: 8
    },
    rowCompact: {
        minHeight: 104
    },
    rowRegular: {
        minHeight: 118
    },
    rowSelected: {
        backgroundColor: 'rgba(255, 255, 255, 0.08)',
        borderColor: colors.glassBorder
    },
    rowPressed: {
        opacity: 0.7
    },
    imageWrap: {
        overflow: 'hidden',
        borderRadius: radius.card,
        backgroundColor: colors.bgRaised
    },
    imageCompact: {
        width: 142,
        height: 80
    },
    imageRegular: {
        width: 164,
        height: 92
    },
    image: {
        width: '100%',
        height: '100%'
    },
    playingBadge: {
        position: 'absolute',
        top: '50%',
        left: '50%',
        width: 36,
        height: 36,
        marginLeft: -18,
        marginTop: -18,
        borderRadius: 18,
        backgroundColor: 'rgba(0, 0, 0, 0.72)',
        alignItems: 'center',
        justifyContent: 'center',
        paddingLeft: 2
    },
    progressTrack: {
        position: 'absolute',
        left: 0,
        right: 0,
        bottom: 0,
        height: 3,
        backgroundColor: 'rgba(255, 255, 255, 0.35)'
    },
    progress: {
        height: 3,
        backgroundColor: colors.accent
    },
    meta: {
        flex: 1,
        minWidth: 0
    },
    title: {
        color: colors.text,
        fontSize: 14,
        fontWeight: '700'
    },
    runtime: {
        color: colors.textDim,
        fontSize: 12,
        marginTop: 3
    },
    overview: {
        color: colors.textDim,
        fontSize: 12,
        lineHeight: 16,
        marginTop: 5
    }
});
