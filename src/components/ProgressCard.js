import { Pressable, Text, View, StyleSheet } from 'react-native';

import { BackdropImage } from './BackdropImage.js';
import { colors, radius } from '../theme/tokens.js';

const THUMB_RATIO = 16 / 9;

/** Continue Watching card: wide thumb, progress bar, series/episode caption. */
export function ProgressCard({ item, imageUri, width, onPress }) {
    const percent = item.UserData?.PlayedPercentage ?? 0;
    const caption =
        item.Type === 'Episode'
            ? `${item.SeriesName} · S${item.ParentIndexNumber ?? '?'}:E${item.IndexNumber ?? '?'}`
            : item.Name;

    return (
        <Pressable onPress={onPress} style={{ width }}>
            <View>
                <BackdropImage
                    uri={imageUri}
                    style={[styles.thumb, { width, height: width / THUMB_RATIO }]}
                />
                {!imageUri ? (
                    <View style={styles.fallback}>
                        <Text numberOfLines={2} style={styles.fallbackText}>
                            {item.SeriesName ?? item.Name}
                        </Text>
                    </View>
                ) : null}
                <View style={styles.track}>
                    <View style={[styles.fill, { width: `${percent}%` }]} />
                </View>
            </View>
            <Text numberOfLines={1} style={styles.caption}>
                {caption}
            </Text>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    thumb: {
        borderRadius: radius.card
    },
    track: {
        position: 'absolute',
        left: 0,
        right: 0,
        bottom: 0,
        height: 3,
        backgroundColor: 'rgba(255, 255, 255, 0.25)',
        borderBottomLeftRadius: radius.card,
        borderBottomRightRadius: radius.card,
        overflow: 'hidden'
    },
    fill: {
        height: '100%',
        backgroundColor: colors.accent
    },
    caption: {
        color: colors.textDim,
        fontSize: 12,
        marginTop: 4
    },
    fallback: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 10
    },
    fallbackText: {
        color: colors.textDim,
        fontSize: 13,
        fontWeight: '600',
        textAlign: 'center'
    }
});
