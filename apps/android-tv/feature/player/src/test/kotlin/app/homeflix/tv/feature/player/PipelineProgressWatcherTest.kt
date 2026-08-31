package app.homeflix.tv.feature.player

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PipelineProgressWatcherTest {
    @Test
    fun `should apply only events after cursor in order`() =
        runTest {
            val gateway =
                QueuedProgressGateway(
                    listOf(
                        listOf(
                            eventDto(sequence = 2, stageId = "resolve"),
                            eventDto(sequence = 1, stageId = "sources"),
                        ),
                        listOf(eventDto(sequence = 2, stageId = "resolve"), eventDto(sequence = 3, stageId = "stream")),
                    ),
                )
            val seen = mutableListOf<PipelineEvent>()

            val handle = watchInto(gateway, seen)
            advanceTimeBy(POLL_ADVANCE_MILLIS)
            handle.stop()

            assertEquals(listOf("sources", "resolve", "stream"), stageIds(seen))
        }

    @Test
    fun `should drain once after stop`() =
        runTest {
            val gateway = QueuedProgressGateway(listOf(listOf(eventDto(sequence = 1, stageId = "sources"))))
            val seen = mutableListOf<PipelineEvent>()

            val handle = watchInto(gateway, seen)
            handle.stop()

            assertTrue(gateway.reads >= 1)
            assertEquals(listOf("sources"), stageIds(seen))
        }

    @Test
    fun `should keep polling through gateway failures`() =
        runTest {
            val gateway =
                QueuedProgressGateway(
                    listOf(null, listOf(eventDto(sequence = 1, stageId = "sources"))),
                )
            val seen = mutableListOf<PipelineEvent>()

            val handle = watchInto(gateway, seen)
            advanceTimeBy(POLL_ADVANCE_MILLIS)
            handle.stop()

            assertEquals(listOf("sources"), stageIds(seen))
        }

    @Test
    fun `should dwell after failed stage before next event`() =
        runTest {
            val gateway =
                QueuedProgressGateway(
                    listOf(
                        listOf(
                            eventDto(sequence = 1, stageId = "resolve", status = "failed"),
                            eventDto(sequence = 2, stageId = "retry"),
                        ),
                    ),
                )
            val timestamps = mutableListOf<Long>()
            val watcher = PipelineProgressWatcher(gateway, backgroundScope)

            val handle =
                watcher.watch(pipelineId = "native-abc", attemptId = "native-abc-a1") {
                    timestamps.add(testScheduler.currentTime)
                }
            advanceTimeBy(2_000)
            handle.stop()

            assertEquals(2, timestamps.size)
            assertTrue(timestamps[1] - timestamps[0] >= 400)
        }

    private fun TestScope.watchInto(
        gateway: QueuedProgressGateway,
        seen: MutableList<PipelineEvent>,
    ): ProgressWatchHandle =
        PipelineProgressWatcher(gateway, backgroundScope)
            .watch(pipelineId = "native-abc", attemptId = "native-abc-a1", onEvent = seen::add)

    private fun stageIds(seen: List<PipelineEvent>): List<String> =
        seen.filterIsInstance<PipelineEvent.StageProgress>().map(PipelineEvent.StageProgress::stageId)

    private fun eventDto(
        sequence: Long,
        stageId: String,
        status: String = "active",
    ): PipelineEventDto =
        PipelineEventDto(
            sequence = sequence,
            stageId = stageId,
            label = "Stage $stageId",
            order = sequence.toInt() * 100,
            status = status,
            sourceAttempt = null,
            sourceCount = null,
            reason = null,
        )
}

private const val POLL_ADVANCE_MILLIS = 500L

private class QueuedProgressGateway(
    responses: List<List<PipelineEventDto>?>,
) : PipelineProgressGateway {
    private val queue = ArrayDeque(responses)
    var reads = 0

    override suspend fun pipelineProgress(
        pipelineId: String,
        attemptId: String,
        afterSequence: Long,
    ): List<PipelineEventDto> {
        reads += 1
        val next = if (queue.isEmpty()) emptyList<PipelineEventDto>() else queue.removeFirst()
        return next ?: error("progress read failed")
    }
}
