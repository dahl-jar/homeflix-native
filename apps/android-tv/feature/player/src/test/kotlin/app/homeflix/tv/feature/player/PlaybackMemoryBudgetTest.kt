package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackMemoryBudgetTest {
    @Test
    fun `should reserve heap headroom with bounded player buffer`() {
        assertEquals(32 * 1_024 * 1_024, PlaybackMemoryBudget.TARGET_BUFFER_BYTES)
        assertTrue(PlaybackMemoryBudget.MAXIMUM_BUFFER_MS <= 30_000)
        assertTrue(PlaybackMemoryBudget.MINIMUM_BUFFER_MS < PlaybackMemoryBudget.MAXIMUM_BUFFER_MS)
    }
}
