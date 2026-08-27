import { Image } from 'expo-image';

import { colors } from '../theme/tokens.js';

const FADE_MS = 300;

export function BackdropImage({ uri, style, contentFit = 'cover' }) {
    return (
        <Image
            source={uri ? { uri } : null}
            style={[{ backgroundColor: colors.bgRaised }, style]}
            contentFit={contentFit}
            transition={FADE_MS}
        />
    );
}
