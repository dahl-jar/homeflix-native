import { useRef } from 'react';
import { Modal, Pressable, Text, ScrollView, StyleSheet, View } from 'react-native';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '../theme/tokens.js';

const OPTION_HEIGHT = 45;

/**
 * Netflix-style fullscreen picker: blurred dark overlay, large centered
 * options, glass close button. Options are { key, label }; a null-key
 * entry acts as the clear/default choice.
 */
export function PickerOverlay({ visible, title, entries, isSelected, onChoose, onClose }) {
    const insets = useSafeAreaInsets();
    const scrollRef = useRef(null);

    return (
        <Modal transparent visible={visible} animationType="fade" onRequestClose={onClose}>
            <BlurView intensity={70} tint="dark" style={styles.overlay}>
                <View style={styles.overlayShade} />
                <Text style={[styles.overlayTitle, { marginTop: insets.top + 24 }]}>
                    {title.toUpperCase()}
                </Text>
                <ScrollView
                    ref={scrollRef}
                    style={styles.list}
                    contentContainerStyle={styles.listContent}
                    showsVerticalScrollIndicator={false}
                    onLayout={(event) => {
                        const viewport = event.nativeEvent.layout.height;
                        const selectedIndex = entries.findIndex((entry) => isSelected(entry));
                        const target = selectedIndex * OPTION_HEIGHT - viewport / 2 + OPTION_HEIGHT / 2;
                        if (target > 0) scrollRef.current?.scrollTo({ y: target, animated: false });
                    }}
                >
                    {entries.map((entry) => (
                        <Pressable
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
                <View style={[styles.closeWrap, { paddingBottom: insets.bottom + 24 }]}>
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
        paddingVertical: 32,
        paddingHorizontal: 24
    },
    option: {
        height: OPTION_HEIGHT,
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
