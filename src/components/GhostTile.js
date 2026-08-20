import { Pressable, Text, StyleSheet } from 'react-native';

import { colors } from '../theme/tokens.js';

/** Quiet secondary action: icon glyph above a small label. */
export function GhostTile({ glyph, label, onPress }) {
    return (
        <Pressable style={styles.tile} onPress={onPress}>
            <Text style={styles.glyph}>{glyph}</Text>
            <Text style={styles.label}>{label}</Text>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    tile: {
        alignItems: 'center',
        width: 76,
        paddingVertical: 8
    },
    glyph: {
        color: colors.textDim,
        fontSize: 22
    },
    label: {
        color: colors.textDim,
        fontSize: 11,
        marginTop: 4,
        textAlign: 'center'
    }
});
