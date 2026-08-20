import { useRef, useState } from 'react';
import { Modal, Pressable, Text, View, ScrollView, StyleSheet } from 'react-native';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, radius } from '../theme/tokens.js';

/**
 * A filter pill that opens a Netflix-style fullscreen picker: blurred dark
 * overlay, large centered options, glass close button. `options` are
 * { key, label }; `selected` is a key or null; `clearLabel` prepends an
 * unselect entry when provided.
 */
const OPTION_HEIGHT = 45;

export function DropdownPill({ title, options, selected, onSelect, clearLabel }) {
    const [open, setOpen] = useState(false);
    const insets = useSafeAreaInsets();
    const scrollRef = useRef(null);
    const selectedOption = options.find((option) => option.key === selected);
    const active = selectedOption != null;

    const choose = (key) => {
        setOpen(false);
        onSelect(key);
    };

    const entries = [
        ...(clearLabel ? [{ key: null, label: clearLabel }] : []),
        ...options
    ];

    return (
        <>
            <Pressable
                style={[styles.pill, active && styles.pillActive]}
                onPress={() => setOpen(true)}
            >
                <Text style={[styles.pillText, active && styles.pillTextActive]}>
                    {selectedOption ? selectedOption.label : title}
                </Text>
                <Ionicons
                    name="chevron-down"
                    size={13}
                    color={active ? '#141414' : colors.textDim}
                />
            </Pressable>
            <Modal transparent visible={open} animationType="fade" onRequestClose={() => setOpen(false)}>
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
                            const selectedIndex = entries.findIndex((entry) =>
                                entry.key === null ? !active : entry.key === selected
                            );
                            const target = selectedIndex * OPTION_HEIGHT - viewport / 2 + OPTION_HEIGHT / 2;
                            if (target > 0) scrollRef.current?.scrollTo({ y: target, animated: false });
                        }}
                    >
                        {entries.map((entry) => {
                            const isSelected =
                                entry.key === null ? !active : entry.key === selected;
                            return (
                                <Pressable
                                    key={entry.key ?? 'clear'}
                                    style={styles.option}
                                    onPress={() => choose(entry.key)}
                                >
                                    <Text
                                        style={[styles.optionText, isSelected && styles.optionTextSelected]}
                                    >
                                        {entry.label}
                                    </Text>
                                </Pressable>
                            );
                        })}
                    </ScrollView>
                    <View style={[styles.closeWrap, { paddingBottom: insets.bottom + 24 }]}>
                        <Pressable style={styles.close} onPress={() => setOpen(false)}>
                            <Ionicons name="close" size={26} color="#141414" />
                        </Pressable>
                    </View>
                </BlurView>
            </Modal>
        </>
    );
}

const styles = StyleSheet.create({
    pill: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 5,
        backgroundColor: colors.bgRaised,
        borderRadius: radius.pill,
        paddingHorizontal: 14,
        paddingVertical: 8
    },
    pillActive: {
        backgroundColor: '#ffffff'
    },
    pillText: {
        color: colors.text,
        fontSize: 13,
        fontWeight: '500'
    },
    pillTextActive: {
        color: '#141414'
    },
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
        paddingVertical: 32
    },
    option: {
        height: OPTION_HEIGHT,
        justifyContent: 'center',
        alignItems: 'center'
    },
    optionText: {
        color: 'rgba(238, 236, 235, 0.55)',
        fontSize: 19,
        fontWeight: '400'
    },
    optionTextSelected: {
        color: '#ffffff',
        fontSize: 23,
        fontWeight: '700'
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
