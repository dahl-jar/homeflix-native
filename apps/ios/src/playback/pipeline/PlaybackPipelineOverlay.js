import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useEffect, useRef, useState } from 'react';
import { Animated, Easing, Pressable, ScrollView, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { backdropUrl } from '../../api/images/imageUrl.js';
import { BackdropImage } from '../../components/BackdropImage/BackdropImage.js';
import { colors } from '../../theme/tokens.js';

import { CrossfadeText } from './CrossfadeText.js';
import { pipelineStageLayout } from './pipelineStageLayout.js';
import { pipelineWaitReassurance } from './pipelineWaitCopy.js';
import { PlaybackPipelineStage } from './PlaybackPipelineStage.js';
import { useReduceMotion } from './useReduceMotion.js';

const MAXIMUM_CONTENT_WIDTH = 820;
const LANDSCAPE_HORIZONTAL_PADDING = 80;
const PORTRAIT_HORIZONTAL_PADDING = 20;
const BLOCK_COUNT = 4;
const BLOCK_ENTER_MS = 450;
const BLOCK_STAGGER_MS = 60;
const BLOCK_RISE_PX = 8;
const REDUCED_ENTER_MS = 200;
const EXIT_MS = 300;
const BACKDROP_ZOOM_FROM = 1.05;
const BACKDROP_ZOOM_MS = 7000;
const WAIT_TICK_MS = 1000;

function progressMessage(progress) {
    if (progress.reason) return progress.reason;
    return progress.stages.find(({ status }) => status === 'active')?.label
        ?? 'Preparing playback';
}

function attemptLabel(progress) {
    if (progress.sourceAttempt) {
        const total = progress.sourceCount ? ` of ${progress.sourceCount}` : '';
        return `Source ${progress.sourceAttempt}${total}`;
    }
    return progress.attempt > 1 ? `Playback attempt ${progress.attempt}` : '';
}

const PRESENCE_SHOWN = 'shown';
const PRESENCE_EXITING = 'exiting';
const PRESENCE_HIDDEN = 'hidden';

function useOverlayPresence(visible, overlayOpacity) {
    const [phase, setPhase] = useState(visible ? PRESENCE_SHOWN : PRESENCE_HIDDEN);
    if (visible && phase !== PRESENCE_SHOWN) setPhase(PRESENCE_SHOWN);
    if (!visible && phase === PRESENCE_SHOWN) setPhase(PRESENCE_EXITING);

    useEffect(() => {
        if (phase !== PRESENCE_EXITING) return undefined;
        const exit = Animated.timing(overlayOpacity, {
            duration: EXIT_MS,
            toValue: 0,
            useNativeDriver: true
        });
        exit.start(({ finished }) => {
            if (finished) setPhase(PRESENCE_HIDDEN);
        });
        return () => exit.stop();
    }, [phase, overlayOpacity]);

    return phase !== PRESENCE_HIDDEN;
}

function useStepReassurance(progress, shown) {
    const activeStageId = progress.stages
        .find(({ status }) => status === 'active')?.id ?? null;
    const stepKey = `${activeStageId}:${progress.sourceAttempt}:${progress.attempt}`;
    const stepStartedAt = useRef(null);
    const [tick, setTick] = useState(null);

    useEffect(() => {
        stepStartedAt.current = Date.now();
    }, [stepKey]);

    useEffect(() => {
        if (!shown) return undefined;
        const interval = setInterval(() => {
            setTick({
                key: stepKey,
                message: pipelineWaitReassurance(Date.now() - stepStartedAt.current)
            });
        }, WAIT_TICK_MS);
        return () => clearInterval(interval);
    }, [shown, stepKey]);

    return tick?.key === stepKey ? tick.message : null;
}

function useEntrance(shown, reduceMotion, overlayOpacity) {
    const [backdropScale] = useState(() => new Animated.Value(1));
    const [blocks] = useState(() => Array.from(
        { length: BLOCK_COUNT },
        () => new Animated.Value(0)
    ));

    useEffect(() => {
        if (!shown) return undefined;
        if (reduceMotion) {
            overlayOpacity.setValue(0);
            backdropScale.setValue(1);
            blocks.forEach((block) => block.setValue(1));
            const fade = Animated.timing(overlayOpacity, {
                duration: REDUCED_ENTER_MS,
                toValue: 1,
                useNativeDriver: true
            });
            fade.start();
            return () => fade.stop();
        }
        overlayOpacity.setValue(1);
        backdropScale.setValue(BACKDROP_ZOOM_FROM);
        blocks.forEach((block) => block.setValue(0));
        const entrance = Animated.parallel([
            Animated.stagger(
                BLOCK_STAGGER_MS,
                blocks.map((block) => Animated.timing(block, {
                    duration: BLOCK_ENTER_MS,
                    easing: Easing.out(Easing.cubic),
                    toValue: 1,
                    useNativeDriver: true
                }))
            ),
            Animated.timing(backdropScale, {
                duration: BACKDROP_ZOOM_MS,
                easing: Easing.out(Easing.quad),
                toValue: 1,
                useNativeDriver: true
            })
        ]);
        entrance.start();
        return () => entrance.stop();
    }, [shown, reduceMotion, overlayOpacity, backdropScale, blocks]);

    const blockStyle = (index) => ({
        opacity: blocks[index],
        transform: [{
            translateY: blocks[index].interpolate({
                inputRange: [0, 1],
                outputRange: [BLOCK_RISE_PX, 0]
            })
        }]
    });
    return { backdropScale, blockStyle };
}

export function PlaybackPipelineOverlay({ item, onExit, progress, serverUrl }) {
    const insets = useSafeAreaInsets();
    const { height, width } = useWindowDimensions();
    const reduceMotion = useReduceMotion();
    const [overlayOpacity] = useState(() => new Animated.Value(0));
    const shown = useOverlayPresence(progress.visible, overlayOpacity);
    const { backdropScale, blockStyle } = useEntrance(shown, reduceMotion, overlayOpacity);
    const reassurance = useStepReassurance(progress, shown);
    const landscape = width > height;
    const horizontalPadding = landscape
        ? LANDSCAPE_HORIZONTAL_PADDING
        : PORTRAIT_HORIZONTAL_PADDING;
    const stageViewportWidth = Math.max(
        0,
        Math.min(width, MAXIMUM_CONTENT_WIDTH) - horizontalPadding * 2
    );
    const stageLayout = pipelineStageLayout(stageViewportWidth, progress.stages.length);
    if (!shown) return null;
    return (
        <Animated.View style={[StyleSheet.absoluteFill, { opacity: overlayOpacity }]}>
            <Animated.View style={[
                StyleSheet.absoluteFill,
                { transform: [{ scale: backdropScale }] }
            ]}>
                <BackdropImage
                    contentFit="cover"
                    style={StyleSheet.absoluteFill}
                    uri={backdropUrl(serverUrl, item, 1280)}
                />
            </Animated.View>
            <LinearGradient
                colors={[
                    'rgba(8, 7, 8, 0.7)',
                    'rgba(8, 7, 8, 0.82)',
                    'rgba(8, 7, 8, 0.96)'
                ]}
                locations={[0, 0.52, 1]}
                style={StyleSheet.absoluteFill}
            />
            <Pressable
                accessibilityLabel="Close player"
                accessibilityRole="button"
                hitSlop={10}
                onPress={onExit}
                style={[
                    styles.close,
                    {
                        left: Math.max(insets.left, 14),
                        top: landscape ? 12 : Math.max(insets.top, 18)
                    }
                ]}
            >
                <Ionicons color={colors.text} name="chevron-back" size={30} />
            </Pressable>
            <View style={[
                styles.content,
                landscape ? styles.contentLandscape : styles.contentPortrait
            ]}>
                <Animated.View style={blockStyle(0)}>
                    <Text style={styles.eyebrow}>PREPARING PLAYBACK</Text>
                </Animated.View>
                <Animated.View style={blockStyle(1)}>
                    <Text numberOfLines={2} style={styles.title}>{item.Name}</Text>
                </Animated.View>
                <Animated.View style={blockStyle(2)}>
                    <ScrollView
                        contentContainerStyle={[
                            styles.stages,
                            stageLayout.centered && styles.stagesCentered
                        ]}
                        horizontal
                        showsHorizontalScrollIndicator={false}
                        style={styles.stageScroller}
                    >
                        {progress.stages.map((stage, index) => (
                            <PlaybackPipelineStage
                                isLast={index === progress.stages.length - 1}
                                key={stage.id}
                                nextStatus={progress.stages[index + 1]?.status ?? null}
                                stage={stage}
                                width={stageLayout.stageWidth}
                            />
                        ))}
                    </ScrollView>
                </Animated.View>
                <Animated.View style={blockStyle(3)}>
                    <CrossfadeText
                        accessibilityLiveRegion="polite"
                        style={[
                            styles.message,
                            progress.reason && styles.failure
                        ]}
                        value={progressMessage(progress)}
                    />
                    <CrossfadeText style={styles.attempt} value={attemptLabel(progress)} />
                    <CrossfadeText style={styles.reassurance} value={reassurance ?? ''} />
                </Animated.View>
            </View>
        </Animated.View>
    );
}

const styles = StyleSheet.create({
    close: {
        position: 'absolute',
        zIndex: 2,
        width: 52,
        height: 52,
        borderRadius: 26,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'rgba(0, 0, 0, 0.42)',
        borderColor: colors.glassBorder,
        borderWidth: 1
    },
    content: {
        flex: 1,
        alignSelf: 'center',
        justifyContent: 'center',
        width: '100%',
        maxWidth: MAXIMUM_CONTENT_WIDTH
    },
    contentLandscape: {
        paddingHorizontal: LANDSCAPE_HORIZONTAL_PADDING,
        paddingVertical: 24
    },
    contentPortrait: {
        paddingHorizontal: PORTRAIT_HORIZONTAL_PADDING,
        paddingVertical: 96
    },
    eyebrow: {
        color: colors.textDim,
        fontSize: 11,
        fontWeight: '700',
        letterSpacing: 2.1,
        textAlign: 'center'
    },
    title: {
        marginTop: 10,
        color: colors.text,
        fontSize: 26,
        fontWeight: '800',
        textAlign: 'center',
        textShadowColor: 'rgba(0, 0, 0, 0.85)',
        textShadowRadius: 8
    },
    stageScroller: {
        width: '100%',
        height: 72,
        marginTop: 34,
        flexGrow: 0,
        flexShrink: 0
    },
    stages: {
        flexDirection: 'row'
    },
    stagesCentered: {
        flexGrow: 1,
        justifyContent: 'center'
    },
    message: {
        minHeight: 22,
        marginTop: 20,
        color: colors.text,
        fontSize: 15,
        fontWeight: '600',
        textAlign: 'center'
    },
    failure: {
        color: colors.danger
    },
    attempt: {
        minHeight: 17,
        marginTop: 4,
        color: colors.textDim,
        fontSize: 12,
        textAlign: 'center'
    },
    reassurance: {
        minHeight: 16,
        marginTop: 10,
        color: 'rgba(238, 236, 235, 0.5)',
        fontSize: 12,
        fontStyle: 'italic',
        textAlign: 'center'
    }
});
