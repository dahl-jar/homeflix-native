import { Pressable, StyleSheet, Text } from 'react-native';

import { colors, radius } from '../../theme/tokens.js';

const LABELS = {
    Intro: 'Skip intro',
    Recap: 'Skip recap',
    Outro: 'Skip credits'
};

export function SkipSegmentButton({ segment, onPress }) {
    if (!segment) return null;
    return (
        <Pressable style={styles.button} onPress={onPress}>
            <Text style={styles.text}>{LABELS[segment.type]}</Text>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    button: {
        position: 'absolute',
        right: 20,
        bottom: 56,
        borderRadius: radius.button,
        backgroundColor: colors.glassBg,
        borderColor: colors.glassBorder,
        borderWidth: 1,
        paddingHorizontal: 18,
        paddingVertical: 11
    },
    text: {
        color: colors.text,
        fontSize: 15,
        fontWeight: '600'
    }
});
