package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlaybackHealthMonitorTest {
    @Test
    fun `should fail when first frame misses startup deadline`() {
        val monitor = PlaybackHealthMonitor()
        monitor.onLoad(nowMs = 0)
        monitor.onState(PlaybackEngineState.BUFFERING, playWhenReady = true, nowMs = 0)

        assertEquals(PlaybackHealthFailure.STARTUP_TIMEOUT, monitor.evaluate(nowMs = 30_000))
    }

    @Test
    fun `should measure startup deadline from load time`() {
        val monitor = PlaybackHealthMonitor()
        monitor.onLoad(nowMs = 1_000)
        monitor.onState(PlaybackEngineState.BUFFERING, playWhenReady = true, nowMs = 1_000)

        assertNull(monitor.evaluate(nowMs = 29_000))
        assertEquals(PlaybackHealthFailure.STARTUP_TIMEOUT, monitor.evaluate(nowMs = 31_000))
    }

    @Test
    fun `should fail after sustained rebuffer without progress`() {
        val monitor = PlaybackHealthMonitor()
        monitor.onLoad(nowMs = 0)
        monitor.onFirstFrame(nowMs = 1_000)
        monitor.onPosition(positionSeconds = 120.0, nowMs = 2_000)
        monitor.onState(PlaybackEngineState.BUFFERING, playWhenReady = true, nowMs = 3_000)

        assertNull(monitor.evaluate(nowMs = 17_999))
        assertEquals(PlaybackHealthFailure.REBUFFER_TIMEOUT, monitor.evaluate(nowMs = 18_000))
    }

    @Test
    fun `should measure rebuffer from first frame when already buffering`() {
        val monitor = bufferingMonitor(firstFrameMs = 10_000)

        assertNull(monitor.evaluate(nowMs = 24_999))
        assertEquals(PlaybackHealthFailure.REBUFFER_TIMEOUT, monitor.evaluate(nowMs = 25_000))
    }

    @Test
    fun `should restart rebuffer deadline after playback progress`() {
        val monitor = bufferingMonitor()
        monitor.onPosition(positionSeconds = 10.0, nowMs = 10_000)

        assertNull(monitor.evaluate(nowMs = 24_999))
        assertEquals(PlaybackHealthFailure.REBUFFER_TIMEOUT, monitor.evaluate(nowMs = 25_000))
    }

    @Test
    fun `should retain rebuffer deadline for insignificant position change`() {
        val monitor = bufferingMonitor()
        monitor.onPosition(positionSeconds = 100.0, nowMs = 0)
        monitor.onPosition(positionSeconds = 99.95, nowMs = 10_000)

        assertEquals(PlaybackHealthFailure.REBUFFER_TIMEOUT, monitor.evaluate(nowMs = 15_000))
    }

    @Test
    fun `should retain rebuffer deadline at minimum progress boundary`() {
        val monitor = bufferingMonitor()
        monitor.onPosition(positionSeconds = 0.1, nowMs = 10_000)

        assertEquals(PlaybackHealthFailure.REBUFFER_TIMEOUT, monitor.evaluate(nowMs = 15_000))
    }

    @Test
    fun `should not treat paused buffering as a stall`() {
        val monitor = PlaybackHealthMonitor()
        monitor.onLoad(nowMs = 0)
        monitor.onFirstFrame(nowMs = 1_000)
        monitor.onState(PlaybackEngineState.BUFFERING, playWhenReady = false, nowMs = 2_000)

        assertNull(monitor.evaluate(nowMs = 60_000))
    }

    @Test
    fun `should reject materially early end`() {
        val monitor = PlaybackHealthMonitor()

        assertEquals(
            PlaybackHealthFailure.EARLY_END,
            monitor.onEnded(positionSeconds = 600.0, expectedDurationSeconds = 14_535.0),
        )
        assertNull(monitor.onEnded(positionSeconds = 14_520.0, expectedDurationSeconds = 14_535.0))
    }

    @Test
    fun `should accept end at exact tolerance boundary`() {
        val monitor = PlaybackHealthMonitor()

        assertNull(monitor.onEnded(positionSeconds = 970.0, expectedDurationSeconds = 1_000.0))
        assertEquals(
            PlaybackHealthFailure.EARLY_END,
            monitor.onEnded(positionSeconds = 969.9, expectedDurationSeconds = 1_000.0),
        )
    }

    @Test
    fun `should use fractional tolerance for long items`() {
        val monitor = PlaybackHealthMonitor()

        assertNull(monitor.onEnded(positionSeconds = 9_800.0, expectedDurationSeconds = 10_000.0))
        assertEquals(
            PlaybackHealthFailure.EARLY_END,
            monitor.onEnded(positionSeconds = 9_799.9, expectedDurationSeconds = 10_000.0),
        )
    }

    @Test
    fun `should ignore end when duration is unknown`() {
        val monitor = PlaybackHealthMonitor()

        assertNull(monitor.onEnded(positionSeconds = 0.0, expectedDurationSeconds = 0.0))
    }

    private fun bufferingMonitor(firstFrameMs: Long = 0): PlaybackHealthMonitor =
        PlaybackHealthMonitor().also { monitor ->
            monitor.onLoad(nowMs = 0)
            monitor.onState(PlaybackEngineState.BUFFERING, playWhenReady = true, nowMs = 0)
            monitor.onFirstFrame(nowMs = firstFrameMs)
        }
}
