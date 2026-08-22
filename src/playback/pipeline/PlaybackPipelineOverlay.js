import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, ScrollView, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { backdropUrl } from '../../api/imageUrl.js';
import { BackdropImage } from '../../components/BackdropImage.js';
import { colors } from '../../theme/tokens.js';

import { pipelineStageLayout } from './pipelineStageLayout.js';
import { PlaybackPipelineStage } from './PlaybackPipelineStage.js';

const MAXIMUM_CONTENT_WIDTH = 820;
const LANDSCAPE_HORIZONTAL_PADDING = 80;
const PORTRAIT_HORIZONTAL_PADDING = 20;

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
    return progress.attempt > 1 ? `Playback attempt ${progress.attempt}` : null;
}

export function PlaybackPipelineOverlay({ item, onExit, progress, serverUrl }) {
    const insets = useSafeAreaInsets();
    const { height, width } = useWindowDimensions();
    const landscape = width > height;
    const horizontalPadding = landscape
        ? LANDSCAPE_HORIZONTAL_PADDING
        : PORTRAIT_HORIZONTAL_PADDING;
    const stageViewportWidth = Math.max(
        0,
        Math.min(width, MAXIMUM_CONTENT_WIDTH) - horizontalPadding * 2
    );
    const stageLayout = pipelineStageLayout(stageViewportWidth, progress.stages.length);
    const attempt = attemptLabel(progress);
    if (!progress.visible) return null;
    return (
        <View style={StyleSheet.absoluteFill}>
            <BackdropImage
                contentFit="cover"
                style={StyleSheet.absoluteFill}
                uri={backdropUrl(serverUrl, item, 1280)}
            />
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
                <Text style={styles.eyebrow}>PREPARING PLAYBACK</Text>
                <Text numberOfLines={2} style={styles.title}>{item.Name}</Text>
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
                            stage={stage}
                            width={stageLayout.stageWidth}
                        />
                    ))}
                </ScrollView>
                <Text accessibilityLiveRegion="polite" style={[
                    styles.message,
                    progress.reason && styles.failure
                ]}>
                    {progressMessage(progress)}
                </Text>
                {attempt ? <Text style={styles.attempt}>{attempt}</Text> : null}
            </View>
        </View>
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
    }
});
