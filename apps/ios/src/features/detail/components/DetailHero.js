import { LinearGradient } from 'expo-linear-gradient';
import { StyleSheet, Text, View } from 'react-native';

import { backdropUrl } from '../../../api/images/imageUrl.js';
import { BackButton } from '../../../components/BackButton/BackButton.js';
import { BackdropImage } from '../../../components/BackdropImage/BackdropImage.js';
import { colors, spacing } from '../../../theme/tokens.js';

const BACKDROP_RATIO = 16 / 11;

export function DetailHero({ item, onBack, serverUrl, width }) {
    return (
        <View>
            <BackdropImage
                uri={backdropUrl(serverUrl, item, 1280)}
                style={{ width, height: width / BACKDROP_RATIO }}
            />
            <LinearGradient
                colors={['rgba(21, 19, 19, 0.45)', 'transparent', 'rgba(21, 19, 19, 0.6)', colors.bg]}
                locations={[0, 0.3, 0.75, 1]}
                style={StyleSheet.absoluteFill}
            />
            <BackButton style={styles.back} onPress={onBack} />
            <Text numberOfLines={2} style={styles.title}>
                {item.Name}
            </Text>
        </View>
    );
}

const styles = StyleSheet.create({
    back: {
        top: 58,
        left: 14
    },
    title: {
        position: 'absolute',
        left: spacing.screen,
        right: spacing.screen,
        bottom: 10,
        color: colors.text,
        fontSize: 28,
        fontWeight: '700'
    }
});
