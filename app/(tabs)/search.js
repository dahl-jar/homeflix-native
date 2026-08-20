import { useEffect, useMemo, useState } from 'react';
import { FlatList, Text, TextInput, View, StyleSheet, useWindowDimensions } from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useSession } from '../../src/session/SessionProvider.js';
import { searchItems, fetchItem } from '../../src/api/items.js';
import { fetchRecommendations } from '../../src/api/recommendations.js';
import { createSearchController } from '../../src/features/search/debounce.js';
import { PosterCard } from '../../src/components/PosterCard.js';
import { ScreenBackground } from '../../src/components/ScreenBackground.js';
import { primaryUrl } from '../../src/api/imageUrl.js';
import { colors, radius, spacing } from '../../src/theme/tokens.js';

const COLUMNS = 3;
const BOTTOM_CLEARANCE = 110;

export default function SearchScreen() {
    const session = useSession();
    const router = useRouter();
    const insets = useSafeAreaInsets();
    const { width } = useWindowDimensions();
    const [results, setResults] = useState([]);
    const [popular, setPopular] = useState([]);
    const [query, setQuery] = useState('');

    const cardWidth = (width - spacing.screen * 2 - spacing.card * (COLUMNS - 1)) / COLUMNS;

    useEffect(() => {
        if (!session.client || !session.userId) return;
        const { client, userId } = session;
        fetchRecommendations(client)
            .then(async (recs) => {
                const topRanked = [...recs].sort((a, b) => a.Rank - b.Rank).slice(0, 12);
                const resolved = await Promise.all(
                    topRanked.map((rec) => fetchItem(client, userId, rec.ItemId).catch(() => null))
                );
                setPopular(resolved.filter(Boolean));
            })
            .catch(() => setPopular([]));
    }, [session.client, session.userId]);

    const controller = useMemo(() => {
        if (!session.client || !session.userId) return null;
        const { client, userId } = session;
        return createSearchController({
            run: async (query, signal) => {
                const result = await searchItems(client, userId, query);
                if (!signal.aborted) setResults(result.Items);
            }
        });
    }, [session.client, session.userId]);

    return (
        <View style={[styles.screen, { paddingTop: insets.top + 12 }]}>
            <ScreenBackground />
            <Text style={styles.screenTitle}>Search</Text>
            <TextInput
                style={styles.input}
                placeholder="Films, series, anime"
                placeholderTextColor={colors.textDim}
                autoCorrect={false}
                autoCapitalize="none"
                onChangeText={(text) => {
                    setQuery(text);
                    if (text.trim() === '') setResults([]);
                    controller?.onQuery(text);
                }}
            />
            {query.trim() === '' && popular.length > 0 ? (
                <Text style={styles.popularLabel}>Popular on Homeflix</Text>
            ) : null}
            <FlatList
                data={query.trim() === '' ? popular : results}
                numColumns={COLUMNS}
                keyExtractor={(item) => item.Id}
                columnWrapperStyle={styles.row}
                contentContainerStyle={{ paddingBottom: BOTTOM_CLEARANCE }}
                renderItem={({ item }) => (
                    <PosterCard
                        item={item}
                        imageUri={primaryUrl(session.serverUrl, item, 300)}
                        width={cardWidth}
                        showTitle
                        onPress={() => router.push(`/details/${item.Id}`)}
                    />
                )}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: colors.bg,
        paddingHorizontal: spacing.screen
    },
    screenTitle: {
        color: colors.text,
        fontSize: 28,
        fontWeight: '700',
        marginBottom: 14
    },
    popularLabel: {
        color: colors.textDim,
        fontSize: 13,
        letterSpacing: 1,
        textTransform: 'uppercase',
        marginBottom: 12
    },
    input: {
        backgroundColor: colors.bgRaised,
        borderRadius: radius.button,
        color: colors.text,
        fontSize: 16,
        paddingHorizontal: 16,
        paddingVertical: 12,
        marginBottom: 16
    },
    row: {
        gap: spacing.card,
        marginBottom: 14
    }
});
