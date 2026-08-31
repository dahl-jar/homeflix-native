package app.homeflix.tv.feature.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private const val DEFAULT_MAX_ATTEMPTS = 3
private const val DEFAULT_QUEUE_LIMIT = 100
private const val DEFAULT_RETRY_MS = 1_000L

class PlaybackTelemetry(
    private val gateway: TelemetryGateway,
    private val pipeline: PlaybackPipelineIds,
    private val scope: CoroutineScope,
    private val retryMs: Long = DEFAULT_RETRY_MS,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val queueLimit: Int = DEFAULT_QUEUE_LIMIT,
) {
    private val queue = ArrayDeque<QueuedEvent>()
    private var drainJob: Job? = null

    var droppedCount = 0
        private set

    fun log(
        event: String,
        fields: Map<String, Any?> = emptyMap(),
    ): Boolean {
        if (queue.size >= queueLimit) {
            droppedCount += 1
            return false
        }
        val payload = pipeline.nextEvent(event, fields + ("telemetryDroppedCount" to droppedCount))
        queue.addLast(QueuedEvent(payload))
        startDrain()
        return true
    }

    suspend fun flush() {
        drainJob?.join()
    }

    private fun startDrain() {
        if (drainJob?.isActive == true) return
        drainJob =
            scope.launch {
                while (queue.isNotEmpty()) {
                    val entry = queue.first()
                    if (deliver(entry.payload)) {
                        queue.removeFirst()
                    } else {
                        entry.attempts += 1
                        if (entry.attempts >= maxAttempts) {
                            queue.removeFirst()
                            droppedCount += 1
                        } else {
                            delay(retryMs)
                        }
                    }
                }
            }
    }

    private suspend fun deliver(payload: JsonObject): Boolean =
        try {
            gateway.logPipelineEvent(payload)
            true
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            false
        }

    private class QueuedEvent(
        val payload: JsonObject,
    ) {
        var attempts = 0
    }
}
