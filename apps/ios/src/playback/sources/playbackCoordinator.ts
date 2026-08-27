import type { ApiClient } from '../../api/client/client.ts';
import { createNativeDeviceProfile } from '../device/deviceProfile.js';
import { createPlaybackPipeline } from '../pipeline/playbackPipeline.js';
import { createPlaybackSessionMonitor } from '../session-monitoring/playbackSessionMonitor.js';
import { createPlaybackTelemetry } from '../telemetry/playbackTelemetry.js';
import { playbackTrackMetadata } from '../tracks/playbackTrackMetadata.js';

import { resolvePlaybackAttemptWithProgress } from './playbackAttemptResolution.ts';
import { createPlaybackRequestPolicy } from './playbackRequestPolicy.ts';
import { releaseResolvedPlayback } from './playbackSourceRelease.ts';
import type {
    MediaSource,
    PlaybackAttempt,
    PlaybackBaseRequest,
    PlaybackOptions,
    PlaybackPipeline,
    PlaybackRequest,
    PlaybackTelemetry,
    PipelineProgressEvent,
    PlayMethod,
    ReleasedPlayback
} from './playbackTypes.ts';

const TICKS_PER_SECOND = 10_000_000;

type PlaybackSessionMonitorFactory = (options: {
    client: ApiClient;
    context: PlaybackReporterContext;
    telemetry: PlaybackTelemetry;
}) => ReturnType<typeof createPlaybackSessionMonitor>;

const createSessionMonitor = createPlaybackSessionMonitor as unknown as PlaybackSessionMonitorFactory;

export type PlaybackReporterContext = {
    itemId: string;
    mediaSourceId: string;
    playSessionId: string;
    pipelineId: string;
    attemptId: string;
    playMethod: PlayMethod;
    audioStreamIndex: number;
    subtitleStreamIndex: number;
    videoDelivery?: string;
    audioDelivery?: string;
    sourceWidth?: number;
    sourceHeight?: number;
};

type ReporterContextInput = {
    item: PlaybackOptions['item'];
    mediaSource: MediaSource;
    playbackInfo: { PlaySessionId: string };
    pipeline: PlaybackPipeline;
    attempt: PlaybackAttempt;
    playMethod: PlayMethod;
    tracks: ReleasedPlayback['tracks'];
    videoDelivery?: string;
    audioDelivery?: string;
    sourceWidth?: number;
    sourceHeight?: number;
};

function reportProgress(
    options: PlaybackOptions,
    type: string,
    fields: Record<string, unknown> = {}
) {
    options.onPipelineProgress?.({ type, ...fields });
}

function reportStage(
    options: PlaybackOptions,
    stageId: string,
    label: string,
    order: number,
    status: string
) {
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

function baseRequest(options: PlaybackOptions): PlaybackBaseRequest {
    return {
        itemId: options.item.Id,
        userId: options.userId,
        startTimeTicks: options.startTimeTicks,
        ...createPlaybackRequestPolicy(options.platform),
        deviceProfile: createNativeDeviceProfile(options.platform)
    };
}

function reporterContext(options: ReporterContextInput): PlaybackReporterContext {
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

async function resolveAndRelease(
    options: PlaybackOptions,
    pipeline: PlaybackPipeline,
    telemetry: PlaybackTelemetry
) {
    const attempt = pipeline.startAttempt(null);
    const request: PlaybackRequest = {
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
        onProgress: (event: PipelineProgressEvent) => reportProgress(options, event.type, event)
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
        item: options.item,
        mediaSource: released.mediaSource,
        playbackInfo: released.playbackInfo,
        pipeline,
        attempt: selectedAttempt,
        playMethod: released.playMethod,
        tracks: released.tracks,
        videoDelivery: resolution.videoDelivery,
        audioDelivery: resolution.audioDelivery,
        sourceWidth: resolution.sourceWidth,
        sourceHeight: resolution.sourceHeight
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
        sessionMonitor: createSessionMonitor({ client: options.client, context, telemetry }),
        trackMetadata: playbackTrackMetadata(
            resolution.mediaSource,
            resolution.audioStreamIndex,
            resolution.subtitleStreamIndex
        ),
        attemptedSourceIds: [released.mediaSource.Id]
    };
}

export async function negotiatePlayback(options: PlaybackOptions) {
    const pipeline = (options.pipeline ?? createPlaybackPipeline({
        item: options.item,
        createId: options.createId ?? createPipelineId
    })) as PlaybackPipeline;
    const telemetry = (options.telemetry ?? createPlaybackTelemetry({
        client: options.client,
        pipeline
    })) as PlaybackTelemetry;
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
