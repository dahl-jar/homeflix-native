import { Ionicons } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import { useRouter, usePathname } from 'expo-router';
import { View, Pressable, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, radius } from '../theme/tokens.js';

const LIBRARY_ICONS = {
    movies: 'film-outline',
    tvshows: 'tv-outline'
};

/** Floating glass nav pill, the native twin of the web bottom nav. */
export function BottomPill({ views }) {
    const router = useRouter();
    const pathname = usePathname();
    const insets = useSafeAreaInsets();

    const entries = [
        { key: 'home', icon: 'home-outline', href: '/(tabs)/home', active: pathname.endsWith('/home') },
        ...views.map((view) => ({
            key: view.Id,
            icon: LIBRARY_ICONS[view.CollectionType] ?? 'albums-outline',
            href: `/(tabs)/library/${view.Id}`,
            active: pathname.includes(view.Id)
        })),
        { key: 'search', icon: 'search-outline', href: '/(tabs)/search', active: pathname.endsWith('/search') },
        { key: 'profile', icon: 'person-outline', href: '/(tabs)/profile', active: pathname.endsWith('/profile') }
    ];

    return (
        <View style={[styles.wrap, { bottom: insets.bottom + 10 }]} pointerEvents="box-none">
            <BlurView intensity={40} tint="dark" style={styles.pill}>
                {entries.map((entry) => (
                    <Pressable
                        key={entry.key}
                        style={styles.item}
                        onPress={() => router.replace(entry.href)}
                    >
                        <Ionicons
                            name={entry.icon}
                            size={24}
                            color={entry.active ? '#ffffff' : colors.textDim}
                        />
                    </Pressable>
                ))}
            </BlurView>
        </View>
    );
}

const styles = StyleSheet.create({
    wrap: {
        position: 'absolute',
        left: 0,
        right: 0,
        alignItems: 'center'
    },
    pill: {
        flexDirection: 'row',
        gap: 6,
        paddingHorizontal: 18,
        paddingVertical: 10,
        borderRadius: radius.pill,
        borderWidth: 1,
        borderColor: colors.glassBorder,
        backgroundColor: colors.glassBg,
        overflow: 'hidden'
    },
    item: {
        width: 52,
        alignItems: 'center',
        paddingVertical: 6
    }
});
