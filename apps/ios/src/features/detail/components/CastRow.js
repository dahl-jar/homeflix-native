import { StyleSheet, Text, View } from 'react-native';

import { BackdropImage } from '../../../components/BackdropImage/BackdropImage.js';
import { colors } from '../../../theme/tokens.js';

import { HorizontalSection } from './HorizontalSection.js';

const CAST_LIMIT = 12;

function personImageUrl(serverUrl, person) {
    if (!person.PrimaryImageTag) return null;
    return `${serverUrl}/Items/${person.Id}/Images/Primary?tag=${person.PrimaryImageTag}&maxWidth=200&quality=90`;
}

export function CastRow({ people, serverUrl }) {
    const cast = (people ?? []).filter((person) => person.Type === 'Actor').slice(0, CAST_LIMIT);
    if (cast.length === 0) return null;

    return (
        <HorizontalSection title="Cast">
            {cast.map((person) => (
                <View key={person.Id} style={styles.card}>
                    <BackdropImage
                        uri={personImageUrl(serverUrl, person)}
                        style={styles.image}
                    />
                    <Text numberOfLines={1} style={styles.name}>
                        {person.Name}
                    </Text>
                </View>
            ))}
        </HorizontalSection>
    );
}

const styles = StyleSheet.create({
    card: {
        width: 84,
        alignItems: 'center'
    },
    image: {
        width: 84,
        height: 84,
        borderRadius: 42
    },
    name: {
        color: colors.textDim,
        fontSize: 11,
        marginTop: 6
    }
});
