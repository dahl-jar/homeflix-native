import { StyleSheet, View } from 'react-native';

import { GlassChip } from '../../../components/GlassChip/GlassChip.js';
import { chips, starText } from '../format.js';

export function DetailChips({ item }) {
    const star = starText(item);

    return (
        <View style={styles.chips}>
            {chips(item).map((label) => (
                <GlassChip key={label} label={label} />
            ))}
            {star ? <GlassChip label={star} accentStar /> : null}
        </View>
    );
}

const styles = StyleSheet.create({
    chips: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 8,
        marginTop: 12,
        marginBottom: 16
    }
});
