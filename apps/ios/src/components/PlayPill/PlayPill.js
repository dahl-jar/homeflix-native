import { Pressable, Text, StyleSheet } from 'react-native';

import { playerLauncher } from '../../playback/player/playerLauncher.js';
import { radius } from '../../theme/tokens.js';

export function PlayPill({ item, origin, label = 'Play', mediaSourceId = null }) {
    return (
        <Pressable
            style={styles.pill}
            onPress={() => playerLauncher.play(item, origin, { mediaSourceId })}
        >
            <Text style={styles.text}>▶ {label}</Text>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    pill: {
        backgroundColor: '#ffffff',
        borderRadius: radius.button,
        paddingVertical: 13,
        alignItems: 'center'
    },
    text: {
        color: '#141414',
        fontSize: 16,
        fontWeight: '600'
    }
});
