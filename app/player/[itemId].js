import { Redirect, useIsFocused, useLocalSearchParams, useRouter } from 'expo-router';
import { ActivityIndicator, Platform, StyleSheet, Text, View } from 'react-native';

import { PlaybackController } from '../../src/playback/PlaybackController.js';
import { exitPlaybackRoute } from '../../src/playback/playerNavigation.js';
import { restoreAppPortrait } from '../../src/playback/playerOrientation.js';
import { usePlaybackItem } from '../../src/playback/usePlaybackItem.js';
import { SESSION_STATUS, useSession } from '../../src/session/SessionProvider.js';
import { colors } from '../../src/theme/tokens.js';

function param(value) {
    return Array.isArray(value) ? value[0] : value;
}

export default function PlayerRoute() {
    const params = useLocalSearchParams();
    const isFocused = useIsFocused();
    const router = useRouter();
    const session = useSession();
    const itemId = param(params.itemId);
    const mode = param(params.mode);
    const preferredMediaSourceId = param(params.mediaSourceId) ?? null;
    const itemState = usePlaybackItem(session.client, session.userId, itemId);

    if (session.status === SESSION_STATUS.signedOut) return <Redirect href="/login" />;
    if (!itemId || itemState.failed) {
        return (
            <View style={styles.screen}>
                <Text style={styles.error}>Unable to load this item.</Text>
            </View>
        );
    }
    if (!itemState.item) {
        return (
            <View style={styles.screen}>
                <ActivityIndicator color={colors.text} size="large" />
            </View>
        );
    }
    const startTimeTicks = mode === 'restart'
        ? 0
        : itemState.item.UserData?.PlaybackPositionTicks ?? 0;

    return (
        <PlaybackController
            active={isFocused}
            client={session.client}
            item={itemState.item}
            onAdvance={(nextItem) => router.replace({
                pathname: '/player/[itemId]',
                params: { itemId: nextItem.Id, mode: 'resume', origin: 'auto-next' }
            })}
            onExit={() => exitPlaybackRoute(router, restoreAppPortrait)}
            platform={Platform.OS}
            preferredMediaSourceId={preferredMediaSourceId}
            serverUrl={session.serverUrl}
            startTimeTicks={startTimeTicks}
            userId={session.userId}
        />
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: '#000000',
        alignItems: 'center',
        justifyContent: 'center'
    },
    error: {
        color: colors.text,
        fontSize: 16
    }
});
