import { useEffect, useState } from 'react';
import { ScrollView, Text, View, Pressable, StyleSheet, useWindowDimensions } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { LinearGradient } from 'expo-linear-gradient';

import { useSession } from '../../src/session/SessionProvider.js';
import { fetchItem, fetchSeasons, fetchEpisodes } from '../../src/api/items.js';
import { backdropUrl, primaryUrl } from '../../src/api/imageUrl.js';
import { chips, starText, runtimeText } from '../../src/features/detail/format.js';
import { BackdropImage } from '../../src/components/BackdropImage.js';
import { GlassChip } from '../../src/components/GlassChip.js';
import { PlayPill } from '../../src/components/PlayPill.js';
import { GhostTile } from '../../src/components/GhostTile.js';
import { playerLauncher } from '../../src/playback/playerLauncher.js';
import { colors, radius, spacing } from '../../src/theme/tokens.js';

const BACKDROP_RATIO = 16 / 11;
const BOTTOM_CLEARANCE = 60;

export default function DetailScreen() {
    const session = useSession();
    const router = useRouter();
    const { width } = useWindowDimensions();
    const { id } = useLocalSearchParams();
    const [item, setItem] = useState(null);
    const [seasons, setSeasons] = useState([]);
    const [seasonIndex, setSeasonIndex] = useState(0);
    const [episodes, setEpisodes] = useState([]);

    useEffect(() => {
        if (!session.client || !session.userId || !id) return;
        fetchItem(session.client, session.userId, id).then(setItem);
    }, [session.client, session.userId, id]);

    useEffect(() => {
        if (!item || item.Type !== 'Series') return;
        fetchSeasons(session.client, session.userId, item.Id).then((result) => {
            setSeasons(result.Items);
            setSeasonIndex(0);
        });
    }, [item]);

    useEffect(() => {
        if (!item || seasons.length === 0) return;
        const season = seasons[seasonIndex];
        fetchEpisodes(session.client, session.userId, item.Id, season.Id).then((result) =>
            setEpisodes(result.Items)
        );
    }, [seasons, seasonIndex]);

    if (!item) return <View style={styles.screen} />;

    const cast = (item.People ?? []).filter((person) => person.Type === 'Actor').slice(0, 12);
    const star = starText(item);

    return (
        <ScrollView style={styles.screen} contentContainerStyle={{ paddingBottom: BOTTOM_CLEARANCE }}>
            <View>
                <BackdropImage
                    uri={backdropUrl(session.serverUrl, item, 1280)}
                    style={{ width, height: width / BACKDROP_RATIO }}
                />
                <LinearGradient
                    colors={['rgba(21, 19, 19, 0.45)', 'transparent', 'rgba(21, 19, 19, 0.6)', colors.bg]}
                    locations={[0, 0.3, 0.75, 1]}
                    style={StyleSheet.absoluteFill}
                />
                <Pressable style={styles.back} onPress={() => router.back()}>
                    <Text style={styles.backGlyph}>‹</Text>
                </Pressable>
                <Text numberOfLines={2} style={styles.title}>
                    {item.Name}
                </Text>
            </View>

            <View style={styles.body}>
                <View style={styles.chips}>
                    {chips(item).map((label) => (
                        <GlassChip key={label} label={label} />
                    ))}
                    {star ? <GlassChip label={star} accentStar /> : null}
                </View>

                <PlayPill
                    item={item}
                    origin="detail"
                    label={item.UserData?.PlaybackPositionTicks > 0 ? 'Resume' : 'Play'}
                />

                <View style={styles.tiles}>
                    <GhostTile glyph="▤" label="Trailer" onPress={() => playerLauncher.play(item, 'trailer')} />
                    <GhostTile glyph="✓" label={item.UserData?.Played ? 'Played' : 'Mark Played'} onPress={() => {}} />
                    <GhostTile glyph="↺" label="Restart" onPress={() => playerLauncher.play(item, 'restart')} />
                </View>

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

                {seasons.length > 0 ? (
                    <View style={styles.seasonBlock}>
                        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                            <View style={styles.seasonRow}>
                                {seasons.map((season, i) => (
                                    <Pressable
                                        key={season.Id}
                                        style={[styles.seasonTab, i === seasonIndex && styles.seasonTabActive]}
                                        onPress={() => setSeasonIndex(i)}
                                    >
                                        <Text
                                            style={[
                                                styles.seasonText,
                                                i === seasonIndex && styles.seasonTextActive
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
                                    uri={primaryUrl(session.serverUrl, episode, 320)}
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
                ) : null}

                {cast.length > 0 ? (
                    <View style={styles.castBlock}>
                        <Text style={styles.sectionTitle}>Cast</Text>
                        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                            <View style={styles.castRow}>
                                {cast.map((person) => (
                                    <View key={person.Id} style={styles.castCard}>
                                        <BackdropImage
                                            uri={
                                                person.PrimaryImageTag
                                                    ? `${session.serverUrl}/Items/${person.Id}/Images/Primary?tag=${person.PrimaryImageTag}&maxWidth=200&quality=90`
                                                    : null
                                            }
                                            style={styles.castImage}
                                        />
                                        <Text numberOfLines={1} style={styles.castName}>
                                            {person.Name}
                                        </Text>
                                    </View>
                                ))}
                            </View>
                        </ScrollView>
                    </View>
                ) : null}
            </View>
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: colors.bg
    },
    back: {
        position: 'absolute',
        top: 58,
        left: 14,
        width: 38,
        height: 38,
        borderRadius: 19,
        backgroundColor: colors.glassBg,
        borderWidth: 1,
        borderColor: colors.glassBorder,
        alignItems: 'center',
        justifyContent: 'center'
    },
    backGlyph: {
        color: colors.text,
        fontSize: 24,
        lineHeight: 26
    },
    title: {
        position: 'absolute',
        left: spacing.screen,
        right: spacing.screen,
        bottom: 10,
        color: colors.text,
        fontSize: 28,
        fontWeight: '700'
    },
    body: {
        paddingHorizontal: spacing.screen
    },
    chips: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 8,
        marginTop: 12,
        marginBottom: 16
    },
    tiles: {
        flexDirection: 'row',
        justifyContent: 'flex-start',
        gap: 18,
        marginTop: 10,
        marginBottom: 6
    },
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
    },
    seasonBlock: {
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
    },
    castBlock: {
        marginTop: 24
    },
    sectionTitle: {
        color: colors.text,
        fontSize: 18,
        fontWeight: '700',
        marginBottom: 10
    },
    castRow: {
        flexDirection: 'row',
        gap: 12
    },
    castCard: {
        width: 84,
        alignItems: 'center'
    },
    castImage: {
        width: 84,
        height: 84,
        borderRadius: 42
    },
    castName: {
        color: colors.textDim,
        fontSize: 11,
        marginTop: 6
    }
});
