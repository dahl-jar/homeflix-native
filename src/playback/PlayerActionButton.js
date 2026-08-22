import { Ionicons, MaterialIcons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text } from 'react-native';

import { colors } from '../theme/tokens.js';

export function PlayerActionButton({ family = 'ion', icon, label, onPress }) {
    const Icon = family === 'material' ? MaterialIcons : Ionicons;
    return (
        <Pressable
            accessibilityLabel={label}
            accessibilityRole="button"
            onPress={onPress}
            style={({ pressed }) => [styles.button, pressed && styles.pressed]}
        >
            <Icon color={colors.text} name={icon} size={24} />
            <Text numberOfLines={2} style={styles.label}>{label}</Text>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    button: {
        width: 104,
        minHeight: 66,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 8,
        paddingVertical: 8
    },
    label: {
        color: colors.text,
        fontSize: 11,
        marginTop: 5,
        textAlign: 'center'
    },
    pressed: {
        opacity: 0.6
    }
});
