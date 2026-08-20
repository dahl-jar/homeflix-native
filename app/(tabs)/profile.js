import { Text, View, Pressable, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { Image } from 'expo-image';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useSession } from '../../src/session/SessionProvider.js';
import { colors, radius } from '../../src/theme/tokens.js';

export default function ProfileScreen() {
    const session = useSession();
    const router = useRouter();
    const insets = useSafeAreaInsets();
    const user = session.user;

    const onSwitchProfile = async () => {
        await session.signOut();
        router.replace('/login');
    };

    return (
        <View style={[styles.screen, { paddingTop: insets.top + 40 }]}>
            {user?.PrimaryImageTag ? (
                <Image
                    source={{
                        uri: `${session.serverUrl}/Users/${user.Id}/Images/Primary?tag=${user.PrimaryImageTag}&quality=90`
                    }}
                    style={styles.avatar}
                />
            ) : (
                <View style={[styles.avatar, styles.avatarFallback]} />
            )}
            <Text style={styles.name}>{user?.Name ?? ''}</Text>

            <Pressable style={styles.action} onPress={onSwitchProfile}>
                <Text style={styles.actionText}>Switch Profile</Text>
            </Pressable>

            <View style={styles.meta}>
                <Text style={styles.metaLine}>Server: {session.serverUrl}</Text>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: colors.bg,
        alignItems: 'center'
    },
    avatar: {
        width: 110,
        height: 110,
        borderRadius: radius.card + 2
    },
    avatarFallback: {
        backgroundColor: '#5f312e'
    },
    name: {
        color: colors.text,
        fontSize: 22,
        fontWeight: '600',
        marginTop: 14
    },
    action: {
        marginTop: 32,
        borderColor: colors.pillBorder,
        borderWidth: 1,
        borderRadius: radius.pill,
        paddingHorizontal: 28,
        paddingVertical: 12
    },
    actionText: {
        color: colors.text,
        fontSize: 15,
        fontWeight: '500'
    },
    meta: {
        position: 'absolute',
        bottom: 130
    },
    metaLine: {
        color: colors.textDim,
        fontSize: 12
    }
});
