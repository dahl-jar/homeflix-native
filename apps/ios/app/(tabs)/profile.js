import { Ionicons } from '@expo/vector-icons';
import Constants from 'expo-constants';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ScrollView, Text, View, Pressable, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ScreenBackground } from '../../src/components/ScreenBackground/ScreenBackground.js';
import { useSession } from '../../src/session/SessionProvider.js';
import { colors, radius, spacing } from '../../src/theme/tokens.js';

const BOTTOM_CLEARANCE = 120;

function Section({ label, children }) {
    return (
        <View style={styles.section}>
            <Text style={styles.sectionLabel}>{label}</Text>
            <View style={styles.card}>{children}</View>
        </View>
    );
}

function Row({ icon, label, value, chevron = false, onPress, last = false }) {
    return (
        <Pressable
            style={[styles.row, !last && styles.rowBorder]}
            onPress={onPress}
            disabled={!onPress}
        >
            <Ionicons name={icon} size={20} color={colors.textDim} style={styles.rowIcon} />
            <Text style={styles.rowLabel}>{label}</Text>
            {value ? <Text style={styles.rowValue}>{value}</Text> : null}
            {chevron ? <Ionicons name="chevron-forward" size={16} color={colors.textDim} /> : null}
        </Pressable>
    );
}

export default function SettingsScreen() {
    const session = useSession();
    const router = useRouter();
    const insets = useSafeAreaInsets();
    const [cacheCleared, setCacheCleared] = useState(false);
    const user = session.user;

    const onSwitchProfile = async () => {
        await session.signOut();
        router.replace('/login');
    };

    const onClearImageCache = async () => {
        await Image.clearDiskCache();
        await Image.clearMemoryCache();
        setCacheCleared(true);
    };

    return (
        <View style={styles.root}>
            <ScreenBackground />
            <ScrollView
                style={styles.screen}
                contentContainerStyle={{ paddingTop: insets.top + 16, paddingBottom: BOTTOM_CLEARANCE }}
            >
            <Text style={styles.title}>Settings</Text>

            <View style={styles.profileCard}>
                {user?.PrimaryImageTag ? (
                    <Image
                        source={{
                            uri: `${session.serverUrl}/Users/${user.Id}/Images/Primary?tag=${user.PrimaryImageTag}&quality=90`
                        }}
                        style={styles.avatar}
                    />
                ) : (
                    <View style={[styles.avatar, styles.avatarFallback]}>
                        <Ionicons name="person" size={30} color="#e2d9d7" />
                    </View>
                )}
                <View style={styles.profileMeta}>
                    <Text style={styles.profileName}>{user?.Name ?? ''}</Text>
                    <Text style={styles.profileSub}>Homeflix profile</Text>
                </View>
                <Pressable style={styles.switchButton} onPress={onSwitchProfile}>
                    <Text style={styles.switchText}>Switch</Text>
                </Pressable>
            </View>

            <Section label="SERVER">
                <Row icon="server-outline" label="Address" value={session.serverUrl?.replace('http://', '')} />
                <Row icon="pulse-outline" label="Status" value="Connected" last />
            </Section>

            <Section label="APP">
                <Row
                    icon="image-outline"
                    label="Clear image cache"
                    value={cacheCleared ? 'Cleared' : null}
                    chevron={!cacheCleared}
                    onPress={onClearImageCache}
                />
                <Row
                    icon="information-circle-outline"
                    label="Version"
                    value={Constants.expoConfig?.version ?? '1.0.0'}
                    last
                />
            </Section>
            </ScrollView>
        </View>
    );
}

const styles = StyleSheet.create({
    root: {
        flex: 1,
        backgroundColor: colors.bg
    },
    screen: {
        flex: 1,
        paddingHorizontal: spacing.screen
    },
    title: {
        color: colors.text,
        fontSize: 28,
        fontWeight: '700',
        marginBottom: 20
    },
    profileCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: colors.bgRaised,
        borderRadius: radius.card + 4,
        padding: 14,
        marginBottom: 8
    },
    avatar: {
        width: 56,
        height: 56,
        borderRadius: radius.card
    },
    avatarFallback: {
        backgroundColor: '#5f312e',
        alignItems: 'center',
        justifyContent: 'center'
    },
    profileMeta: {
        flex: 1,
        marginLeft: 12
    },
    profileName: {
        color: colors.text,
        fontSize: 17,
        fontWeight: '600'
    },
    profileSub: {
        color: colors.textDim,
        fontSize: 12,
        marginTop: 2
    },
    switchButton: {
        borderColor: colors.pillBorder,
        borderWidth: 1,
        borderRadius: radius.pill,
        paddingHorizontal: 16,
        paddingVertical: 8
    },
    switchText: {
        color: colors.text,
        fontSize: 13,
        fontWeight: '500'
    },
    section: {
        marginTop: 18
    },
    sectionLabel: {
        color: colors.textDim,
        fontSize: 12,
        letterSpacing: 1,
        marginBottom: 8,
        marginLeft: 4
    },
    card: {
        backgroundColor: colors.bgRaised,
        borderRadius: radius.card + 4,
        paddingHorizontal: 14
    },
    row: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 14
    },
    rowBorder: {
        borderBottomWidth: 1,
        borderBottomColor: 'rgba(255, 255, 255, 0.07)'
    },
    rowIcon: {
        marginRight: 12
    },
    rowLabel: {
        color: colors.text,
        fontSize: 15,
        flex: 1
    },
    rowValue: {
        color: colors.textDim,
        fontSize: 13,
        marginRight: 6
    }
});
