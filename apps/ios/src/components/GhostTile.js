import { Ionicons } from '@expo/vector-icons';
import { Pressable, Text, StyleSheet } from 'react-native';

import { colors } from '../theme/tokens.js';

/** Quiet secondary action: one consistent icon above a small label. */
export function GhostTile({ icon, label, onPress }) {
    return (
        <Pressable style={styles.tile} onPress={onPress}>
            <Ionicons name={icon} size={22} color={colors.textDim} />
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
    label: {
        color: colors.textDim,
        fontSize: 11,
        marginTop: 4,
        textAlign: 'center'
    }
});
