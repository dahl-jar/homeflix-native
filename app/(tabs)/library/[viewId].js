import { useEffect, useRef, useState } from 'react';
import { FlatList, ScrollView, Pressable, Text, View, StyleSheet, useWindowDimensions } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useSession } from '../../../src/session/SessionProvider.js';
import { fetchLibraryPage, fetchGenres } from '../../../src/api/items.js';
import { createPager } from '../../../src/features/library/paging.js';
import { SORT_OPTIONS, buildLibraryQuery } from '../../../src/features/library/filters.js';
import { PosterCard } from '../../../src/components/PosterCard.js';
import { primaryUrl } from '../../../src/api/imageUrl.js';
import { colors, radius, spacing } from '../../../src/theme/tokens.js';

const COLUMNS = 3;
const BOTTOM_CLEARANCE = 110;

function Chip({ label, active, onPress }) {
    return (
        <Pressable style={[styles.chip, active && styles.chipActive]} onPress={onPress}>
            <Text style={[styles.chipText, active && styles.chipTextActive]}>{label}</Text>
        </Pressable>
    );
}

export default function LibraryScreen() {
    const session = useSession();
    const router = useRouter();
    const insets = useSafeAreaInsets();
    const { width } = useWindowDimensions();
    const { viewId } = useLocalSearchParams();
    const pagerRef = useRef(null);
    const loadingRef = useRef(false);
    const [items, setItems] = useState([]);
    const [genres, setGenres] = useState([]);
    const [sortKey, setSortKey] = useState(SORT_OPTIONS[0].key);
    const [genreId, setGenreId] = useState(null);

    const sort = SORT_OPTIONS.find((option) => option.key === sortKey);
    const cardWidth = (width - spacing.screen * 2 - spacing.card * (COLUMNS - 1)) / COLUMNS;

    const loadPage = async () => {
        if (loadingRef.current) return;
        loadingRef.current = true;
        const pager = pagerRef.current;
        const result = await fetchLibraryPage(session.client, session.userId, {
            parentId: viewId,
            startIndex: pager.nextStartIndex(),
            limit: pager.pageSize,
            ...buildLibraryQuery({ sort, genreId })
        });
        pager.applyPage(result);
        setItems([...pager.items]);
        loadingRef.current = false;
    };

    useEffect(() => {
        if (!session.client || !session.userId || !viewId) return;
        pagerRef.current = createPager({ pageSize: 100 });
        setItems([]);
        loadPage();
    }, [session.client, session.userId, viewId, sortKey, genreId]);

    useEffect(() => {
        if (!session.client || !session.userId || !viewId) return;
        fetchGenres(session.client, session.userId, viewId)
            .then((result) => setGenres(result.Items))
            .catch(() => setGenres([]));
        setGenreId(null);
    }, [session.client, session.userId, viewId]);

    return (
        <View style={styles.screen}>
            <FlatList
                data={items}
                numColumns={COLUMNS}
                keyExtractor={(item) => item.Id}
                columnWrapperStyle={styles.row}
                contentContainerStyle={{
                    paddingTop: insets.top + 8,
                    paddingHorizontal: spacing.screen,
                    paddingBottom: BOTTOM_CLEARANCE
                }}
                ListHeaderComponent={
                    <View style={styles.header}>
                        <View style={styles.sortRow}>
                            {SORT_OPTIONS.map((option) => (
                                <Chip
                                    key={option.key}
                                    label={option.label}
                                    active={option.key === sortKey}
                                    onPress={() => setSortKey(option.key)}
                                />
                            ))}
                        </View>
                        {genres.length > 0 ? (
                            <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                                <View style={styles.genreRow}>
                                    <Chip
                                        label="All"
                                        active={genreId === null}
                                        onPress={() => setGenreId(null)}
                                    />
                                    {genres.map((genre) => (
                                        <Chip
                                            key={genre.Id}
                                            label={genre.Name}
                                            active={genre.Id === genreId}
                                            onPress={() => setGenreId(genre.Id)}
                                        />
                                    ))}
                                </View>
                            </ScrollView>
                        ) : null}
                        {pagerRef.current ? (
                            <Text style={styles.count}>{pagerRef.current.total} titles</Text>
                        ) : null}
                    </View>
                }
                onEndReachedThreshold={0.5}
                onEndReached={() => {
                    const pager = pagerRef.current;
                    if (pager && pager.shouldLoadMore(items.length - 1)) loadPage();
                }}
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
        backgroundColor: colors.bg
    },
    header: {
        marginBottom: 12
    },
    sortRow: {
        flexDirection: 'row',
        gap: 8,
        marginBottom: 10
    },
    genreRow: {
        flexDirection: 'row',
        gap: 8,
        paddingRight: spacing.screen
    },
    chip: {
        backgroundColor: colors.bgRaised,
        borderRadius: radius.pill,
        paddingHorizontal: 14,
        paddingVertical: 7
    },
    chipActive: {
        backgroundColor: '#ffffff'
    },
    chipText: {
        color: colors.textDim,
        fontSize: 13,
        fontWeight: '500'
    },
    chipTextActive: {
        color: '#141414'
    },
    row: {
        gap: spacing.card,
        marginBottom: 14
    },
    count: {
        color: colors.textDim,
        fontSize: 13,
        marginTop: 12
    }
});
