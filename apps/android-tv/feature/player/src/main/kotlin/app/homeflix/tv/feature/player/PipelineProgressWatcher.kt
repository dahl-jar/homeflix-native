package app.homeflix.tv.feature.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 120L
private const val FAILURE_TRANSITION_MS = 400L

interface ProgressWatchHandle {
    suspend fun stop()
}

interface ProgressWatcherFactory {
    fun watch(
        pipelineId: String,
        attemptId: String,
        onEvent: (PipelineEvent) -> Unit,
    ): ProgressWatchHandle
}

class PipelineProgressWatcher(
    private val gateway: PipelineProgressGateway,
    private val scope: CoroutineScope,
    private val pollIntervalMs: Long = POLL_INTERVAL_MS,
    private val failureDwellMs: Long = FAILURE_TRANSITION_MS,
) : ProgressWatcherFactory {
    override fun watch(
        pipelineId: String,
        attemptId: String,
        onEvent: (PipelineEvent) -> Unit,
    ): ProgressWatchHandle {
        val cursor = Cursor()

        suspend fun readAndApply(dwellOnFailure: Boolean) {
            val events =
                gateway
                    .pipelineProgress(pipelineId, attemptId, cursor.afterSequence)
                    .filter { it.sequence > cursor.afterSequence }
                    .sortedBy(PipelineEventDto::sequence)
            for (dto in events) {
                cursor.afterSequence = dto.sequence
                val event = dto.toStageProgress()
                if (event != null) {
                    onEvent(event)
                    if (dwellOnFailure && event.status == StageStatus.FAILED) delay(failureDwellMs)
                }
            }
        }

        val polling: Job =
            scope.launch {
                while (true) {
                    try {
                        readAndApply(dwellOnFailure = true)
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                        continueAfterFailedPoll()
                    }
                    delay(pollIntervalMs)
                }
            }

        return object : ProgressWatchHandle {
            override suspend fun stop() {
                polling.cancel()
                try {
                    readAndApply(dwellOnFailure = false)
                } catch (failure: CancellationException) {
                    throw failure
                } catch (_: Exception) {
                    continueAfterFailedPoll()
                }
            }
        }
    }

    private fun continueAfterFailedPoll() = Unit

    private class Cursor {
        var afterSequence = 0L
    }
}

private fun PipelineEventDto.toStageProgress(): PipelineEvent.StageProgress? {
    val id = stageId?.takeIf(String::isNotEmpty)
    val text = label?.takeIf(String::isNotEmpty)
    val stageStatus = stageStatus(status)
    return when {
        id == null || text == null || stageStatus == null -> null
        else ->
            PipelineEvent.StageProgress(
                stageId = id,
                label = text,
                order = order ?: 0,
                status = stageStatus,
                sourceAttempt = sourceAttempt,
                sourceCount = sourceCount,
                reason = reason,
            )
    }
}

private fun stageStatus(value: String?): StageStatus? =
    when (value) {
        "pending" -> StageStatus.PENDING
        "active" -> StageStatus.ACTIVE
        "complete" -> StageStatus.COMPLETE
        "failed" -> StageStatus.FAILED
        else -> null
    }
