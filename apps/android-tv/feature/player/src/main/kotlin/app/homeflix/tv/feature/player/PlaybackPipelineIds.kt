package app.homeflix.tv.feature.player

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.random.Random

private val PIPELINE_ID_PATTERN = Regex("^[A-Za-z0-9-]+$")
private const val PIPELINE_ID_LIMIT = 64
private const val ATTEMPT_ID_LIMIT = 96
private const val ID_RADIX = 36

data class PlaybackAttempt(
    val attempt: Int,
    val attemptId: String,
    val mediaSourceId: String?,
)

fun defaultPipelineId(): String {
    val timestamp = System.currentTimeMillis().toString(ID_RADIX)
    val random = Random.nextLong(Long.MAX_VALUE).toString(ID_RADIX)
    return "native-$timestamp-$random"
}

class PlaybackPipelineIds(
    val itemId: String,
    private val itemName: String,
    val pipelineId: String,
    private val now: () -> Long,
) {
    private val startedAt: Long
    private var attemptCount = 0
    private var currentAttempt: PlaybackAttempt? = null
    private var sequence = 0L

    init {
        require(
            pipelineId.isNotEmpty() &&
                pipelineId.length <= PIPELINE_ID_LIMIT &&
                PIPELINE_ID_PATTERN.matches(pipelineId),
        ) { "pipeline id must contain only letters, numbers, and hyphens" }
        startedAt = now()
    }

    fun startAttempt(): PlaybackAttempt {
        attemptCount += 1
        val attemptId = "$pipelineId-a$attemptCount"
        check(attemptId.length <= ATTEMPT_ID_LIMIT) { "attempt id exceeds server limit" }
        val attempt = PlaybackAttempt(attempt = attemptCount, attemptId = attemptId, mediaSourceId = null)
        currentAttempt = attempt
        return attempt
    }

    fun selectAttemptSource(mediaSourceId: String): PlaybackAttempt {
        val attempt = checkNotNull(currentAttempt) { "playback attempt has not started" }
        val selected = attempt.copy(mediaSourceId = mediaSourceId)
        currentAttempt = selected
        return selected
    }

    fun nextEvent(
        event: String,
        fields: Map<String, Any?> = emptyMap(),
    ): JsonObject {
        sequence += 1
        val attempt = currentAttempt
        return buildJsonObject {
            fields.forEach { (key, value) -> put(key, telemetryPrimitive(value)) }
            put("event", event)
            put("pipelineId", pipelineId)
            attempt?.attemptId?.let { put("attemptId", it) }
            put("sequence", sequence)
            put("component", "native")
            put("itemId", itemId)
            put("itemName", itemName)
            attempt?.mediaSourceId?.let { put("mediaSourceId", it) }
            put("attempt", attempt?.attempt ?: 0)
            put("elapsedMs", (now() - startedAt).coerceAtLeast(0))
        }
    }
}

private fun telemetryPrimitive(value: Any?): JsonPrimitive =
    when (value) {
        null -> JsonPrimitive(null as String?)
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }
