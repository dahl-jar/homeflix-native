import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { SessionProvider } from '../src/session/SessionProvider.js';
import { colors } from '../src/theme/tokens.js';

export default function RootLayout() {
    return (
        <SafeAreaProvider>
            <SessionProvider>
                <StatusBar style="light" />
                <Stack
                    screenOptions={{
                        headerShown: false,
                        contentStyle: { backgroundColor: colors.bg }
                    }}
                />
            </SessionProvider>
        </SafeAreaProvider>
    );
}
