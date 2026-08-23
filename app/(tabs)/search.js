import { useRouter } from 'expo-router';
import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, FlatList, Text, TextInput, View, StyleSheet, useWindowDimensions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { searchItems, fetchItemsByIds } from '../../src/api/items.js';
import { fetchRecommendations } from '../../src/api/recommendations.js';
import { GridPosterCard } from '../../src/components/GridPosterCard.js';
import { ScreenBackground } from '../../src/components/ScreenBackground.js';
import { createPagedSearchController } from '../../src/features/search/pagedSearch.js';
import { selectSearchItem } from '../../src/features/search/selectSearchItem.js';
import { useSession } from '../../src/session/SessionProvider.js';
import { colors, radius, spacing } from '../../src/theme/tokens.js';

const COLUMNS = 3;
const BOTTOM_CLEARANCE = 110;
const SEARCH_PAGE_SIZE = 18;
const PENDING_SEARCH_STATUSES = new Set([
    'debouncing',
    'loading',
    'external',
    'paging'
]);

export default function SearchScreen() {
    const session = useSession();
    const router = useRouter();
    const insets = useSafeAreaInsets();
    const { width } = useWindowDimensions();
    const [results, setResults] = useState([]);
    const [popular, setPopular] = useState([]);
    const [query, setQuery] = useState('');
    const [searchStatus, setSearchStatus] = useState('idle');
    const [selectionFailed, setSelectionFailed] = useState(false);
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
        return createPagedSearchController({
            pageSize: SEARCH_PAGE_SIZE,
            loadLocalPage: (request) => searchItems(client, userId, {
                ...request,
                localOnly: true
            }),
            loadMergedPage: (request) => searchItems(client, userId, {
                ...request,
                localOnly: false
            }),
            onReset: () => setResults([]),
            onResults: ({ items }) => setResults(items),
            onStatus: setSearchStatus
        });
    }, [client, userId]);

    useEffect(() => () => controller?.dispose(), [controller]);

    const selectItem = async (item) => {
        setSelectionFailed(false);
        try {
            await selectSearchItem({
                client,
                userId,
                itemId: item.Id,
                navigate: (route) => router.push(route)
            });
        } catch {
            setSelectionFailed(true);
        }
    };

    const searchPending = PENDING_SEARCH_STATUSES.has(searchStatus);

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
                    setSelectionFailed(false);
                    controller?.onQuery(text);
                }}
            />
            {query.trim() === '' && popular.length > 0 ? (
                <Text style={styles.popularLabel}>Popular on Homeflix</Text>
            ) : null}
            {selectionFailed ? (
                <Text style={styles.errorText}>Could not add this title. Try again.</Text>
            ) : null}
            <FlatList
                showsVerticalScrollIndicator={false}
                data={query.trim() === '' ? popular : results}
                numColumns={COLUMNS}
                keyExtractor={(item) => item.Id}
                columnWrapperStyle={styles.row}
                contentContainerStyle={{ paddingBottom: BOTTOM_CLEARANCE }}
                onEndReachedThreshold={0.5}
                onEndReached={() => controller?.loadMore()}
                ListFooterComponent={searchPending ? (
                    <ActivityIndicator color={colors.textDim} style={styles.loader} />
                ) : null}
                renderItem={({ item }) => (
                    <GridPosterCard
                        item={item}
                        serverUrl={serverUrl}
                        width={cardWidth}
                        showTitle
                        onPress={query.trim() ? () => selectItem(item) : undefined}
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
    errorText: {
        color: colors.danger,
        fontSize: 13,
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
    },
    loader: {
        paddingVertical: 20
    }
});
