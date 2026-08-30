package app.homeflix.tv.core.designsystem

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TvFocusStyleTest {
    @Test
    fun `should resolve focus scale`() {
        assertEquals(1.0f, TvFocusStyle.scale(isFocused = false))
        assertEquals(1.12f, TvFocusStyle.scale(isFocused = true))
    }
}
