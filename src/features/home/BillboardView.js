import MaskedView from '@react-native-masked-view/masked-view';
import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { Text, View, Pressable, StyleSheet, useWindowDimensions } from 'react-native';

import { backdropUrl } from '../../api/imageUrl.js';
import { BackdropImage } from '../../components/BackdropImage.js';
import { PlayPill } from '../../components/PlayPill.js';
import { colors, radius, spacing } from '../../theme/tokens.js';

const ROTATE_MS = 12000;
const HEIGHT_FACTOR = 1.25;
const GENRE_LIMIT = 3;

export function BillboardView({ items, baseUrl, onActiveItem }) {
    const router = useRouter();
    const { width } = useWindowDimensions();
    const [index, setIndex] = useState(0);

    const item = items.length > 0 ? items[index % items.length] : null;

    useEffect(() => {
        if (item && onActiveItem) onActiveItem(item);
    }, [item, onActiveItem]);

    useEffect(() => {
        if (items.length < 2) return undefined;
        const timer = setInterval(() => {
            setIndex((current) => (current + 1) % items.length);
        }, ROTATE_MS);
        return () => clearInterval(timer);
    }, [items.length]);

    if (!item) return null;

    return (
        <View>
            <MaskedView
                maskElement={
                    <LinearGradient
                        colors={['#000000', '#000000', 'transparent']}
                        locations={[0, 0.45, 0.94]}
                        style={{ flex: 1 }}
                    />
                }
            >
                <BackdropImage
                    uri={backdropUrl(baseUrl, item, 1280)}
                    style={{ width, height: width * HEIGHT_FACTOR }}
                />
            </MaskedView>
            <LinearGradient
                colors={['transparent', 'rgba(21, 19, 19, 0.55)', 'rgba(21, 19, 19, 0.2)']}
                locations={[0, 0.6, 1]}
                style={styles.shade}
            />
            <View style={styles.content}>
                <Text numberOfLines={2} style={styles.title}>
                    {item.Name}
                </Text>
                {item.Genres?.length ? (
                    <Text style={styles.genres}>
                        {item.Genres.slice(0, GENRE_LIMIT).join('  •  ')}
                    </Text>
                ) : null}
                <View style={styles.buttons}>
                    <View style={styles.playWrap}>
                        <PlayPill item={item} origin="billboard" />
                    </View>
                    <Pressable
                        style={styles.moreInfo}
                        onPress={() => router.push(`/details/${item.Id}`)}
                    >
                        <Text style={styles.moreInfoText}>More Info</Text>
                    </Pressable>
                </View>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    shade: {
        position: 'absolute',
        left: 0,
        right: 0,
        bottom: 0,
        height: '82%'
    },
    content: {
        position: 'absolute',
        left: spacing.screen,
        right: spacing.screen,
        bottom: 86
    },
    title: {
        color: colors.text,
        fontSize: 30,
        fontWeight: '800',
        textAlign: 'center',
        textShadowColor: 'rgba(0, 0, 0, 0.8)',
        textShadowRadius: 8
    },
    genres: {
        color: colors.textDim,
        fontSize: 13,
        textAlign: 'center',
        marginTop: 8
    },
    buttons: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
        marginTop: 12
    },
    playWrap: {
        flex: 2
    },
    moreInfo: {
        flex: 1,
        backgroundColor: colors.glassBg,
        borderColor: colors.glassBorder,
        borderWidth: 1,
        borderRadius: radius.button,
        paddingVertical: 13,
        alignItems: 'center'
    },
    moreInfoText: {
        color: colors.text,
        fontSize: 15,
        fontWeight: '500'
    }
});
