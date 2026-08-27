import { useEffect, useMemo, useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { PickerOverlay } from '../../components/PickerOverlay/PickerOverlay.js';
import { EpisodePickerOverlay } from '../episodes/EpisodePickerOverlay.js';
import { audioTrackEntries, selectedTrackKey, subtitleTrackEntries } from '../tracks/playerTrackMenu.js';

import { PlayerActionButton } from './PlayerActionButton.js';

export function PlayerBottomActions({ episodeMenu, nextEpisode, onInteract, onMenuOpenChange, playback, serverUrl }) {
    const [menu, setMenu] = useState(null);
    const audioTracks = useMemo(() => audioTrackEntries(playback.snapshot), [playback.snapshot]);
    const subtitleTracks = useMemo(() => subtitleTrackEntries(playback.snapshot), [playback.snapshot]);
    useEffect(() => {
        onMenuOpenChange(menu !== null);
        return () => onMenuOpenChange(false);
    }, [menu, onMenuOpenChange]);
    const open = (nextMenu) => {
        onInteract();
        setMenu(nextMenu);
    };
    const close = () => {
        setMenu(null);
        onInteract();
    };
    const openEpisodes = () => {
        open('episodes');
        episodeMenu.load();
    };

    return (
        <>
            <View style={styles.actions}>
                <PlayerActionButton family="material" icon="audiotrack" label="Audio" onPress={() => open('audio')} />
                <PlayerActionButton family="material" icon="subtitles" label="Subtitles" onPress={() => open('subtitles')} />
                {episodeMenu.available ? (
                    <PlayerActionButton icon="list-outline" label="Episodes" onPress={openEpisodes} />
                ) : null}
                {nextEpisode.nextEpisode ? (
                    <PlayerActionButton icon="play-skip-forward" label="Next Episode" onPress={nextEpisode.playNext} />
                ) : null}
            </View>
            <PickerOverlay
                visible={menu === 'audio'}
                title="Audio"
                entries={audioTracks}
                isSelected={(entry) => selectedTrackKey(playback.snapshot, entry)}
                onChoose={(key) => {
                    const index = Number(key.split(':')[1]);
                    playback.selectAudioTrack(playback.snapshot.audioTracks[index]);
                    close();
                }}
                onClose={close}
            />
            <PickerOverlay
                visible={menu === 'subtitles'}
                title="Subtitles"
                entries={subtitleTracks}
                isSelected={(entry) => selectedTrackKey(playback.snapshot, entry)}
                onChoose={(key) => {
                    const indexText = key.split(':')[1];
                    const track = indexText === 'off'
                        ? null
                        : playback.snapshot.subtitleTracks[Number(indexText)];
                    playback.selectSubtitleTrack(track);
                    close();
                }}
                onClose={close}
            />
            <EpisodePickerOverlay
                episodeMenu={episodeMenu}
                serverUrl={serverUrl}
                visible={menu === 'episodes'}
                onChoose={(itemId) => {
                    close();
                    episodeMenu.select(itemId);
                }}
                onClose={close}
            />
        </>
    );
}

const styles = StyleSheet.create({
    actions: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-evenly',
        marginTop: 4,
        minHeight: 66
    }
});
