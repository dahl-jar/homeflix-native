import { StyleSheet, Text, View } from 'react-native';

import { colors } from '../../theme/tokens.js';

export function CardFallback({ label, numberOfLines, paddingHorizontal }) {
    return (
        <View style={[styles.container, { paddingHorizontal }]}>
            <Text numberOfLines={numberOfLines} style={styles.text}>
                {label}
            </Text>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        alignItems: 'center',
        justifyContent: 'center'
    },
    text: {
        color: colors.textDim,
        fontSize: 13,
        fontWeight: '600',
        textAlign: 'center'
    }
});
