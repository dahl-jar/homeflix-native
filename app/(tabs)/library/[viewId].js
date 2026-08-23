import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { FlatList, ScrollView, Pressable, Text, View, StyleSheet, useWindowDimensions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { fetchLibraryPage, fetchFilterOptions, fetchItem } from '../../../src/api/items.js';
import { createPager } from '../../../src/api/paging.js';
import { DropdownPill } from '../../../src/components/DropdownPill.js';
import { GridPosterCard } from '../../../src/components/GridPosterCard.js';
import { ScreenBackground } from '../../../src/components/ScreenBackground.js';
import {
    SORT_OPTIONS,
    RATING_OPTIONS,
    STATUS_OPTIONS,
    buildLibraryQuery,
    decadesFromYears
} from '../../../src/features/library/filters.js';
import { useSession } from '../../../src/session/SessionProvider.js';
import { colors, spacing } from '../../../src/theme/tokens.js';

const COLUMNS = 3;
const BOTTOM_CLEARANCE = 110;
const PAGE_SIZE = 100;

function createFilterState(viewId) {
    return {
        viewId,
        sortKey: SORT_OPTIONS[0].key,
        genre: null,
        decadeKey: null,
        ratingKey: null,
        statusKey: null
    };
}

export default function LibraryScreen() {
    const session = useSession();
    const insets = useSafeAreaInsets();
    const { width } = useWindowDimensions();
    const { viewId } = useLocalSearchParams();
    const pagerRef = useRef(null);
    const loadingKeyRef = useRef(null);
    const [pageState, setPageState] = useState({ queryKey: null, items: [], total: 0 });
    const [genres, setGenres] = useState([]);
    const [decadeOptions, setDecadeOptions] = useState([]);
    const [viewName, setViewName] = useState('');
    const [filterState, setFilterState] = useState(() => createFilterState(viewId));
    const { client, serverUrl, userId } = session;
    const activeFilters = filterState.viewId === viewId ? filterState : createFilterState(viewId);
    const { sortKey, genre, decadeKey, ratingKey, statusKey } = activeFilters;

    const selection = useMemo(() => ({
        sort: SORT_OPTIONS.find((option) => option.key === sortKey),
        genre,
        decade: decadeOptions.find((option) => option.key === decadeKey) ?? null,
        rating: RATING_OPTIONS.find((option) => option.key === ratingKey) ?? null,
        status: STATUS_OPTIONS.find((option) => option.key === statusKey) ?? null
    }), [decadeKey, decadeOptions, genre, ratingKey, sortKey, statusKey]);
    const queryKey = JSON.stringify([viewId, sortKey, genre, decadeKey, ratingKey, statusKey]);
    const items = pageState.queryKey === queryKey ? pageState.items : [];
    const total = pageState.queryKey === queryKey ? pageState.total : 0;
    const cardWidth = (width - spacing.screen * 2 - spacing.card * (COLUMNS - 1)) / COLUMNS;

    const updateFilters = useCallback((changes) => {
        setFilterState((current) => ({
            ...(current.viewId === viewId ? current : createFilterState(viewId)),
            ...changes
        }));
    }, [viewId]);

    const loadPage = useCallback(async (pager = pagerRef.current) => {
        if (!pager || loadingKeyRef.current === queryKey) return;
        loadingKeyRef.current = queryKey;

        try {
            const result = await fetchLibraryPage(client, userId, {
                parentId: viewId,
                startIndex: pager.nextStartIndex(),
                limit: pager.pageSize,
                ...buildLibraryQuery(selection)
            });
            pager.applyPage(result);
            setPageState({ queryKey, items: [...pager.items], total: pager.total });
        } catch {
            return;
        } finally {
            if (loadingKeyRef.current === queryKey) loadingKeyRef.current = null;
        }
    }, [client, queryKey, selection, userId, viewId]);

    useEffect(() => {
        if (!client || !userId || !viewId) return;
        const pager = createPager({ pageSize: PAGE_SIZE });
        pagerRef.current = pager;
        void loadPage(pager);
    }, [client, loadPage, userId, viewId]);

    useEffect(() => {
        if (!client || !userId || !viewId) return;
        fetchFilterOptions(client, userId, viewId)
            .then(({ genres: names, years }) => {
                setGenres(names);
                setDecadeOptions(decadesFromYears(years));
            })
            .catch(() => {
                setGenres([]);
                setDecadeOptions([]);
            });
        fetchItem(client, userId, viewId)
            .then((view) => setViewName(view.Name))
            .catch(() => setViewName(''));
    }, [client, userId, viewId]);

    return (
        <View style={styles.screen}>
            <ScreenBackground />
            <View style={{ paddingTop: insets.top + 8 }}>
                <View style={styles.titleRow}>
                    <Text style={styles.screenTitle}>{viewName}</Text>
                    {total ? (
                        <Text style={styles.countInline}>{total.toLocaleString()}</Text>
                    ) : null}
                </View>
                <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    contentContainerStyle={styles.pillRow}
                >
                    {genre || decadeKey || ratingKey || statusKey ? (
                        <Pressable
                            style={styles.clearPill}
                            onPress={() => {
                                updateFilters({
                                    genre: null,
                                    decadeKey: null,
                                    ratingKey: null,
                                    statusKey: null
                                });
                            }}
                        >
                            <Ionicons name="close" size={16} color={colors.text} />
                        </Pressable>
                    ) : null}
                    <DropdownPill
                        title="Sort"
                        options={SORT_OPTIONS}
                        selected={sortKey}
                        onSelect={(key) => updateFilters({ sortKey: key ?? SORT_OPTIONS[0].key })}
                    />
                    <DropdownPill
                        title="Genre"
                        clearLabel="All genres"
                        options={genres.map((name) => ({ key: name, label: name }))}
                        selected={genre}
                        onSelect={(key) => updateFilters({ genre: key })}
                    />
                    <DropdownPill
                        title="Decade"
                        clearLabel="Any decade"
                        options={decadeOptions}
                        selected={decadeKey}
                        onSelect={(key) => updateFilters({ decadeKey: key })}
                    />
                    <DropdownPill
                        title="Rating"
                        clearLabel="Any rating"
                        options={RATING_OPTIONS}
                        selected={ratingKey}
                        onSelect={(key) => updateFilters({ ratingKey: key })}
                    />
                    <DropdownPill
                        title="Watched"
                        clearLabel="All"
                        options={STATUS_OPTIONS}
                        selected={statusKey}
                        onSelect={(key) => updateFilters({ statusKey: key })}
                    />
                </ScrollView>
            </View>
            <FlatList
                showsVerticalScrollIndicator={false}
                data={items}
                numColumns={COLUMNS}
                keyExtractor={(item) => item.Id}
                columnWrapperStyle={styles.row}
                contentContainerStyle={{
                    paddingHorizontal: spacing.screen,
                    paddingBottom: BOTTOM_CLEARANCE
                }}
                onEndReachedThreshold={0.5}
                onEndReached={() => {
                    const pager = pagerRef.current;
                    if (pager && pager.shouldLoadMore(items.length - 1)) loadPage();
                }}
                renderItem={({ item }) => (
                    <GridPosterCard
                        item={item}
                        serverUrl={serverUrl}
                        width={cardWidth}
                    />
                )}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: colors.bg
    },
    titleRow: {
        flexDirection: 'row',
        alignItems: 'baseline',
        gap: 10,
        marginBottom: 14,
        marginLeft: spacing.screen
    },
    screenTitle: {
        color: colors.text,
        fontSize: 28,
        fontWeight: '700'
    },
    countInline: {
        color: colors.textDim,
        fontSize: 14
    },
    pillRow: {
        flexDirection: 'row',
        gap: 8,
        paddingLeft: spacing.screen,
        paddingRight: spacing.screen + 8,
        paddingBottom: 12
    },
    clearPill: {
        width: 34,
        height: 34,
        borderRadius: 17,
        borderWidth: 1,
        borderColor: colors.pillBorder,
        alignItems: 'center',
        justifyContent: 'center',
        alignSelf: 'center'
    },
    row: {
        gap: spacing.card,
        marginBottom: spacing.card
    }
});
