package app.homeflix.tv.feature.player

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackSessionReporterTest {
    private val gateway = RecordingSessionGateway()
    private var clock = 0L

    @Test
    fun `should report start once`() =
        runTest {
            val reporter = reporter()

            assertTrue(reporter.start(snapshot(positionTicks = 10)))
            assertFalse(reporter.start(snapshot(positionTicks = 10)))
            assertEquals(listOf("start"), gateway.calls.map(RecordedReport::kind))
        }

    @Test
    fun `should gate progress to interval`() =
        runTest {
            val reporter = reporter()
            reporter.start(snapshot(positionTicks = 0))

            clock = 4_000
            assertFalse(reporter.progress(snapshot(positionTicks = 1)))
            clock = 10_000
            assertTrue(reporter.progress(snapshot(positionTicks = 2)))

            assertEquals(listOf("start", "progress"), gateway.calls.map(RecordedReport::kind))
        }

    @Test
    fun `should force progress on demand`() =
        runTest {
            val reporter = reporter()
            reporter.start(snapshot(positionTicks = 0))

            clock = 1_000
            assertTrue(reporter.progress(snapshot(positionTicks = 1, isPaused = true), force = true))
        }

    @Test
    fun `should skip progress before start and after stop`() =
        runTest {
            val reporter = reporter()

            assertFalse(reporter.progress(snapshot(positionTicks = 1)))
            reporter.start(snapshot(positionTicks = 0))
            reporter.stop(snapshot(positionTicks = 5, failed = true))
            assertFalse(reporter.progress(snapshot(positionTicks = 6)))
        }

    @Test
    fun `should report stop once with failed flag`() =
        runTest {
            val reporter = reporter()
            reporter.start(snapshot(positionTicks = 0))

            assertTrue(reporter.stop(snapshot(positionTicks = 5, failed = true)))
            assertFalse(reporter.stop(snapshot(positionTicks = 5)))

            val stop = gateway.calls.last()
            assertEquals("stop", stop.kind)
            assertTrue(stop.snapshot.failed)
        }

    @Test
    fun `should swallow reporting failures`() =
        runTest {
            gateway.failing = true
            val reporter = reporter()

            assertFalse(reporter.start(snapshot(positionTicks = 0)))
        }

    private fun reporter(): PlaybackSessionReporter =
        PlaybackSessionReporter(
            gateway = gateway,
            context = sessionContext(),
            now = { clock },
        )

    private fun snapshot(
        positionTicks: Long,
        isPaused: Boolean = false,
        failed: Boolean = false,
    ): SessionSnapshot = SessionSnapshot(positionTicks = positionTicks, isPaused = isPaused, failed = failed)

    private fun sessionContext(): SessionContext =
        SessionContext(
            itemId = "item-1",
            mediaSourceId = "source-1",
            playSessionId = "session-1",
            pipelineId = "native-abc",
            attemptId = "native-abc-a1",
            playMethod = PlayMethod.DIRECT_PLAY,
            audioStreamIndex = 1,
            subtitleStreamIndex = -1,
        )
}

private data class RecordedReport(
    val kind: String,
    val snapshot: SessionSnapshot,
)

private class RecordingSessionGateway : SessionReportGateway {
    val calls = mutableListOf<RecordedReport>()
    var failing = false

    override suspend fun reportStart(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) = record("start", snapshot)

    override suspend fun reportProgress(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) = record("progress", snapshot)

    override suspend fun reportStop(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) = record("stop", snapshot)

    private fun record(
        kind: String,
        snapshot: SessionSnapshot,
    ) {
        if (failing) error("report rejected")
        calls.add(RecordedReport(kind, snapshot))
    }
}
