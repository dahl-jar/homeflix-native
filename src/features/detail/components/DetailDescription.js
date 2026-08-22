import { StyleSheet, Text, View } from 'react-native';

import { colors, radius } from '../../../theme/tokens.js';

export function DetailDescription({ item }) {
    return (
        <>
            {item.Overview ? <Text style={styles.overview}>{item.Overview}</Text> : null}
            {item.Genres?.length ? (
                <View style={styles.genreWrap}>
                    <Text style={styles.sectionLabel}>Genres</Text>
                    <View style={styles.genres}>
                        {item.Genres.map((genre) => (
                            <View key={genre} style={styles.genrePill}>
                                <Text style={styles.genreText}>{genre}</Text>
                            </View>
                        ))}
                    </View>
                </View>
            ) : null}
        </>
    );
}

const styles = StyleSheet.create({
    overview: {
        color: '#d6d2d1',
        fontSize: 15,
        lineHeight: 23,
        marginTop: 12
    },
    sectionLabel: {
        color: colors.textDim,
        fontSize: 13,
        marginBottom: 8
    },
    genreWrap: {
        marginTop: 20
    },
    genres: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 8
    },
    genrePill: {
        borderColor: colors.pillBorder,
        borderWidth: 1,
        borderRadius: radius.pill,
        paddingHorizontal: 14,
        paddingVertical: 6
    },
    genreText: {
        color: colors.text,
        fontSize: 13,
        fontWeight: '500'
    }
});
