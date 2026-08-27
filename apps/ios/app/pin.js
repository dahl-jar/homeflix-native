import { useRouter, useLocalSearchParams } from 'expo-router';
import { useRef, useState } from 'react';
import { Text, View, Pressable, StyleSheet } from 'react-native';

import { authenticate } from '../src/api/auth/auth.js';
import { BackButton } from '../src/components/BackButton/BackButton.js';
import { createPinModel } from '../src/features/auth/pinModel.js';
import { useSession } from '../src/session/SessionProvider.js';
import { colors } from '../src/theme/tokens.js';

const KEYPAD = [1, 2, 3, 4, 5, 6, 7, 8, 9, null, 0, 'back'];
const PIN_LENGTH = 4;
const VERTICAL_BIAS = 48;

export default function PinScreen() {
    const router = useRouter();
    const session = useSession();
    const { username } = useLocalSearchParams();
    const modelRef = useRef(createPinModel(PIN_LENGTH));
    const [filled, setFilled] = useState(0);
    const [error, setError] = useState(false);

    const onDigit = async (digit) => {
        const model = modelRef.current;
        const result = model.append(digit);
        setFilled(model.digits.length);
        setError(false);
        if (!result.submit) return;
        try {
            const auth = await authenticate(session.client, username, result.pin);
            await session.signIn(auth);
            router.replace('/(tabs)/home');
        } catch {
            model.clear();
            setFilled(0);
            setError(true);
        }
    };

    const onBackspace = () => {
        const model = modelRef.current;
        model.digits.pop();
        setFilled(model.digits.length);
    };

    return (
        <View style={styles.screen}>
            <BackButton style={styles.back} onPress={() => router.back()} />
            <Text style={styles.title}>Enter your PIN to access this profile.</Text>
            <View style={styles.boxes}>
                {Array.from({ length: PIN_LENGTH }, (_, i) => (
                    <View key={i} style={[styles.box, error && styles.boxError]}>
                        {i < filled ? <View style={styles.dot} /> : null}
                    </View>
                ))}
            </View>
            <View style={styles.keypad}>
                {KEYPAD.map((key, i) =>
                    key === null ? (
                        <View key={i} style={styles.key} />
                    ) : (
                        <Pressable
                            key={i}
                            style={styles.key}
                            onPress={() => (key === 'back' ? onBackspace() : onDigit(key))}
                        >
                            <Text style={styles.keyText}>{key === 'back' ? '⌫' : key}</Text>
                        </Pressable>
                    )
                )}
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: colors.bg,
        alignItems: 'center',
        justifyContent: 'center',
        paddingTop: VERTICAL_BIAS
    },
    back: {
        top: 62,
        left: 16
    },
    title: {
        color: colors.text,
        fontSize: 20,
        textAlign: 'center',
        paddingHorizontal: 40,
        marginBottom: 32
    },
    boxes: {
        flexDirection: 'row',
        gap: 14,
        marginBottom: 48
    },
    box: {
        width: 48,
        height: 58,
        borderWidth: 1,
        borderColor: '#979797',
        alignItems: 'center',
        justifyContent: 'center'
    },
    boxError: {
        borderColor: colors.accent
    },
    dot: {
        width: 13,
        height: 13,
        borderRadius: 7,
        backgroundColor: '#ffffff'
    },
    keypad: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        width: 3 * 88,
        justifyContent: 'center'
    },
    key: {
        width: 88,
        height: 72,
        alignItems: 'center',
        justifyContent: 'center'
    },
    keyText: {
        color: colors.text,
        fontSize: 26
    }
});
