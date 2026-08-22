import { useEffect, useState } from 'react';
import { AccessibilityInfo, Animated, StyleSheet } from 'react-native';

import { colors } from '../../theme/tokens.js';

export function PlaybackPipelineActiveHalo() {
    const [animation] = useState(() => new Animated.Value(0));
    const [reduceMotion, setReduceMotion] = useState(false);

    useEffect(() => {
        let mounted = true;
        AccessibilityInfo.isReduceMotionEnabled().then((enabled) => {
            if (mounted) setReduceMotion(enabled);
        });
        const subscription = AccessibilityInfo.addEventListener(
            'reduceMotionChanged',
            setReduceMotion
        );
        return () => {
            mounted = false;
            subscription.remove();
        };
    }, []);

    useEffect(() => {
        if (reduceMotion) {
            animation.stopAnimation();
            animation.setValue(0);
            return undefined;
        }
        const pulse = Animated.loop(Animated.timing(animation, {
            duration: 1200,
            toValue: 1,
            useNativeDriver: true
        }));
        pulse.start();
        return () => pulse.stop();
    }, [animation, reduceMotion]);

    return (
        <Animated.View
            pointerEvents="none"
            style={[
                styles.halo,
                {
                    opacity: animation.interpolate({
                        inputRange: [0, 1],
                        outputRange: [0.5, 0]
                    }),
                    transform: [{
                        scale: animation.interpolate({
                            inputRange: [0, 1],
                            outputRange: [1, 1.65]
                        })
                    }]
                }
            ]}
        />
    );
}

const styles = StyleSheet.create({
    halo: {
        position: 'absolute',
        width: 28,
        height: 28,
        borderRadius: 14,
        borderColor: colors.accent,
        borderWidth: 2
    }
});
