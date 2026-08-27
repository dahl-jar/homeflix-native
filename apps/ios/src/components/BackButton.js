import { Pressable, StyleSheet, Text } from 'react-native';

import { colors } from '../theme/tokens.js';

export function BackButton({ onPress, style }) {
    return (
        <Pressable style={[styles.button, style]} onPress={onPress}>
            <Text style={styles.glyph}>‹</Text>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    button: {
        position: 'absolute',
        width: 38,
        height: 38,
        borderRadius: 19,
        backgroundColor: colors.glassBg,
        borderWidth: 1,
        borderColor: colors.glassBorder,
        alignItems: 'center',
        justifyContent: 'center'
    },
    glyph: {
        color: colors.text,
        fontSize: 24,
        lineHeight: 26
    }
});
