import { useEffect, useState } from 'react';
import { Text, View, Pressable, StyleSheet, FlatList } from 'react-native';
import { useRouter } from 'expo-router';
import { Image } from 'expo-image';

import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';

import { useSession } from '../src/session/SessionProvider.js';
import { fetchPublicUsers, toGateCard, authenticate } from '../src/api/auth.js';
import { userImageUrl } from '../src/api/imageUrl.js';
import { colors, radius } from '../src/theme/tokens.js';

const CARD_SIZE = 110;

export default function LoginScreen() {
    const router = useRouter();
    const session = useSession();
    const [cards, setCards] = useState([]);

    useEffect(() => {
        if (!session.client) return;
        fetchPublicUsers(session.client).then((users) => setCards(users.map(toGateCard)));
    }, [session.client]);

    const onSelect = async (card) => {
        if (card.hasPassword) {
            router.push({ pathname: '/pin', params: { username: card.name } });
            return;
        }
        const result = await authenticate(session.client, card.name);
        await session.signIn(result);
        router.replace('/(tabs)/home');
    };

    return (
        <View style={styles.screen}>
            <Text style={styles.wordmark}>HOMEFLIX</Text>
            <Text style={styles.title}>Who's watching?</Text>
            <FlatList
                data={cards}
                numColumns={2}
                keyExtractor={(card) => card.id}
                columnWrapperStyle={styles.row}
                contentContainerStyle={styles.list}
                renderItem={({ item: card }) => {
                    const imageUri = card.imageTag
                        ? userImageUrl(session.serverUrl, { Id: card.id, PrimaryImageTag: card.imageTag })
                        : null;
                    return (
                        <Pressable style={styles.card} onPress={() => onSelect(card)}>
                            {imageUri ? (
                                <Image source={{ uri: imageUri }} style={styles.avatar} />
                            ) : (
                                <LinearGradient
                                    colors={['#7a3b38', '#4a2a28']}
                                    start={{ x: 0, y: 0 }}
                                    end={{ x: 1, y: 1 }}
                                    style={[styles.avatar, styles.avatarFallback]}
                                >
                                    <Ionicons name="person" size={44} color="#e2d9d7" />
                                </LinearGradient>
                            )}
                            <Text style={styles.name}>{card.name}</Text>
                        </Pressable>
                    );
                }}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: colors.bg,
        paddingTop: 80
    },
    wordmark: {
        color: colors.accent,
        fontSize: 18,
        fontWeight: '900',
        letterSpacing: 3,
        marginLeft: 20
    },
    title: {
        color: colors.text,
        fontSize: 32,
        fontWeight: '400',
        textAlign: 'center',
        marginTop: 60,
        marginBottom: 32
    },
    list: {
        alignItems: 'center'
    },
    row: {
        gap: 24,
        marginBottom: 24
    },
    card: {
        alignItems: 'center'
    },
    avatar: {
        width: CARD_SIZE,
        height: CARD_SIZE,
        borderRadius: radius.card + 2
    },
    avatarFallback: {
        alignItems: 'center',
        justifyContent: 'center'
    },
    name: {
        color: colors.textDim,
        marginTop: 8
    }
});
