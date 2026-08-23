const REASSURANCE_SLOW_MS = 8000;
const REASSURANCE_LONG_MS = 20000;
const SLOW_MESSAGE = 'Still working on it';
const LONG_MESSAGE = 'This can take a moment for large libraries';

export function pipelineWaitReassurance(elapsedMs) {
    if (elapsedMs >= REASSURANCE_LONG_MS) return LONG_MESSAGE;
    if (elapsedMs >= REASSURANCE_SLOW_MS) return SLOW_MESSAGE;
    return null;
}
