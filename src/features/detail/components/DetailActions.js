import { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { GhostTile } from '../../../components/GhostTile.js';
import { PickerOverlay } from '../../../components/PickerOverlay.js';
import { PlayPill } from '../../../components/PlayPill.js';
import { playerLauncher } from '../../../playback/playerLauncher.js';
import { AUTO_SOURCE_KEY, sourceOptions } from '../sources.js';

export function DetailActions({ item, sources }) {
    const [sourceSelection, setSourceSelection] = useState({ itemId: null, key: AUTO_SOURCE_KEY });
    const [pickerOpen, setPickerOpen] = useState(false);
    const sourceKey = sourceSelection.itemId === item.Id ? sourceSelection.key : AUTO_SOURCE_KEY;

    return (
        <>
            <PlayPill
                item={item}
                origin="detail"
                label={item.UserData?.PlaybackPositionTicks > 0 ? 'Resume' : 'Play'}
                mediaSourceId={sourceKey === AUTO_SOURCE_KEY ? null : sourceKey}
            />
            <View style={styles.tiles}>
                <GhostTile icon="film-outline" label="Trailer" onPress={() => playerLauncher.play(item, 'trailer')} />
                <GhostTile icon="checkmark-circle-outline" label={item.UserData?.Played ? 'Played' : 'Mark Played'} onPress={() => {}} />
                <GhostTile icon="refresh-outline" label="Restart" onPress={() => playerLauncher.play(item, 'restart')} />
                <GhostTile
                    icon={sourceKey === AUTO_SOURCE_KEY ? 'layers-outline' : 'layers'}
                    label="Source"
                    onPress={() => setPickerOpen(true)}
                />
            </View>
            <PickerOverlay
                visible={pickerOpen}
                title="Source"
                entries={sourceOptions(sources, item.Name)}
                isSelected={(entry) => entry.key === sourceKey}
                onChoose={(key) => {
                    setPickerOpen(false);
                    setSourceSelection({ itemId: item.Id, key: key ?? AUTO_SOURCE_KEY });
                }}
                onClose={() => setPickerOpen(false)}
            />
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
