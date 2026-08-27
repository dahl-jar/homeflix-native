const STAGE_STATUSES = new Set(['pending', 'active', 'complete', 'failed']);

export function createPlaybackProgress(attempt = 1, sourceOffset = 0) {
    return {
        attempt,
        sourceOffset,
        sourceAttempt: null,
        visible: true,
        videoVisible: false,
        reason: null,
        sourceCount: null,
        stages: []
    };
}

function validStageEvent(event) {
    return event.type === 'stage_progress'
        && typeof event.stageId === 'string'
        && event.stageId.length > 0
        && typeof event.label === 'string'
        && event.label.length > 0
        && Number.isFinite(event.order)
        && STAGE_STATUSES.has(event.status);
}

function orderedStages(stages) {
    return [...stages].sort((left, right) =>
        left.order - right.order || left.id.localeCompare(right.id)
    );
}

function applyStageProgress(progress, event) {
    if (!validStageEvent(event)) return progress;
    const nextStage = {
        id: event.stageId,
        label: event.label,
        order: event.order,
        status: event.status
    };
    const exists = progress.stages.some(({ id }) => id === event.stageId);
    const stages = exists
        ? progress.stages.map((stage) => stage.id === event.stageId ? nextStage : stage)
        : [...progress.stages, nextStage];
    return {
        ...progress,
        sourceAttempt: Number.isFinite(event.sourceAttempt)
            ? progress.sourceOffset + event.sourceAttempt
            : progress.sourceAttempt,
        sourceCount: event.sourceCount ?? progress.sourceCount,
        reason: event.status === 'failed' ? event.reason ?? 'source failed' : null,
        stages: orderedStages(stages)
    };
}

function startResolution(progress) {
    return createPlaybackProgress(progress.attempt, progress.sourceOffset);
}

function completeResolution(progress, event) {
    return {
        ...progress,
        sourceCount: event.sourceCount ?? progress.sourceCount,
        stages: progress.stages.map((stage) => ({ ...stage, status: 'complete' }))
    };
}

function releaseVideo(progress) {
    return { ...progress, videoVisible: true };
}

function completePlayback(progress) {
    return {
        ...progress,
        visible: false,
        videoVisible: true,
        stages: progress.stages.map((stage) => ({ ...stage, status: 'complete' }))
    };
}

function retryPlayback(progress) {
    return createPlaybackProgress(
        progress.attempt + 1,
        progress.sourceAttempt ?? progress.sourceOffset
    );
}

function failPlayback(progress, event) {
    const activeIndex = progress.stages.findIndex(({ status }) => status === 'active');
    const stages = activeIndex < 0
        ? progress.stages
        : progress.stages.map((stage, index) => index === activeIndex
            ? { ...stage, status: 'failed' }
            : stage
        );
    return {
        ...progress,
        visible: true,
        videoVisible: false,
        reason: event.reason ?? 'playback failed',
        stages
    };
}

const TRANSITIONS = Object.freeze({
    stage_progress: applyStageProgress,
    resolution_started: startResolution,
    resolution_completed: completeResolution,
    release_completed: releaseVideo,
    playing: completePlayback,
    retry: retryPlayback,
    failed: failPlayback
});

export function transitionPlaybackProgress(progress, event) {
    const transition = TRANSITIONS[event.type];
    return transition ? transition(progress, event) : progress;
}
