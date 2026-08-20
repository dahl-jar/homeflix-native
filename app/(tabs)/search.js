import { useMemo, useState } from 'react';
import { FlatList, TextInput, View, StyleSheet, useWindowDimensions } from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useSession } from '../../src/session/SessionProvider.js';
import { searchItems } from '../../src/api/items.js';
import { createSearchController } from '../../src/features/search/debounce.js';
import { PosterCard } from '../../src/components/PosterCard.js';
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

    const cardWidth = (width - spacing.screen * 2 - spacing.card * (COLUMNS - 1)) / COLUMNS;

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
            <TextInput
                style={styles.input}
                placeholder="Films, series, anime"
                placeholderTextColor={colors.textDim}
                autoCorrect={false}
                autoCapitalize="none"
                onChangeText={(text) => {
                    if (text.trim() === '') setResults([]);
                    controller?.onQuery(text);
                }}
            />
            <FlatList
                data={results}
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
