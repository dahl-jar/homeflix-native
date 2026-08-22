const LEGAL_EVENTS = {
    idle: new Set(['LOAD']),
    loading: new Set(['SOURCE_READY', 'FAILED']),
    ready: new Set(['PLAYING', 'FAILED', 'RECOVERING']),
    playing: new Set(['PAUSED', 'ENDED', 'FAILED', 'RECOVERING']),
    paused: new Set(['PLAYING', 'ENDED', 'FAILED', 'RECOVERING']),
    recovering: new Set(['SOURCE_READY', 'PLAYING', 'FAILED']),
    ended: new Set(['LOAD']),
    failed: new Set(['LOAD'])
};

const EVENT_STATUS = {
    LOAD: 'loading',
    SOURCE_READY: 'ready',
    PLAYING: 'playing',
    PAUSED: 'paused',
    RECOVERING: 'recovering',
    ENDED: 'ended',
    FAILED: 'failed'
};

export function createPlaybackState() {
    return {
        status: 'idle',
        positionSeconds: 0,
        durationSeconds: 0,
        attemptId: null,
        reason: null
    };
}

export function reducePlaybackState(state, event) {
    if (event.type === 'TIME_UPDATED') {
        return {
            ...state,
            positionSeconds: event.positionSeconds,
            durationSeconds: event.durationSeconds
        };
    }
    if (!LEGAL_EVENTS[state.status]?.has(event.type)) return state;
    return {
        ...state,
        status: EVENT_STATUS[event.type],
        attemptId: event.attemptId ?? state.attemptId,
        reason: event.reason ?? null
    };
}
