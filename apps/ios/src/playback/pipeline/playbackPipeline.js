const PIPELINE_ID_PATTERN = /^[A-Za-z0-9-]+$/;
const PIPELINE_ID_LIMIT = 64;
const ATTEMPT_ID_LIMIT = 96;

function requirePipelineId(value) {
    if (typeof value !== 'string' || value.length === 0 || value.length > PIPELINE_ID_LIMIT || !PIPELINE_ID_PATTERN.test(value)) {
        throw new Error('pipeline id must contain only letters, numbers, and hyphens');
    }
    return value;
}

export function createPlaybackPipeline({ item, now = Date.now, createId }) {
    const pipelineId = requirePipelineId(createId());
    const startedAt = now();
    let attempt = 0;
    let currentAttempt = null;
    let sequence = 0;

    const pipeline = {
        pipelineId,
        itemId: item.Id,
        itemName: item.Name,
        startedAt,
        startAttempt(mediaSourceId) {
            attempt += 1;
            const attemptId = `${pipelineId}-a${attempt}`;
            if (attemptId.length > ATTEMPT_ID_LIMIT) throw new Error('attempt id exceeds server limit');
            currentAttempt = { attempt, attemptId, mediaSourceId };
            return { ...currentAttempt };
        },
        selectAttemptSource(mediaSourceId) {
            if (!currentAttempt) throw new Error('playback attempt has not started');
            currentAttempt = { ...currentAttempt, mediaSourceId };
            return { ...currentAttempt };
        },
        nextEvent(event, fields = {}) {
            sequence += 1;
            return {
                ...fields,
                event,
                pipelineId,
                attemptId: currentAttempt?.attemptId ?? null,
                sequence,
                component: 'native',
                itemId: item.Id,
                itemName: item.Name,
                mediaSourceId: currentAttempt?.mediaSourceId ?? null,
                attempt: currentAttempt?.attempt ?? 0,
                elapsedMs: Math.max(0, now() - startedAt)
            };
        }
    };

    return pipeline;
}
