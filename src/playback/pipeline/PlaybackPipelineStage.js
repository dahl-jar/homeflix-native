import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';

import { colors } from '../../theme/tokens.js';

import { PlaybackPipelineActiveHalo } from './PlaybackPipelineActiveHalo.js';

function StageMarker({ status }) {
    if (status === 'active') {
        return (
            <>
                <PlaybackPipelineActiveHalo />
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

export function PlaybackPipelineStage({ isLast, stage, width }) {
    const active = stage.status === 'active';
    const complete = stage.status === 'complete';
    const failed = stage.status === 'failed';
    return (
        <View
            accessibilityLabel={`${stage.label}: ${stage.status}`}
            style={[styles.stage, { width }]}
        >
            {!isLast ? (
                <View style={[
                    styles.connector,
                    complete && styles.connectorSettled
                ]} />
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
        </View>
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
        left: '50%',
        width: '100%',
        height: 2,
        backgroundColor: 'rgba(255, 255, 255, 0.2)'
    },
    connectorSettled: {
        backgroundColor: colors.success
    },
    marker: {
        width: 28,
        height: 28,
        borderRadius: 14,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'rgba(20, 18, 19, 0.88)',
        borderColor: 'rgba(255, 255, 255, 0.3)',
        borderWidth: 2
    },
    markerActive: {
        borderColor: colors.accent,
        backgroundColor: colors.accent
    },
    markerComplete: {
        borderColor: colors.success,
        backgroundColor: colors.success
    },
    markerFailed: {
        borderColor: colors.danger,
        backgroundColor: colors.danger
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
