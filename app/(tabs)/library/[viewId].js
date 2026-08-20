import { useEffect, useRef, useState } from 'react';
import { FlatList, ScrollView, Pressable, Text, View, StyleSheet, useWindowDimensions } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useSession } from '../../../src/session/SessionProvider.js';
import { fetchLibraryPage, fetchFilterOptions, fetchItem } from '../../../src/api/items.js';
import { createPager } from '../../../src/features/library/paging.js';
import {
    SORT_OPTIONS,
    RATING_OPTIONS,
    STATUS_OPTIONS,
    buildLibraryQuery,
    decadesFromYears
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
    const [decadeOptions, setDecadeOptions] = useState([]);
    const [viewName, setViewName] = useState('');
    const [sortKey, setSortKey] = useState(SORT_OPTIONS[0].key);
    const [genre, setGenre] = useState(null);
    const [decadeKey, setDecadeKey] = useState(null);
    const [ratingKey, setRatingKey] = useState(null);
    const [statusKey, setStatusKey] = useState(null);

    const selection = {
        sort: SORT_OPTIONS.find((option) => option.key === sortKey),
        genre,
        decade: decadeOptions.find((option) => option.key === decadeKey) ?? null,
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
    }, [session.client, session.userId, viewId, sortKey, genre, decadeKey, ratingKey, statusKey]);

    useEffect(() => {
        if (!session.client || !session.userId || !viewId) return;
        fetchFilterOptions(session.client, session.userId, viewId)
            .then(({ genres: names, years }) => {
                setGenres(names);
                setDecadeOptions(decadesFromYears(years));
            })
            .catch(() => {
                setGenres([]);
                setDecadeOptions([]);
            });
        fetchItem(session.client, session.userId, viewId)
            .then((view) => setViewName(view.Name))
            .catch(() => setViewName(''));
        setGenre(null);
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
                    {genre || decadeKey || ratingKey || statusKey ? (
                        <Pressable
                            style={styles.clearPill}
                            onPress={() => {
                                setGenre(null);
                                setDecadeKey(null);
                                setRatingKey(null);
                                setStatusKey(null);
                            }}
                        >
                            <Ionicons name="close" size={16} color={colors.text} />
                        </Pressable>
                    ) : null}
                    <DropdownPill
                        title="Sort"
                        options={SORT_OPTIONS}
                        selected={sortKey}
                        onSelect={(key) => setSortKey(key ?? SORT_OPTIONS[0].key)}
                    />
                    <DropdownPill
                        title="Genre"
                        clearLabel="All genres"
                        options={genres.map((name) => ({ key: name, label: name }))}
                        selected={genre}
                        onSelect={setGenre}
                    />
                    <DropdownPill
                        title="Decade"
                        clearLabel="Any decade"
                        options={decadeOptions}
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
