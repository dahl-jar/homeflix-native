import { getPlaybackProgress } from './playbackProgressApi.js';

const POLL_INTERVAL_MS = 120;
const FAILURE_TRANSITION_MS = 400;

function waitForPoll(signal) {
    return new Promise((resolve) => {
        const timer = setTimeout(resolve, POLL_INTERVAL_MS);
        signal.addEventListener('abort', () => {
            clearTimeout(timer);
            resolve();
        }, { once: true });
    });
}

function waitForFrame() {
    return new Promise((resolve) => requestAnimationFrame(() => resolve()));
}

function waitForFailureTransition(signal) {
    if (signal.aborted) return Promise.resolve();
    return new Promise((resolve) => {
        const finish = () => {
            clearTimeout(timer);
            signal.removeEventListener('abort', finish);
            resolve();
        };
        const timer = setTimeout(finish, FAILURE_TRANSITION_MS);
        signal.addEventListener('abort', finish, { once: true });
    });
}

function progressEvent(entry) {
    return {
        type: 'stage_progress',
        sequence: entry.Sequence,
        stageId: entry.StageId,
        label: entry.Label,
        order: entry.Order,
        status: entry.Status,
        sourceAttempt: entry.SourceAttempt,
        sourceCount: entry.SourceCount,
        reason: entry.Reason
    };
}

function retainedEvents(response, afterSequence) {
    if (!Array.isArray(response?.Events)) return [];
    return [...response.Events]
        .filter(({ Sequence }) => Number.isInteger(Sequence) && Sequence > afterSequence)
        .sort((left, right) => left.Sequence - right.Sequence);
}

export function watchPlaybackProgress({
    client,
    pipelineId,
    attemptId,
    onProgress,
    waitForFailureTransition: nextFailureTransition = waitForFailureTransition,
    waitForFrame: nextFrame = waitForFrame,
    waitForPoll: nextPoll = waitForPoll
}) {
    const controller = new AbortController();
    let afterSequence = 0;
    let stopping = false;
    let stopPromise = null;

    async function readAndApply() {
        const response = await getPlaybackProgress(client, {
            pipelineId,
            attemptId,
            afterSequence
        });
        for (const entry of retainedEvents(response, afterSequence)) {
            afterSequence = entry.Sequence;
            onProgress(progressEvent(entry));
            await nextFrame();
            if (entry.Status === 'failed') {
                await nextFailureTransition(controller.signal);
            }
        }
    }

    async function poll() {
        while (!stopping) {
            try {
                await readAndApply();
            } catch {}
            if (!stopping) await nextPoll(controller.signal);
        }
    }

    const polling = poll();

    return {
        stop() {
            if (stopPromise) return stopPromise;
            stopping = true;
            controller.abort();
            stopPromise = polling.then(async () => {
                try {
                    await readAndApply();
                } catch {}
            });
            return stopPromise;
        }
    };
}
