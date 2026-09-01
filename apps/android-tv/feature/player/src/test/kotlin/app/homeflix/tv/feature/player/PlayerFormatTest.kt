package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlayerFormatTest {
    @Test
    fun `should format time with hours`() {
        assertEquals("1:04:09", formatPlaybackTime(3849.7))
    }

    @Test
    fun `should format time under an hour`() {
        assertEquals("4:09", formatPlaybackTime(249.0))
        assertEquals("0:00", formatPlaybackTime(Double.NaN))
        assertEquals("0:00", formatPlaybackTime(-3.0))
    }

    @Test
    fun `should center stages that fit`() {
        val layout = pipelineStageLayout(viewportWidth = 800f, stageCount = 5)

        assertTrue(layout.centered)
        assertEquals(140f, layout.stageWidth)
    }

    @Test
    fun `should clamp narrow stages and scroll`() {
        val layout = pipelineStageLayout(viewportWidth = 300f, stageCount = 6)

        assertFalse(layout.centered)
        assertEquals(64f, layout.stageWidth)
    }

    @Test
    fun `should center empty stage row at maximum width`() {
        val layout = pipelineStageLayout(viewportWidth = 0f, stageCount = 0)

        assertTrue(layout.centered)
        assertEquals(140f, layout.stageWidth)
    }

    @Test
    fun `should escalate wait reassurance copy`() {
        assertNull(pipelineWaitReassurance(7_999))
        assertEquals("Still working on it", pipelineWaitReassurance(8_000))
        assertEquals("This can take a moment for large libraries", pipelineWaitReassurance(20_000))
    }

    @Test
    fun `should auto hide only while playing unpinned and shown`() {
        assertTrue(shouldScheduleAutoHide(PlaybackStatus.PLAYING, hidden = false, pinned = false))
        assertFalse(shouldScheduleAutoHide(PlaybackStatus.PLAYING, hidden = true, pinned = false))
        assertFalse(shouldScheduleAutoHide(PlaybackStatus.PLAYING, hidden = false, pinned = true))
        assertFalse(shouldScheduleAutoHide(PlaybackStatus.PAUSED, hidden = false, pinned = false))
    }
}
