import { Pressable, Text, View, StyleSheet } from 'react-native';

import { colors, radius } from '../../theme/tokens.js';
import { BackdropImage } from '../BackdropImage/BackdropImage.js';
import { CardFallback } from '../CardFallback/CardFallback.js';

const THUMB_RATIO = 16 / 9;

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
                    <CardFallback
                        label={item.SeriesName ?? item.Name}
                        numberOfLines={2}
                        paddingHorizontal={10}
                    />
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
    }
});
