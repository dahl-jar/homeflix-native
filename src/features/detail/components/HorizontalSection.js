import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { colors } from '../../../theme/tokens.js';

export function HorizontalSection({ children, title }) {
    return (
        <View style={styles.section}>
            <Text style={styles.title}>{title}</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                <View style={styles.row}>{children}</View>
            </ScrollView>
        </View>
    );
}

const styles = StyleSheet.create({
    section: {
        marginTop: 24
    },
    title: {
        color: colors.text,
        fontSize: 18,
        fontWeight: '700',
        marginBottom: 10
    },
    row: {
        flexDirection: 'row',
        gap: 12
    }
});
