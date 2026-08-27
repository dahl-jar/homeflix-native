import { useEffect, useState } from 'react';
import { Animated, Easing } from 'react-native';

import { colors } from '../../theme/tokens.js';

import { useReduceMotion } from './useReduceMotion.js';

const ROTATION_MS = 1100;
const DEFAULT_SIZE = 28;
const RING_WIDTH = 2;
const TRACK_COLOR = 'rgba(255, 255, 255, 0.22)';

export function PlaybackPipelineActiveArc({ size = DEFAULT_SIZE, style }) {
    const [rotation] = useState(() => new Animated.Value(0));
    const reduceMotion = useReduceMotion();

    useEffect(() => {
        if (reduceMotion) {
            rotation.stopAnimation();
            rotation.setValue(0);
            return undefined;
        }
        const spin = Animated.loop(Animated.timing(rotation, {
            duration: ROTATION_MS,
            easing: Easing.linear,
            toValue: 1,
            useNativeDriver: true
        }));
        spin.start();
        return () => spin.stop();
    }, [reduceMotion, rotation]);

    return (
        <Animated.View
            pointerEvents="none"
            style={[
                style,
                {
                    width: size,
                    height: size,
                    borderRadius: size / 2,
                    borderWidth: RING_WIDTH,
                    borderColor: reduceMotion ? colors.accent : TRACK_COLOR,
                    borderTopColor: colors.accent,
                    transform: [{
                        rotate: rotation.interpolate({
                            inputRange: [0, 1],
                            outputRange: ['0deg', '360deg']
                        })
                    }]
                }
            ]}
        />
    );
}
