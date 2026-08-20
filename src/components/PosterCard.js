import { Pressable, Text, StyleSheet } from 'react-native';

import { BackdropImage } from './BackdropImage.js';
import { colors, radius } from '../theme/tokens.js';

const POSTER_RATIO = 2 / 3;

export function PosterCard({ item, imageUri, width, onPress, showTitle = false }) {
    return (
        <Pressable onPress={onPress} style={{ width }}>
            <BackdropImage
                uri={imageUri}
                style={[styles.poster, { width, height: width / POSTER_RATIO }]}
            />
            {showTitle ? (
                <Text numberOfLines={1} style={styles.title}>
                    {item.Name}
                </Text>
            ) : null}
        </Pressable>
    );
}

const styles = StyleSheet.create({
    poster: {
        borderRadius: radius.card
    },
    title: {
        color: colors.textDim,
        fontSize: 12,
        marginTop: 4
    }
});
