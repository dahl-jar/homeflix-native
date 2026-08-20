import { Redirect } from 'expo-router';
import { ActivityIndicator, Text, Pressable, View, StyleSheet } from 'react-native';

import { useSession, SESSION_STATUS } from '../src/session/SessionProvider.js';
import { colors } from '../src/theme/tokens.js';

export default function Entry() {
    const session = useSession();

    if (session.status === SESSION_STATUS.signedIn) return <Redirect href="/(tabs)/home" />;
    if (session.status === SESSION_STATUS.signedOut) return <Redirect href="/login" />;

    return (
        <View style={styles.screen}>
            {session.status === SESSION_STATUS.unreachable ? (
                <>
                    <Text style={styles.wordmark}>HOMEFLIX</Text>
                    <Text style={styles.message}>
                        Can't reach the server.{'\n'}Check Wi-Fi or private network and try again.
                    </Text>
                    <Pressable style={styles.retry} onPress={session.retryResolve}>
                        <Text style={styles.retryText}>Retry</Text>
                    </Pressable>
                </>
            ) : (
                <ActivityIndicator color={colors.text} />
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: colors.bg,
        alignItems: 'center',
        justifyContent: 'center',
        gap: 16
    },
    wordmark: {
        color: colors.accent,
        fontSize: 22,
        fontWeight: '900',
        letterSpacing: 4
    },
    message: {
        color: colors.textDim,
        textAlign: 'center'
    },
    retry: {
        backgroundColor: colors.accent,
        borderRadius: 8,
        paddingHorizontal: 24,
        paddingVertical: 10
    },
    retryText: {
        color: '#ffffff',
        fontWeight: '600'
    }
});
