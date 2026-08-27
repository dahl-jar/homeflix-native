import { StyleSheet, View } from 'react-native';

import { GhostTile } from '../../../components/GhostTile.js';
import { PlayPill } from '../../../components/PlayPill.js';
import { playerLauncher } from '../../../playback/playerLauncher.js';

export function DetailActions({ item }) {
    return (
        <>
            <PlayPill
                item={item}
                origin="detail"
                label={item.UserData?.PlaybackPositionTicks > 0 ? 'Resume' : 'Play'}
            />
            <View style={styles.tiles}>
                <GhostTile icon="film-outline" label="Trailer" onPress={() => playerLauncher.play(item, 'trailer')} />
                <GhostTile icon="checkmark-circle-outline" label={item.UserData?.Played ? 'Played' : 'Mark Played'} onPress={() => {}} />
                <GhostTile icon="refresh-outline" label="Restart" onPress={() => playerLauncher.play(item, 'restart')} />
            </View>
        </>
    );
}

const styles = StyleSheet.create({
    tiles: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        marginTop: 10,
        marginBottom: 6,
        paddingHorizontal: 4
    }
});
