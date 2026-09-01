package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlaybackPositionTrackerTest {
    @Test
    fun `should retain confirmed position after unexplained backward jump`() {
        val tracker = PlaybackPositionTracker(startPositionSeconds = 11_557.0)

        tracker.update(12_157.0)
        val decision = tracker.update(0.0)

        assertFalse(decision.accepted)
        assertEquals(12_157.0, decision.positionSeconds)
        assertEquals(12_157.0, tracker.confirmedPositionSeconds)
    }

    @Test
    fun `should accept intentional backward seek`() {
        val tracker = PlaybackPositionTracker(startPositionSeconds = 600.0)

        tracker.armUserSeek(300.0)
        val decision = tracker.update(300.5)

        assertTrue(decision.accepted)
        assertEquals(300.5, decision.positionSeconds)
    }

    @Test
    fun `should accept small position jitter within tolerance`() {
        val tracker = PlaybackPositionTracker(startPositionSeconds = 600.0)

        val decision = tracker.update(597.0)

        assertTrue(decision.accepted)
        assertEquals(597.0, decision.positionSeconds)
    }
}
