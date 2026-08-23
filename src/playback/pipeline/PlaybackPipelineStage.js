import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useEffect, useState } from 'react';
import { Animated, Easing, StyleSheet, Text, View } from 'react-native';

import { colors } from '../../theme/tokens.js';

import { PlaybackPipelineActiveArc } from './PlaybackPipelineActiveArc.js';
import { useReduceMotion } from './useReduceMotion.js';

const ENTER_MS = 260;
const ENTER_SCALE_FROM = 0.9;
const MARKER_SIZE = 28;
const CONNECTOR_GAP = 6;
const SWEEP_MS = 1400;
const SWEEP_WIDTH = 28;
const SWEEP_COLORS = [
    'rgba(255, 255, 255, 0)',
    'rgba(255, 255, 255, 0.75)',
    'rgba(255, 255, 255, 0)'
];

function StageMarker({ status }) {
    if (status === 'active') {
        return (
            <>
                <PlaybackPipelineActiveArc style={styles.arc} />
                <View style={styles.activeDot} />
            </>
        );
    }
    if (status === 'complete') {
        return <Ionicons color={colors.text} name="checkmark" size={15} />;
    }
    if (status === 'failed') {
        return <Ionicons color={colors.text} name="close" size={15} />;
    }
    return <View style={styles.pendingDot} />;
}

function ConnectorSweep({ width }) {
    const [travel] = useState(() => new Animated.Value(0));
    const reduceMotion = useReduceMotion();

    useEffect(() => {
        if (reduceMotion) {
            travel.stopAnimation();
            travel.setValue(0);
            return undefined;
        }
        const sweep = Animated.loop(Animated.timing(travel, {
            duration: SWEEP_MS,
            easing: Easing.inOut(Easing.quad),
            toValue: 1,
            useNativeDriver: true
        }));
        sweep.start();
        return () => sweep.stop();
    }, [reduceMotion, travel]);

    if (reduceMotion) return null;
    return (
        <View pointerEvents="none" style={styles.sweepTrack}>
            <Animated.View style={{
                transform: [{
                    translateX: travel.interpolate({
                        inputRange: [0, 1],
                        outputRange: [-SWEEP_WIDTH, width]
                    })
                }]
            }}>
                <LinearGradient
                    colors={SWEEP_COLORS}
                    end={{ x: 1, y: 0 }}
                    start={{ x: 0, y: 0 }}
                    style={styles.sweepLight}
                />
            </Animated.View>
        </View>
    );
}

export function PlaybackPipelineStage({ isLast, nextStatus, stage, width }) {
    const active = stage.status === 'active';
    const complete = stage.status === 'complete';
    const failed = stage.status === 'failed';
    const [enter] = useState(() => new Animated.Value(0));
    const reduceMotion = useReduceMotion();
    const connectorLength = Math.max(0, width - MARKER_SIZE - CONNECTOR_GAP * 2);

    useEffect(() => {
        if (reduceMotion) {
            enter.setValue(1);
            return undefined;
        }
        const appear = Animated.timing(enter, {
            duration: ENTER_MS,
            easing: Easing.out(Easing.quad),
            toValue: 1,
            useNativeDriver: true
        });
        appear.start();
        return () => appear.stop();
    }, [enter, reduceMotion]);

    return (
        <Animated.View
            accessibilityLabel={`${stage.label}: ${stage.status}`}
            style={[
                styles.stage,
                { width },
                {
                    opacity: enter,
                    transform: [{
                        scale: enter.interpolate({
                            inputRange: [0, 1],
                            outputRange: [ENTER_SCALE_FROM, 1]
                        })
                    }]
                }
            ]}
        >
            {!isLast ? (
                <View style={[
                    styles.connector,
                    {
                        left: width / 2 + MARKER_SIZE / 2 + CONNECTOR_GAP,
                        width: connectorLength
                    },
                    complete && styles.connectorSettled
                ]}>
                    {nextStatus === 'active' ? <ConnectorSweep width={connectorLength} /> : null}
                </View>
            ) : null}
            <View style={[
                styles.marker,
                active && styles.markerActive,
                complete && styles.markerComplete,
                failed && styles.markerFailed
            ]}>
                <StageMarker status={stage.status} />
            </View>
            <Text
                numberOfLines={2}
                style={[
                    styles.label,
                    active && styles.labelEmphasis,
                    failed && styles.labelFailed
                ]}
            >
                {stage.label}
            </Text>
        </Animated.View>
    );
}

const styles = StyleSheet.create({
    stage: {
        alignItems: 'center',
        flexShrink: 0
    },
    connector: {
        position: 'absolute',
        top: 13,
        height: 2,
        borderRadius: 1,
        overflow: 'hidden',
        backgroundColor: 'rgba(255, 255, 255, 0.16)'
    },
    connectorSettled: {
        backgroundColor: 'rgba(255, 255, 255, 0.35)'
    },
    sweepTrack: {
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%'
    },
    sweepLight: {
        width: SWEEP_WIDTH,
        height: 2
    },
    marker: {
        width: 28,
        height: 28,
        borderRadius: 14,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'rgb(20, 18, 19)',
        borderColor: 'rgba(255, 255, 255, 0.3)',
        borderWidth: 2
    },
    markerActive: {
        borderColor: 'transparent'
    },
    markerComplete: {
        borderColor: colors.glassBorder,
        backgroundColor: colors.bgRaised
    },
    markerFailed: {
        borderColor: colors.danger,
        backgroundColor: colors.danger
    },
    arc: {
        position: 'absolute'
    },
    pendingDot: {
        width: 6,
        height: 6,
        borderRadius: 3,
        backgroundColor: 'rgba(255, 255, 255, 0.45)'
    },
    activeDot: {
        width: 7,
        height: 7,
        borderRadius: 3.5,
        backgroundColor: colors.text
    },
    label: {
        minHeight: 32,
        marginTop: 9,
        paddingHorizontal: 2,
        alignSelf: 'stretch',
        color: 'rgba(238, 236, 235, 0.58)',
        fontSize: 11,
        fontWeight: '500',
        lineHeight: 15,
        textAlign: 'center'
    },
    labelEmphasis: {
        color: colors.text,
        fontWeight: '700'
    },
    labelFailed: {
        color: colors.danger,
        fontWeight: '700'
    }
});
