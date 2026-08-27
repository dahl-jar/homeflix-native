import { Ionicons } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import { useRef } from 'react';
import { Modal, Pressable, Text, ScrollView, StyleSheet, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '../../theme/tokens.js';

const OPTION_HEIGHT = 45;

export function PickerOverlay({ visible, title, entries, isSelected, onChoose, onClose }) {
    const insets = useSafeAreaInsets();
    const { height, width } = useWindowDimensions();
    const landscape = width > height;
    const scrollRef = useRef(null);

    return (
        <Modal
            transparent
            visible={visible}
            animationType="fade"
            supportedOrientations={['portrait', 'portrait-upside-down', 'landscape', 'landscape-left', 'landscape-right']}
            onRequestClose={onClose}
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
                            paddingRight: Math.max(insets.right, 24)
                        }
                    ]}
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
        height: OPTION_HEIGHT,
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
