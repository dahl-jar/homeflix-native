import { Ionicons, MaterialIcons } from '@expo/vector-icons';
import { Pressable, StyleSheet } from 'react-native';

import { colors } from '../../theme/tokens.js';

export function PlayerIconButton({ accessibilityLabel, family = 'ion', icon, onPress, prominent = false, size }) {
    const Icon = family === 'material' ? MaterialIcons : Ionicons;
    const iconSize = size ?? (prominent ? 38 : 30);
    return (
        <Pressable
            accessibilityLabel={accessibilityLabel}
            accessibilityRole="button"
            hitSlop={10}
            onPress={onPress}
            style={({ pressed }) => [
                styles.button,
                prominent && styles.prominent,
                pressed && styles.pressed
            ]}
        >
            <Icon color={colors.text} name={icon} size={iconSize} />
        </Pressable>
    );
}

const styles = StyleSheet.create({
    button: {
        alignItems: 'center',
        justifyContent: 'center',
        width: 54,
        height: 54,
        borderRadius: 27
    },
    prominent: {
        width: 70,
        height: 70,
        borderRadius: 35,
        backgroundColor: 'rgba(0, 0, 0, 0.48)'
    },
    pressed: {
        opacity: 0.65,
        transform: [{ scale: 0.96 }]
    }
});
