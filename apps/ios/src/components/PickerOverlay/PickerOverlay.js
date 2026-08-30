import { Ionicons } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import { useRef, useState } from 'react';
import { Modal, Pressable, Text, ScrollView, StyleSheet, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '../../theme/tokens.js';

import {
    PICKER_OPTION_HEIGHT,
    pickerIndexFromOffset,
    pickerInsetForViewport,
    pickerOffsetForIndex,
    selectedPickerIndex
} from './pickerModel.js';

export function PickerOverlay({ visible, title, entries, isSelected, onChoose, onClose }) {
    const insets = useSafeAreaInsets();
    const { height, width } = useWindowDimensions();
    const landscape = width > height;
    const scrollRef = useRef(null);
    const choosingRef = useRef(false);
    const [viewportHeight, setViewportHeight] = useState(0);
    const selectedIndex = selectedPickerIndex(entries, isSelected);
    const contentInset = pickerInsetForViewport(viewportHeight);
    const scrollToSelected = () => {
        if (selectedIndex < 0) return;
        scrollRef.current?.scrollTo({
            y: pickerOffsetForIndex(selectedIndex),
            animated: false
        });
    };
    const chooseAtOffset = (offset) => {
        if (choosingRef.current) return;
        const index = pickerIndexFromOffset(offset, entries.length);
        if (index < 0) return;
        choosingRef.current = true;
        onChoose(entries[index].key);
    };
    const adjustSelection = (direction) => {
        const index = Math.max(0, Math.min(entries.length - 1, selectedIndex + direction));
        if (entries[index]) onChoose(entries[index].key);
    };

    return (
        <Modal
            transparent
            visible={visible}
            animationType="fade"
            supportedOrientations={['portrait', 'portrait-upside-down', 'landscape', 'landscape-left', 'landscape-right']}
            onRequestClose={onClose}
            onShow={() => scrollToSelected()}
        >
            <BlurView intensity={70} tint="dark" style={styles.overlay}>
                <View style={styles.overlayShade} />
                <Text style={[
                    styles.overlayTitle,
                    { marginTop: Math.max(insets.top, landscape ? 12 : 24) }
                ]}>
                    {title.toUpperCase()}
                </Text>
                <ScrollView
                    ref={scrollRef}
                    style={styles.list}
                    contentContainerStyle={[
                        styles.listContent,
                        landscape ? styles.listContentLandscape : styles.listContentPortrait,
                        {
                            paddingLeft: Math.max(insets.left, 24),
                            paddingRight: Math.max(insets.right, 24),
                            paddingTop: contentInset,
                            paddingBottom: contentInset
                        }
                    ]}
                    accessibilityActions={[
                        { name: 'increment', label: 'Next option' },
                        { name: 'decrement', label: 'Previous option' }
                    ]}
                    accessibilityRole="adjustable"
                    accessibilityValue={{ text: entries[selectedIndex]?.label ?? '' }}
                    decelerationRate="fast"
                    showsVerticalScrollIndicator={false}
                    snapToAlignment="start"
                    snapToInterval={PICKER_OPTION_HEIGHT}
                    onAccessibilityAction={({ nativeEvent }) => {
                        if (nativeEvent.actionName === 'increment') adjustSelection(1);
                        if (nativeEvent.actionName === 'decrement') adjustSelection(-1);
                    }}
                    onLayout={(event) => {
                        setViewportHeight(event.nativeEvent.layout.height);
                        scrollToSelected();
                    }}
                    onContentSizeChange={scrollToSelected}
                    onMomentumScrollEnd={({ nativeEvent }) => {
                        chooseAtOffset(nativeEvent.contentOffset.y);
                    }}
                    onScrollBeginDrag={() => {
                        choosingRef.current = false;
                    }}
                    onScrollEndDrag={({ nativeEvent }) => {
                        chooseAtOffset(
                            nativeEvent.targetContentOffset?.y ?? nativeEvent.contentOffset.y
                        );
                    }}
                >
                    {entries.map((entry) => (
                        <Pressable
                            accessibilityLabel={entry.label}
                            accessibilityRole="button"
                            accessibilityState={{ selected: isSelected(entry) }}
                            key={entry.key ?? 'clear'}
                            style={styles.option}
                            onPress={() => onChoose(entry.key)}
                        >
                            <Text style={[styles.optionText, isSelected(entry) && styles.optionTextSelected]}>
                                {entry.label}
                            </Text>
                        </Pressable>
                    ))}
                </ScrollView>
                <View style={[
                    styles.closeWrap,
                    { paddingBottom: Math.max(insets.bottom, landscape ? 12 : 24) }
                ]}>
                    <Pressable style={styles.close} onPress={onClose}>
                        <Ionicons name="close" size={26} color="#141414" />
                    </Pressable>
                </View>
            </BlurView>
        </Modal>
    );
}

const styles = StyleSheet.create({
    overlay: {
        flex: 1
    },
    overlayShade: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(10, 9, 9, 0.82)'
    },
    overlayTitle: {
        color: colors.textDim,
        fontSize: 13,
        letterSpacing: 2,
        textAlign: 'center'
    },
    list: {
        flex: 1
    },
    listContent: {
        flexGrow: 1,
        justifyContent: 'center',
        alignItems: 'center'
    },
    listContentLandscape: {
        paddingVertical: 12
    },
    listContentPortrait: {
        paddingVertical: 32
    },
    option: {
        height: PICKER_OPTION_HEIGHT,
        width: '100%',
        maxWidth: 720,
        justifyContent: 'center',
        alignItems: 'center'
    },
    optionText: {
        color: 'rgba(238, 236, 235, 0.55)',
        fontSize: 17,
        fontWeight: '400',
        textAlign: 'center'
    },
    optionTextSelected: {
        color: '#ffffff',
        fontSize: 20,
        fontWeight: '700',
        textAlign: 'center'
    },
    closeWrap: {
        alignItems: 'center'
    },
    close: {
        width: 52,
        height: 52,
        borderRadius: 26,
        backgroundColor: '#ffffff',
        alignItems: 'center',
        justifyContent: 'center'
    }
});
