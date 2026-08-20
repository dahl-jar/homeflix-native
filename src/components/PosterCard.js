import { Pressable, Text, View, StyleSheet } from 'react-native';

import { BackdropImage } from './BackdropImage.js';
import { colors, radius } from '../theme/tokens.js';

const POSTER_RATIO = 2 / 3;

export function PosterCard({ item, imageUri, width, onPress, showTitle = false }) {
    return (
        <Pressable onPress={onPress} style={{ width }}>
            <View>
                <BackdropImage
                    uri={imageUri}
                    style={[styles.poster, { width, height: width / POSTER_RATIO }]}
                />
                {!imageUri ? (
                    <View style={styles.fallback}>
                        <Text numberOfLines={3} style={styles.fallbackText}>
                            {item.Name}
                        </Text>
                    </View>
                ) : null}
            </View>
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
        borderRadius: radius.card,
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: 'rgba(255, 255, 255, 0.10)'
    },
    title: {
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
        paddingHorizontal: 8
    },
    fallbackText: {
        color: colors.textDim,
        fontSize: 13,
        fontWeight: '600',
        textAlign: 'center'
    }
});
