import { useEffect, useState } from 'react';
import { Text, View, Pressable, StyleSheet, useWindowDimensions } from 'react-native';
import { useRouter } from 'expo-router';
import { LinearGradient } from 'expo-linear-gradient';

import { BackdropImage } from '../../components/BackdropImage.js';
import { PlayPill } from '../../components/PlayPill.js';
import { backdropUrl } from '../../api/imageUrl.js';
import { colors, radius, spacing } from '../../theme/tokens.js';

const ROTATE_MS = 12000;
const BILLBOARD_RATIO = 16 / 10;

export function BillboardView({ items, baseUrl }) {
    const router = useRouter();
    const { width } = useWindowDimensions();
    const [index, setIndex] = useState(0);

    useEffect(() => {
        if (items.length < 2) return undefined;
        const timer = setInterval(() => {
            setIndex((current) => (current + 1) % items.length);
        }, ROTATE_MS);
        return () => clearInterval(timer);
    }, [items.length]);

    if (items.length === 0) return null;
    const item = items[index % items.length];

    return (
        <View>
            <BackdropImage
                uri={backdropUrl(baseUrl, item, 1280)}
                style={{ width, height: width / BILLBOARD_RATIO }}
            />
            <LinearGradient
                colors={['transparent', 'rgba(21, 19, 19, 0.65)', colors.bg]}
                style={styles.shade}
            />
            <View style={styles.content}>
                <Text numberOfLines={1} style={styles.title}>
                    {item.Name}
                </Text>
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
        height: '65%'
    },
    content: {
        position: 'absolute',
        left: spacing.screen,
        right: spacing.screen,
        bottom: spacing.screen
    },
    title: {
        color: colors.text,
        fontSize: 26,
        fontWeight: '700',
        textShadowColor: 'rgba(0, 0, 0, 0.8)',
        textShadowRadius: 8
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
