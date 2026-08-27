import type { ApiClient } from '../../api/client/client.ts';

export type PlaybackPlatform = 'ios' | 'android';
export type PlayMethod = 'DirectPlay' | 'DirectStream' | 'Transcode';

export type PlaybackItem = {
    Id: string;
    Name: string;
};

export type MediaSource = {
    Id: string;
    Name?: string;
    MediaStreams?: unknown[];
    SupportsDirectPlay?: boolean;
    SupportsDirectStream?: boolean;
    SupportsTranscoding?: boolean;
    TranscodingSubProtocol?: string;
    TranscodingUrl?: string;
};

export type PlaybackInfo = {
    ErrorCode?: string;
    MediaSources?: MediaSource[];
    PlaySessionId?: string;
    PlaybackPipelineHandle?: unknown;
    PlaybackPipelineDecision?: unknown;
    PlaybackPipelineAudioStreamIndex?: unknown;
    PlaybackPipelineSubtitleStreamIndex?: unknown;
    PlaybackPipelineSourceCount?: unknown;
    PlaybackPipelineVideoDelivery?: unknown;
    PlaybackPipelineAudioDelivery?: unknown;
    PlaybackPipelineSourceWidth?: unknown;
    PlaybackPipelineSourceHeight?: unknown;
};

export type TrackOverride = {
    mediaSourceId: string;
    audioStreamIndex: number;
    subtitleStreamIndex: number;
};

export type PlaybackRequestPolicy = {
    enableDirectPlay: boolean;
    enableDirectStream: boolean;
    allowVideoStreamCopy: boolean;
    allowAudioStreamCopy: boolean;
};

export type PlaybackBaseRequest = PlaybackRequestPolicy & {
    itemId: string;
    userId: string;
    startTimeTicks: number;
    deviceProfile: unknown;
};

export type PlaybackRequest = PlaybackBaseRequest & {
    pipelineId: string;
    attemptId: string;
    rejectedSourceIds?: Set<string>;
    preferredMediaSourceId?: string;
    trackOverride?: TrackOverride;
};

export type ReleasePlaybackRequest = PlaybackRequest & {
    mediaSourceId: string;
    pipelineHandle: string;
    pipelineDecision: string;
    audioStreamIndex: number;
    subtitleStreamIndex: number;
};

export type PipelineProgressEvent = {
    type: string;
    [field: string]: unknown;
};

export type PlaybackAttempt = {
    attempt: number;
    attemptId: string;
    mediaSourceId: string | null;
};

export type PlaybackPipeline = {
    pipelineId: string;
    startAttempt(mediaSourceId: string | null): PlaybackAttempt;
    selectAttemptSource(mediaSourceId: string): PlaybackAttempt;
};

export type PlaybackTelemetry = {
    log(event: string, fields?: Record<string, unknown>): boolean;
    flush(): Promise<void>;
};

export type ProgressWatcher = (options: {
    client: ApiClient;
    pipelineId: string;
    attemptId: string;
    onProgress: (event: PipelineProgressEvent) => void;
}) => { stop(): Promise<void> };

export type PlaybackOptions = {
    client: ApiClient;
    item: PlaybackItem;
    platform: PlaybackPlatform;
    serverUrl: string;
    startTimeTicks: number;
    userId: string;
    createId?: () => string;
    excludedSourceIds?: Set<string>;
    preferredMediaSourceId?: string;
    trackOverride?: TrackOverride;
    onPipelineProgress?: (event: PipelineProgressEvent) => void;
    pipeline?: PlaybackPipeline;
    telemetry?: PlaybackTelemetry;
    watchProgress?: ProgressWatcher;
};

export type PlaybackResolution = {
    mediaSource: MediaSource;
    pipelineHandle: string;
    pipelineDecision: string;
    audioStreamIndex: number;
    subtitleStreamIndex: number;
    sourceCount: number;
    videoDelivery?: string;
    audioDelivery?: string;
    sourceWidth?: number;
    sourceHeight?: number;
};

export type VideoSource = {
    source: {
        uri: string;
        headers: Readonly<Record<string, string>>;
        contentType?: string;
    };
    playMethod: PlayMethod;
};

export type ReleasedPlayback = {
    mediaSource: MediaSource;
    playbackInfo: PlaybackInfo & { PlaySessionId: string };
    playMethod: PlayMethod;
    tracks: {
        audioStreamIndex: number;
        subtitleStreamIndex: number;
    };
    video: VideoSource;
};
