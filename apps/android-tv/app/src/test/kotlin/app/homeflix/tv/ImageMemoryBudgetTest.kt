package app.homeflix.tv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImageMemoryBudgetTest {
    @Test
    fun `should bound image cache to ten percent of heap`() {
        assertEquals(0.10, ImageMemoryBudget.MEMORY_CACHE_PERCENT)
    }
}
