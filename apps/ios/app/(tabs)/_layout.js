import { Stack } from 'expo-router';
import { useEffect, useState } from 'react';
import { View } from 'react-native';

import { fetchUserViews } from '../../src/api/items/items.js';
import { BottomPill } from '../../src/components/BottomPill/BottomPill.js';
import { useSession } from '../../src/session/SessionProvider.js';
import { colors } from '../../src/theme/tokens.js';

export default function TabsLayout() {
    const session = useSession();
    const [views, setViews] = useState([]);

    useEffect(() => {
        if (!session.client || !session.userId) return;
        fetchUserViews(session.client, session.userId).then((result) => setViews(result.Items));
    }, [session.client, session.userId]);

    return (
        <View style={{ flex: 1, backgroundColor: colors.bg }}>
            <Stack
                screenOptions={{
                    headerShown: false,
                    animation: 'fade',
                    contentStyle: { backgroundColor: colors.bg }
                }}
            />
            <BottomPill views={views} />
        </View>
    );
}
