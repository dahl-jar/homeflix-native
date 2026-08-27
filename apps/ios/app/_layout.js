import { Stack, useSegments } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { routeAllowsRotation } from '../src/playback/player/orientationPolicy.js';
import { useRouteOrientation } from '../src/playback/player/playerOrientation.js';
import { SessionProvider } from '../src/session/SessionProvider.js';
import { colors } from '../src/theme/tokens.js';

export default function RootLayout() {
    const segments = useSegments();
    useRouteOrientation(routeAllowsRotation(segments));

    return (
        <SafeAreaProvider>
            <SessionProvider>
                <StatusBar style="light" />
                <Stack
                    screenOptions={{
                        headerShown: false,
                        contentStyle: { backgroundColor: colors.bg }
                    }}
                >
                    <Stack.Screen
                        name="player/[itemId]"
                        options={{ animation: 'fade', presentation: 'fullScreenModal' }}
                    />
                </Stack>
            </SessionProvider>
        </SafeAreaProvider>
    );
}
