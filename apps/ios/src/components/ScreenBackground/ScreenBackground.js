import { LinearGradient } from 'expo-linear-gradient';
import { StyleSheet } from 'react-native';

import { colors } from '../../theme/tokens.js';

export function ScreenBackground() {
    return (
        <LinearGradient
            colors={['#301f20', '#201818', colors.bg]}
            locations={[0, 0.35, 0.7]}
            style={StyleSheet.absoluteFill}
            pointerEvents="none"
        />
    );
}
