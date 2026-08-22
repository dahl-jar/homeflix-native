import { useEffect, useMemo, useState } from 'react';
import { FlatList, Text, TextInput, View, StyleSheet, useWindowDimensions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { searchItems, fetchItemsByIds } from '../../src/api/items.js';
import { fetchRecommendations } from '../../src/api/recommendations.js';
import { GridPosterCard } from '../../src/components/GridPosterCard.js';
import { ScreenBackground } from '../../src/components/ScreenBackground.js';
import { createSearchController } from '../../src/features/search/debounce.js';
import { useSession } from '../../src/session/SessionProvider.js';
import { colors, radius, spacing } from '../../src/theme/tokens.js';

const COLUMNS = 3;
const BOTTOM_CLEARANCE = 110;

export default function SearchScreen() {
    const session = useSession();
    const insets = useSafeAreaInsets();
    const { width } = useWindowDimensions();
    const [results, setResults] = useState([]);
    const [popular, setPopular] = useState([]);
    const [query, setQuery] = useState('');
    const { client, serverUrl, userId } = session;

    const cardWidth = (width - spacing.screen * 2 - spacing.card * (COLUMNS - 1)) / COLUMNS;

    useEffect(() => {
        if (!client || !userId) return;
        fetchRecommendations(client)
            .then(async (recs) => {
                const topRanked = [...recs].sort((a, b) => a.Rank - b.Rank).slice(0, 12);
                const resolved = await fetchItemsByIds(
                    client,
                    userId,
                    topRanked.map((rec) => rec.ItemId)
                );
                const byId = Object.fromEntries(resolved.map((item) => [item.Id, item]));
                setPopular(topRanked.map((rec) => byId[rec.ItemId]).filter(Boolean));
            })
            .catch(() => setPopular([]));
    }, [client, userId]);

    const controller = useMemo(() => {
        if (!client || !userId) return null;
        return createSearchController({
            run: async (query, signal) => {
                const result = await searchItems(client, userId, query);
                if (!signal.aborted) setResults(result.Items);
            }
        });
    }, [client, userId]);

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
                showsVerticalScrollIndicator={false}
                data={query.trim() === '' ? popular : results}
                numColumns={COLUMNS}
                keyExtractor={(item) => item.Id}
                columnWrapperStyle={styles.row}
                contentContainerStyle={{ paddingBottom: BOTTOM_CLEARANCE }}
                renderItem={({ item }) => (
                    <GridPosterCard
                        item={item}
                        serverUrl={serverUrl}
                        width={cardWidth}
                        showTitle
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
