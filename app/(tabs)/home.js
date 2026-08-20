import { useEffect, useState } from 'react';
import { ScrollView, View, Text, Pressable, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { Image } from 'expo-image';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useSession } from '../../src/session/SessionProvider.js';
import { fetchRecommendations } from '../../src/api/recommendations.js';
import { fetchResume, fetchUserViews, fetchLatest, fetchItem } from '../../src/api/items.js';
import { billboardItems } from '../../src/features/home/billboard.js';
import { BillboardView } from '../../src/features/home/BillboardView.js';
import { MediaRow } from '../../src/features/home/MediaRow.js';
import { HomeSkeleton } from '../../src/features/home/HomeSkeleton.js';
import { colors, spacing } from '../../src/theme/tokens.js';

const BOTTOM_CLEARANCE = 110;

export default function HomeScreen() {
    const session = useSession();
    const router = useRouter();
    const insets = useSafeAreaInsets();
    const [billboard, setBillboard] = useState([]);
    const [continueWatching, setContinueWatching] = useState([]);
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!session.client || !session.userId) return;
        const { client, userId } = session;

        fetchRecommendations(client)
            .then(async (recs) => {
                const topRanked = [...recs].sort((a, b) => a.Rank - b.Rank).slice(0, 8);
                const resolved = await Promise.all(
                    topRanked.map((rec) => fetchItem(client, userId, rec.ItemId).catch(() => null))
                );
                const byId = Object.fromEntries(
                    resolved.filter(Boolean).map((item) => [item.Id, item])
                );
                setBillboard(billboardItems(topRanked, byId));
            })
            .catch(() => setBillboard([]));

        fetchResume(client, userId).then((result) => setContinueWatching(result.Items));

        fetchUserViews(client, userId).then(async (viewsResult) => {
            const latest = await Promise.all(
                viewsResult.Items.map(async (view) => ({
                    view,
                    items: await fetchLatest(client, userId, view.Id).catch(() => [])
                }))
            );
            setRows(latest.filter((row) => row.items.length > 0));
            setLoading(false);
        });
    }, [session.client, session.userId]);

    return (
        <ScrollView
            style={styles.screen}
            contentContainerStyle={{ paddingBottom: BOTTOM_CLEARANCE }}
        >
            <View style={[styles.header, { top: insets.top + 4 }]}>
                <Text style={styles.wordmark}>HOMEFLIX</Text>
                <Pressable onPress={() => router.replace('/(tabs)/profile')}>
                    {session.user?.PrimaryImageTag ? (
                        <Image
                            source={{
                                uri: `${session.serverUrl}/Users/${session.user.Id}/Images/Primary?tag=${session.user.PrimaryImageTag}&quality=90`
                            }}
                            style={styles.headerAvatar}
                        />
                    ) : (
                        <View style={[styles.headerAvatar, styles.headerAvatarFallback]} />
                    )}
                </Pressable>
            </View>
            <BillboardView items={billboard} baseUrl={session.serverUrl} />
            {loading && billboard.length === 0 ? <HomeSkeleton /> : null}
            <MediaRow
                title="Continue Watching"
                items={continueWatching}
                baseUrl={session.serverUrl}
                variant="progress"
            />
            {rows.map((row) => (
                <MediaRow
                    key={row.view.Id}
                    title={`Recently Added in ${row.view.Name}`}
                    items={row.items}
                    baseUrl={session.serverUrl}
                />
            ))}
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: colors.bg
    },
    header: {
        position: 'absolute',
        left: spacing.screen,
        right: spacing.screen,
        zIndex: 2,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between'
    },
    headerAvatar: {
        width: 30,
        height: 30,
        borderRadius: 6
    },
    headerAvatarFallback: {
        backgroundColor: '#5f312e'
    },
    wordmark: {
        color: colors.accent,
        fontSize: 17,
        fontWeight: '900',
        letterSpacing: 3,
        textShadowColor: 'rgba(0, 0, 0, 0.7)',
        textShadowRadius: 6
    }
});
