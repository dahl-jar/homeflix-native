import { StyleSheet } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';

import { colors } from '../theme/tokens.js';

/** Ambient screen backdrop: a warm dark tint bleeding from the top into the base background. */
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
