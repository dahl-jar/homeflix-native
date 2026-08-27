import { Ionicons } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import { ActivityIndicator, FlatList, Modal, Pressable, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '../../theme/tokens.js';

import { EpisodePickerRow } from './EpisodePickerRow.js';

const ROW_HEIGHT = 120;

function StatusMessage({ status }) {
    if (status === 'loading' || status === 'idle') {
        return <ActivityIndicator color={colors.text} size="large" />;
    }
    const message = status === 'failed' ? 'Unable to load episodes' : 'No episodes available';
    return <Text style={styles.status}>{message}</Text>;
}

export function EpisodePickerOverlay({ episodeMenu, serverUrl, visible, onChoose, onClose }) {
    const insets = useSafeAreaInsets();
    const { height, width } = useWindowDimensions();
    const landscape = width > height;
    const columns = landscape ? 2 : 1;
    const currentIndex = episodeMenu.entries.findIndex((entry) => entry.current);
    const initialIndex = Math.max(0, currentIndex - columns);
    const hasEntries = episodeMenu.entries.length > 0;

    return (
        <Modal
            animationType="fade"
            onRequestClose={onClose}
            supportedOrientations={['portrait', 'portrait-upside-down', 'landscape', 'landscape-left', 'landscape-right']}
            transparent
            visible={visible}
        >
            <BlurView intensity={80} tint="dark" style={styles.overlay}>
                <View style={styles.shade} />
                <View style={[
                    styles.header,
                    {
                        paddingTop: Math.max(insets.top, 12),
                        paddingLeft: Math.max(insets.left, 20),
                        paddingRight: Math.max(insets.right, 20)
                    }
                ]}>
                    <View style={styles.heading}>
                        <Text style={styles.eyebrow}>EPISODES</Text>
                        {episodeMenu.seriesName ? (
                            <Text numberOfLines={1} style={styles.series}>{episodeMenu.seriesName}</Text>
                        ) : null}
                    </View>
                    <Pressable accessibilityLabel="Close episodes" accessibilityRole="button" onPress={onClose} style={styles.close}>
                        <Ionicons color={colors.text} name="close" size={26} />
                    </Pressable>
                </View>
                {hasEntries ? (
                    <FlatList
                        key={columns}
                        columnWrapperStyle={columns === 2 ? styles.columns : undefined}
                        contentContainerStyle={[
                            styles.listContent,
                            {
                                paddingLeft: Math.max(insets.left, 12),
                                paddingRight: Math.max(insets.right, 12),
                                paddingBottom: Math.max(insets.bottom, 16)
                            }
                        ]}
                        data={episodeMenu.entries}
                        getItemLayout={(_, index) => ({
                            index,
                            length: ROW_HEIGHT,
                            offset: Math.floor(index / columns) * ROW_HEIGHT
                        })}
                        initialScrollIndex={initialIndex}
                        numColumns={columns}
                        renderItem={({ item: entry }) => (
                            <View style={styles.item}>
                                <EpisodePickerRow
                                    compact={landscape}
                                    entry={entry}
                                    selected={entry.current}
                                    serverUrl={serverUrl}
                                    onPress={() => onChoose(entry.key)}
                                />
                            </View>
                        )}
                        showsVerticalScrollIndicator={false}
                    />
                ) : (
                    <View style={styles.statusWrap}>
                        <StatusMessage status={episodeMenu.status} />
                    </View>
                )}
            </BlurView>
        </Modal>
    );
}

const styles = StyleSheet.create({
    overlay: {
        flex: 1
    },
    shade: {
        ...StyleSheet.absoluteFillObject,
        backgroundColor: 'rgba(10, 9, 9, 0.9)'
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingBottom: 10
    },
    heading: {
        flex: 1
    },
    eyebrow: {
        color: colors.textDim,
        fontSize: 11,
        fontWeight: '600',
        letterSpacing: 2
    },
    series: {
        color: colors.text,
        fontSize: 20,
        fontWeight: '700',
        marginTop: 2
    },
    close: {
        width: 48,
        height: 48,
        borderRadius: 24,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'rgba(255, 255, 255, 0.1)'
    },
    listContent: {
        paddingTop: 4
    },
    columns: {
        gap: 8
    },
    item: {
        flex: 1,
        minWidth: 0,
        marginBottom: 2
    },
    statusWrap: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center'
    },
    status: {
        color: colors.textDim,
        fontSize: 15
    }
});
