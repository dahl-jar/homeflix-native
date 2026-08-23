import { useEffect, useRef, useState } from 'react';
import { Animated, Easing } from 'react-native';

import { useReduceMotion } from './useReduceMotion.js';

const FADE_OUT_MS = 120;
const FADE_IN_MS = 200;
const RISE_PX = 4;

export function CrossfadeText({ accessibilityLiveRegion, style, value }) {
    const [shownValue, setShownValue] = useState(value);
    const [opacity] = useState(() => new Animated.Value(1));
    const [rise] = useState(() => new Animated.Value(0));
    const latest = useRef(value);
    const reduceMotion = useReduceMotion();

    useEffect(() => {
        latest.current = value;
        if (value === shownValue) return undefined;
        const fadeOut = Animated.timing(opacity, {
            duration: FADE_OUT_MS,
            toValue: 0,
            useNativeDriver: true
        });
        fadeOut.start(({ finished }) => {
            if (finished) setShownValue(latest.current);
        });
        return () => fadeOut.stop();
    }, [value, shownValue, opacity]);

    useEffect(() => {
        rise.setValue(reduceMotion ? 0 : RISE_PX);
        const fadeIn = Animated.parallel([
            Animated.timing(opacity, {
                duration: FADE_IN_MS,
                easing: Easing.out(Easing.quad),
                toValue: 1,
                useNativeDriver: true
            }),
            Animated.timing(rise, {
                duration: FADE_IN_MS,
                easing: Easing.out(Easing.quad),
                toValue: 0,
                useNativeDriver: true
            })
        ]);
        fadeIn.start();
        return () => fadeIn.stop();
    }, [shownValue, opacity, rise, reduceMotion]);

    return (
        <Animated.Text
            accessibilityLiveRegion={accessibilityLiveRegion}
            style={[style, { opacity, transform: [{ translateY: rise }] }]}
        >
            {shownValue}
        </Animated.Text>
    );
}
