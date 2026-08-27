import { View, StyleSheet, useWindowDimensions } from 'react-native';

import { colors, radius, spacing } from '../../theme/tokens.js';

const BILLBOARD_RATIO = 16 / 10;
const CARD_COUNT = 3;

/** Placeholder blocks shown while the first home load is in flight. */
export function HomeSkeleton() {
    const { width } = useWindowDimensions();

    return (
        <View>
            <View style={[styles.block, { width, height: width / BILLBOARD_RATIO, borderRadius: 0 }]} />
            <View style={styles.rowLabel} />
            <View style={styles.row}>
                {Array.from({ length: CARD_COUNT }, (_, i) => (
                    <View key={i} style={styles.card} />
                ))}
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    block: {
        backgroundColor: colors.bgRaised
    },
    rowLabel: {
        width: 160,
        height: 16,
        borderRadius: 4,
        backgroundColor: colors.bgRaised,
        marginTop: spacing.row,
        marginLeft: spacing.screen
    },
    row: {
        flexDirection: 'row',
        gap: spacing.card,
        paddingHorizontal: spacing.screen,
        marginTop: 12
    },
    card: {
        width: 220,
        height: 124,
        borderRadius: radius.card,
        backgroundColor: colors.bgRaised
    }
});
