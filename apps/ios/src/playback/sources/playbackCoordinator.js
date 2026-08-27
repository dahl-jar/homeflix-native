import { createNativeDeviceProfile } from '../device/deviceProfile.js';
import { createPlaybackPipeline } from '../pipeline/playbackPipeline.js';
import { createPlaybackSessionMonitor } from '../session-monitoring/playbackSessionMonitor.js';
import { createPlaybackTelemetry } from '../telemetry/playbackTelemetry.js';
import { playbackTrackMetadata } from '../tracks/playbackTrackMetadata.js';

import { resolvePlaybackAttemptWithProgress } from './playbackAttemptResolution.js';
import { createPlaybackRequestPolicy } from './playbackRequestPolicy.js';
import { releaseResolvedPlayback } from './playbackSourceRelease.js';

const TICKS_PER_SECOND = 10_000_000;

function reportProgress(options, type, fields = {}) {
    options.onPipelineProgress?.({ type, ...fields });
}

function reportStage(options, stageId, label, order, status) {
    reportProgress(options, 'stage_progress', {
        stageId,
        label,
        order,
        status
    });
}

function createPipelineId() {
    const timestamp = Date.now().toString(36);
    const random = Math.floor(Math.random() * Number.MAX_SAFE_INTEGER).toString(36);
    return `native-${timestamp}-${random}`;
}

function baseRequest(options) {
    return {
        itemId: options.item.Id,
        userId: options.userId,
        startTimeTicks: options.startTimeTicks,
        ...createPlaybackRequestPolicy(options.platform),
        deviceProfile: createNativeDeviceProfile(options.platform)
    };
}

function reporterContext(options) {
    return {
        itemId: options.item.Id,
        mediaSourceId: options.mediaSource.Id,
        playSessionId: options.playbackInfo.PlaySessionId,
        pipelineId: options.pipeline.pipelineId,
        attemptId: options.attempt.attemptId,
        playMethod: options.playMethod,
        audioStreamIndex: options.tracks.audioStreamIndex,
        subtitleStreamIndex: options.tracks.subtitleStreamIndex,
        videoDelivery: options.videoDelivery,
        audioDelivery: options.audioDelivery,
        sourceWidth: options.sourceWidth,
        sourceHeight: options.sourceHeight
    };
}

async function resolveAndRelease(options, pipeline, telemetry) {
    const attempt = pipeline.startAttempt(null);
    const request = {
        ...baseRequest(options),
        pipelineId: pipeline.pipelineId,
        attemptId: attempt.attemptId,
        rejectedSourceIds: options.excludedSourceIds,
        preferredMediaSourceId: options.trackOverride?.mediaSourceId
            ?? options.preferredMediaSourceId,
        trackOverride: options.trackOverride
    };
    reportProgress(options, 'resolution_started');
    const resolution = await resolvePlaybackAttemptWithProgress({
        attempt,
        options,
        pipeline,
        request,
        onProgress: (event) => reportProgress(options, event.type, event)
    });
    if (!resolution) return null;

    const selectedAttempt = pipeline.selectAttemptSource(resolution.mediaSource.Id);
    telemetry.log('sources_loaded', { sourceCount: resolution.sourceCount });
    telemetry.log('source_selected', {
        stage: 'server_resolved',
        sourceName: resolution.mediaSource.Name
    });
    telemetry.log('tracks_resolved', {
        reason: resolution.pipelineDecision,
        audioStreamIndex: resolution.audioStreamIndex,
        subtitleStreamIndex: resolution.subtitleStreamIndex
    });
    reportProgress(options, 'resolution_completed', {
        sourceCount: resolution.sourceCount
    });

    reportStage(options, 'stream', 'Preparing stream', 900, 'active');
    const released = await releaseResolvedPlayback({
        attempt: selectedAttempt,
        options,
        pipeline,
        request,
        resolution
    });
    reportStage(options, 'stream', 'Preparing stream', 900, 'complete');
    reportProgress(options, 'release_completed');
    const context = reporterContext({
        ...options,
        attempt: selectedAttempt,
        pipeline,
        videoDelivery: resolution.videoDelivery,
        audioDelivery: resolution.audioDelivery,
        sourceWidth: resolution.sourceWidth,
        sourceHeight: resolution.sourceHeight,
        ...released
    });
    telemetry.log('source_accepted', {
        playMethod: released.playMethod,
        selectionReason: 'server_resolved_source',
        videoDelivery: resolution.videoDelivery,
        audioDelivery: resolution.audioDelivery,
        sourceWidth: resolution.sourceWidth,
        sourceHeight: resolution.sourceHeight
    });
    return {
        video: released.video,
        context,
        sessionMonitor: createPlaybackSessionMonitor({ client: options.client, context, telemetry }),
        trackMetadata: playbackTrackMetadata(
            resolution.mediaSource,
            resolution.audioStreamIndex,
            resolution.subtitleStreamIndex
        ),
        attemptedSourceIds: [released.mediaSource.Id]
    };
}

export async function negotiatePlayback(options) {
    const pipeline = options.pipeline ?? createPlaybackPipeline({
        item: options.item,
        createId: options.createId ?? createPipelineId
    });
    const telemetry = options.telemetry ?? createPlaybackTelemetry({
        client: options.client,
        pipeline
    });
    const accepted = await resolveAndRelease(options, pipeline, telemetry);
    if (!accepted) {
        telemetry.log('playback_failed', { reason: 'no_compatible_source' });
        throw new Error('no compatible playback source');
    }
    return {
        ...accepted,
        pipeline,
        telemetry,
        startSeconds: options.startTimeTicks / TICKS_PER_SECOND
    };
}
