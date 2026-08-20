import { useEffect, useState } from 'react';
import { ScrollView, View, Text, StyleSheet } from 'react-native';

import { useSession } from '../../src/session/SessionProvider.js';
import { fetchRecommendations } from '../../src/api/recommendations.js';
import { fetchResume, fetchUserViews, fetchLatest, fetchItem } from '../../src/api/items.js';
import { billboardItems } from '../../src/features/home/billboard.js';
import { BillboardView } from '../../src/features/home/BillboardView.js';
import { MediaRow } from '../../src/features/home/MediaRow.js';
import { colors, spacing } from '../../src/theme/tokens.js';

const BOTTOM_CLEARANCE = 110;

export default function HomeScreen() {
    const session = useSession();
    const [billboard, setBillboard] = useState([]);
    const [continueWatching, setContinueWatching] = useState([]);
    const [rows, setRows] = useState([]);

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
        });
    }, [session.client, session.userId]);

    return (
        <ScrollView
            style={styles.screen}
            contentContainerStyle={{ paddingBottom: BOTTOM_CLEARANCE }}
        >
            <View style={styles.header}>
                <Text style={styles.wordmark}>HOMEFLIX</Text>
            </View>
            <BillboardView items={billboard} baseUrl={session.serverUrl} />
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
        top: 60,
        left: spacing.screen,
        zIndex: 2
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
