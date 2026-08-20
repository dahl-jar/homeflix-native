import { useState } from 'react';
import { Pressable, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

import { PickerOverlay } from './PickerOverlay.js';
import { colors, radius } from '../theme/tokens.js';

/**
 * A filter pill that opens the fullscreen picker. `options` are
 * { key, label }; `selected` is a key or null; `clearLabel` prepends an
 * unselect entry when provided.
 */
export function DropdownPill({ title, options, selected, onSelect, clearLabel }) {
    const [open, setOpen] = useState(false);
    const selectedOption = options.find((option) => option.key === selected);
    const active = selectedOption != null;

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
            <PickerOverlay
                visible={open}
                title={title}
                entries={entries}
                isSelected={(entry) => (entry.key === null ? !active : entry.key === selected)}
                onChoose={(key) => {
                    setOpen(false);
                    onSelect(key);
                }}
                onClose={() => setOpen(false)}
            />
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
    }
});
