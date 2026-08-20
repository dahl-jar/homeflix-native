import { useEffect, useRef, useState } from 'react';
import { FlatList, ScrollView, Text, View, StyleSheet, useWindowDimensions } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useSession } from '../../../src/session/SessionProvider.js';
import { fetchLibraryPage, fetchGenres, fetchItem } from '../../../src/api/items.js';
import { createPager } from '../../../src/features/library/paging.js';
import {
    SORT_OPTIONS,
    DECADE_OPTIONS,
    RATING_OPTIONS,
    STATUS_OPTIONS,
    buildLibraryQuery
} from '../../../src/features/library/filters.js';
import { DropdownPill } from '../../../src/components/DropdownPill.js';
import { PosterCard } from '../../../src/components/PosterCard.js';
import { ScreenBackground } from '../../../src/components/ScreenBackground.js';
import { primaryUrl } from '../../../src/api/imageUrl.js';
import { colors, spacing } from '../../../src/theme/tokens.js';

const COLUMNS = 3;
const BOTTOM_CLEARANCE = 110;

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
    const [viewName, setViewName] = useState('');
    const [sortKey, setSortKey] = useState(SORT_OPTIONS[0].key);
    const [genreId, setGenreId] = useState(null);
    const [decadeKey, setDecadeKey] = useState(null);
    const [ratingKey, setRatingKey] = useState(null);
    const [statusKey, setStatusKey] = useState(null);

    const selection = {
        sort: SORT_OPTIONS.find((option) => option.key === sortKey),
        genreId,
        decade: DECADE_OPTIONS.find((option) => option.key === decadeKey) ?? null,
        rating: RATING_OPTIONS.find((option) => option.key === ratingKey) ?? null,
        status: STATUS_OPTIONS.find((option) => option.key === statusKey) ?? null
    };
    const cardWidth = (width - spacing.screen * 2 - spacing.card * (COLUMNS - 1)) / COLUMNS;

    const loadPage = async () => {
        if (loadingRef.current) return;
        loadingRef.current = true;
        const pager = pagerRef.current;
        const result = await fetchLibraryPage(session.client, session.userId, {
            parentId: viewId,
            startIndex: pager.nextStartIndex(),
            limit: pager.pageSize,
            ...buildLibraryQuery(selection)
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
    }, [session.client, session.userId, viewId, sortKey, genreId, decadeKey, ratingKey, statusKey]);

    useEffect(() => {
        if (!session.client || !session.userId || !viewId) return;
        fetchGenres(session.client, session.userId, viewId)
            .then((result) => setGenres(result.Items))
            .catch(() => setGenres([]));
        fetchItem(session.client, session.userId, viewId)
            .then((view) => setViewName(view.Name))
            .catch(() => setViewName(''));
        setGenreId(null);
        setDecadeKey(null);
        setRatingKey(null);
        setStatusKey(null);
    }, [session.client, session.userId, viewId]);

    return (
        <View style={styles.screen}>
            <ScreenBackground />
            <View style={{ paddingTop: insets.top + 8 }}>
                <View style={styles.titleRow}>
                    <Text style={styles.screenTitle}>{viewName}</Text>
                    {pagerRef.current?.total ? (
                        <Text style={styles.countInline}>{pagerRef.current.total.toLocaleString()}</Text>
                    ) : null}
                </View>
                <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    contentContainerStyle={styles.pillRow}
                >
                    <DropdownPill
                        title="Sort"
                        options={SORT_OPTIONS}
                        selected={sortKey}
                        onSelect={(key) => setSortKey(key ?? SORT_OPTIONS[0].key)}
                    />
                    <DropdownPill
                        title="Genre"
                        clearLabel="All genres"
                        options={genres.map((genre) => ({ key: genre.Id, label: genre.Name }))}
                        selected={genreId}
                        onSelect={setGenreId}
                    />
                    <DropdownPill
                        title="Decade"
                        clearLabel="Any decade"
                        options={DECADE_OPTIONS}
                        selected={decadeKey}
                        onSelect={setDecadeKey}
                    />
                    <DropdownPill
                        title="Rating"
                        clearLabel="Any rating"
                        options={RATING_OPTIONS}
                        selected={ratingKey}
                        onSelect={setRatingKey}
                    />
                    <DropdownPill
                        title="Watched"
                        clearLabel="All"
                        options={STATUS_OPTIONS}
                        selected={statusKey}
                        onSelect={setStatusKey}
                    />
                </ScrollView>
            </View>
            <FlatList
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
                    <PosterCard
                        item={item}
                        imageUri={primaryUrl(session.serverUrl, item, 300)}
                        width={cardWidth}
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
        paddingHorizontal: spacing.screen,
        paddingBottom: 12
    },
    row: {
        gap: spacing.card,
        marginBottom: spacing.card
    }
});
