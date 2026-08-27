const SENSITIVE_FIELD = /(authorization|credential|header|password|path|token|uri|url)/i;
const URL_VALUE = /https?:\/\/\S+/gi;
const AUTHORIZATION_VALUE = /Authorization:\s*.*$/gi;
const CREDENTIAL_VALUE = /\b(api[_-]?key|credential|password|token)\s*[=:]\s*[^\s,;]+/gi;
const CONTROL_VALUE = /[\u0000-\u001f\u007f]+/g;

const TEXT_LIMITS = {
    event: 40,
    pipelineId: 64,
    attemptId: 96,
    previousAttemptId: 96,
    component: 32,
    itemId: 64,
    itemName: 256,
    mediaSourceId: 256,
    sourceName: 512,
    stage: 32,
    reason: 160,
    reasoning: 512,
    failureReason: 160,
    selectionReason: 160,
    originalLanguage: 16,
    audioTrack: 512,
    subtitleTrack: 512,
    playerEvent: 64,
    errorType: 128,
    errorName: 128,
    errorMessage: 512,
    playerName: 128,
    playMethod: 32,
    container: 64,
    protocol: 64,
    playbackMode: 64,
    recoveryAction: 64,
    videoDelivery: 16,
    audioDelivery: 16
};

const NUMBER_FIELDS = new Set([
    'sequence',
    'attempt',
    'sourceCount',
    'elapsedMs',
    'stageElapsedMs',
    'audioStreamIndex',
    'subtitleStreamIndex',
    'observedAudioStreamIndex',
    'observedSubtitleStreamIndex',
    'audioTrackCount',
    'subtitleTrackCount',
    'videoCurrentTime',
    'videoDuration',
    'videoWidth',
    'videoHeight',
    'sourceWidth',
    'sourceHeight',
    'videoPlaybackRate',
    'bufferedRangeCount',
    'bufferedStart',
    'bufferedEnd',
    'seekableRangeCount',
    'seekableStart',
    'seekableEnd',
    'telemetryDroppedCount',
    'errorCode',
    'httpStatus'
]);

const BOOLEAN_FIELDS = new Set([
    'videoPaused',
    'videoEnded',
    'videoSeeking',
    'videoSourcePresent',
    'playbackAdvanced',
    'userActivationActive'
]);

function sanitizeText(value, limit) {
    return value
        .replace(AUTHORIZATION_VALUE, 'Authorization: <redacted>')
        .replace(URL_VALUE, '<redacted>')
        .replace(CREDENTIAL_VALUE, '<redacted>')
        .replace(CONTROL_VALUE, ' ')
        .replace(/\s+/g, ' ')
        .trim()
        .slice(0, limit);
}

function sanitizeStringField(key, value) {
    const limit = TEXT_LIMITS[key];
    return limit === undefined ? undefined : sanitizeText(value, limit);
}

function sanitizeNumberField(key, value) {
    return NUMBER_FIELDS.has(key) && Number.isFinite(value) ? value : undefined;
}

function sanitizeBooleanField(key, value) {
    return BOOLEAN_FIELDS.has(key) ? value : undefined;
}

function sanitizeObjectField(key, value) {
    if (value !== null) return undefined;
    return key === 'attemptId' || key === 'mediaSourceId' ? null : undefined;
}

const VALUE_SANITIZERS = {
    string: sanitizeStringField,
    number: sanitizeNumberField,
    boolean: sanitizeBooleanField,
    object: sanitizeObjectField
};

function sanitizedValue(key, value) {
    if (SENSITIVE_FIELD.test(key)) return undefined;
    return VALUE_SANITIZERS[typeof value]?.(key, value);
}

export function sanitizeTelemetryFields(fields) {
    const sanitized = {};
    for (const [key, value] of Object.entries(fields ?? {})) {
        const safeValue = sanitizedValue(key, value);
        if (safeValue !== undefined) sanitized[key] = safeValue;
    }
    return sanitized;
}
