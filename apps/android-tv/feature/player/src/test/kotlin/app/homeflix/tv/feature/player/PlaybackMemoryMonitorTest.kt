package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaybackMemoryMonitorTest {
    @Test
    fun `should map bounded memory and playback state fields`() {
        val fields =
            playbackMemoryTelemetry(
                usage =
                    PlaybackMemoryUsage(
                        heapUsedBytes = 90_000_000,
                        heapMaxBytes = 192_000_000,
                        nativeHeapAllocatedBytes = 20_000_000,
                        totalPssKb = 140_000,
                    ),
                playbackStatus = PlaybackStatus.PLAYING,
                engineState = PlaybackEngineState.READY,
                positionSeconds = 600.0,
                bufferedSeconds = 630.0,
            )

        assertEquals(90_000_000L, fields.getValue("heapUsedBytes"))
        assertEquals(192_000_000L, fields.getValue("heapMaxBytes"))
        assertEquals(20_000_000L, fields.getValue("nativeHeapAllocatedBytes"))
        assertEquals(140_000L, fields.getValue("totalPssKb"))
        assertEquals("playing", fields.getValue("playbackStatus"))
        assertEquals("ready", fields.getValue("engineState"))
        assertEquals(30.0, fields.getValue("bufferedDurationSeconds"))
    }
}
