import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { primaryUrl } from '../../../api/images/imageUrl.js';
import { BackdropImage } from '../../../components/BackdropImage/BackdropImage.js';
import { playerLauncher } from '../../../playback/player/playerLauncher.js';
import { colors, radius } from '../../../theme/tokens.js';
import { runtimeText } from '../format.js';

export function EpisodeSection({ episodes, onSelectSeason, seasonIndex, seasons, serverUrl }) {
    if (seasons.length === 0) return null;

    return (
        <View style={styles.section}>
            <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                <View style={styles.seasonRow}>
                    {seasons.map((season, index) => (
                        <Pressable
                            key={season.Id}
                            style={[styles.seasonTab, index === seasonIndex && styles.seasonTabActive]}
                            onPress={() => onSelectSeason(index)}
                        >
                            <Text
                                style={[
                                    styles.seasonText,
                                    index === seasonIndex && styles.seasonTextActive
                                ]}
                            >
                                {season.Name}
                            </Text>
                        </Pressable>
                    ))}
                </View>
            </ScrollView>
            {episodes.map((episode) => (
                <Pressable
                    key={episode.Id}
                    style={styles.episode}
                    onPress={() => playerLauncher.play(episode, 'episode-list')}
                >
                    <BackdropImage
                        uri={primaryUrl(serverUrl, episode, 320)}
                        style={styles.episodeThumb}
                    />
                    <View style={styles.episodeMeta}>
                        <Text numberOfLines={1} style={styles.episodeTitle}>
                            {episode.IndexNumber}. {episode.Name}
                        </Text>
                        {episode.RunTimeTicks ? (
                            <Text style={styles.episodeRuntime}>
                                {runtimeText(episode.RunTimeTicks)}
                            </Text>
                        ) : null}
                    </View>
                </Pressable>
            ))}
        </View>
    );
}

const styles = StyleSheet.create({
    section: {
        marginTop: 24
    },
    seasonRow: {
        flexDirection: 'row',
        gap: 8,
        marginBottom: 14
    },
    seasonTab: {
        borderRadius: radius.pill,
        paddingHorizontal: 14,
        paddingVertical: 7,
        backgroundColor: colors.bgRaised
    },
    seasonTabActive: {
        backgroundColor: '#ffffff'
    },
    seasonText: {
        color: colors.textDim,
        fontSize: 13,
        fontWeight: '500'
    },
    seasonTextActive: {
        color: '#141414'
    },
    episode: {
        flexDirection: 'row',
        gap: 12,
        marginBottom: 14,
        alignItems: 'center'
    },
    episodeThumb: {
        width: 130,
        height: 74,
        borderRadius: radius.card
    },
    episodeMeta: {
        flex: 1
    },
    episodeTitle: {
        color: colors.text,
        fontSize: 14,
        fontWeight: '500'
    },
    episodeRuntime: {
        color: colors.textDim,
        fontSize: 12,
        marginTop: 3
    }
});
