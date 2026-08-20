import { Text, View, StyleSheet } from 'react-native';

import { colors, radius } from '../theme/tokens.js';

export function GlassChip({ label, accentStar = false }) {
    return (
        <View style={styles.chip}>
            <Text style={styles.text}>
                {accentStar ? '★ ' : ''}
                {label}
            </Text>
        </View>
    );
}

const styles = StyleSheet.create({
    chip: {
        backgroundColor: colors.chipBg,
        borderColor: colors.chipBorder,
        borderWidth: 1,
        borderRadius: radius.pill,
        paddingHorizontal: 12,
        paddingVertical: 4
    },
    text: {
        color: colors.text,
        fontSize: 13
    }
});
