import { useLocalSearchParams, useRouter } from 'expo-router';
import { StyleSheet, useWindowDimensions, View } from 'react-native';

import { DetailView } from '../../src/features/detail/components/DetailView.js';
import { useDetailData } from '../../src/features/detail/useDetailData.js';
import { useSession } from '../../src/session/SessionProvider.js';
import { colors } from '../../src/theme/tokens.js';

export default function DetailScreen() {
    const { id } = useLocalSearchParams();
    const router = useRouter();
    const { width } = useWindowDimensions();
    const { client, serverUrl, userId } = useSession();
    const data = useDetailData({ client, itemId: id, userId });

    if (!data.item) return <View style={styles.screen} />;

    return (
        <DetailView
            data={data}
            onBack={() => router.back()}
            serverUrl={serverUrl}
            width={width}
        />
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: colors.bg
    }
});
