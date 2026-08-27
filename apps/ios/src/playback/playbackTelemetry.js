import { sanitizeTelemetryFields } from './telemetrySanitizer.js';

const DEFAULT_MAX_ATTEMPTS = 3;
const DEFAULT_QUEUE_LIMIT = 100;
const DEFAULT_RETRY_MS = 1_000;

function defaultWait(delay) {
    return new Promise((resolve) => setTimeout(resolve, delay));
}

export function createPlaybackTelemetry({
    client,
    pipeline,
    wait = defaultWait,
    retryMs = DEFAULT_RETRY_MS,
    maxAttempts = DEFAULT_MAX_ATTEMPTS,
    queueLimit = DEFAULT_QUEUE_LIMIT
}) {
    const queue = [];
    let drainPromise = null;
    let droppedCount = 0;

    async function drain() {
        while (queue.length > 0) {
            const entry = queue[0];
            try {
                await client.postNoContent('/ClientLog/PlaybackPipeline', entry.payload);
                queue.shift();
            } catch {
                entry.attempts += 1;
                if (entry.attempts >= maxAttempts) {
                    queue.shift();
                    droppedCount += 1;
                } else {
                    await wait(retryMs);
                }
            }
        }
    }

    function startDrain() {
        if (drainPromise) return;
        drainPromise = drain().finally(() => {
            drainPromise = null;
            if (queue.length > 0) startDrain();
        });
    }

    return {
        get droppedCount() {
            return droppedCount;
        },
        log(event, fields = {}) {
            if (queue.length >= queueLimit) {
                droppedCount += 1;
                return false;
            }
            const payload = sanitizeTelemetryFields(pipeline.nextEvent(event, {
                ...fields,
                telemetryDroppedCount: droppedCount
            }));
            queue.push({ payload, attempts: 0 });
            startDrain();
            return true;
        },
        flush() {
            return drainPromise ?? Promise.resolve();
        }
    };
}
