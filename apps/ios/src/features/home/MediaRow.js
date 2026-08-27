import { useRouter } from 'expo-router';
import { FlatList, Text, View, StyleSheet } from 'react-native';

import { primaryUrl } from '../../api/imageUrl.js';
import { PosterCard } from '../../components/PosterCard.js';
import { ProgressCard } from '../../components/ProgressCard.js';
import { colors, spacing } from '../../theme/tokens.js';

const POSTER_WIDTH = 110;
const PROGRESS_WIDTH = 220;

export function MediaRow({ title, items, baseUrl, variant = 'poster' }) {
    const router = useRouter();
    if (items.length === 0) return null;
    const wide = variant === 'progress';
    const width = wide ? PROGRESS_WIDTH : POSTER_WIDTH;

    return (
        <View style={styles.section}>
            <Text style={styles.title}>{title}</Text>
            <FlatList
                horizontal
                showsHorizontalScrollIndicator={false}
                data={items}
                keyExtractor={(item) => item.Id}
                contentContainerStyle={styles.list}
                renderItem={({ item }) => {
                    const target =
                        item.Type === 'Episode' && item.SeriesId
                            ? `/details/${item.SeriesId}`
                            : `/details/${item.Id}`;
                    const onPress = () => router.push(target);
                    return wide ? (
                        <ProgressCard
                            item={item}
                            imageUri={primaryUrl(baseUrl, item, 440)}
                            width={width}
                            onPress={onPress}
                        />
                    ) : (
                        <PosterCard
                            item={item}
                            imageUri={primaryUrl(baseUrl, item, 220)}
                            width={width}
                            onPress={onPress}
                        />
                    );
                }}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    section: {
        marginTop: spacing.row
    },
    title: {
        color: colors.text,
        fontSize: 18,
        fontWeight: '700',
        marginBottom: 10,
        marginLeft: spacing.screen
    },
    list: {
        paddingHorizontal: spacing.screen,
        gap: spacing.card
    }
});
