package app.homeflix.tv.feature.player

import androidx.compose.ui.layout.ContentScale
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VideoContentModeTest {
    @Test
    fun `should preserve original aspect ratio in fit mode`() {
        assertEquals(ContentScale.Fit, VideoContentMode.FIT.contentScale)
        assertEquals("Fill screen", VideoContentMode.FIT.actionLabel)
    }

    @Test
    fun `should crop proportionally in fill mode`() {
        assertEquals(ContentScale.Crop, VideoContentMode.FILL.contentScale)
        assertEquals("Fit video", VideoContentMode.FILL.actionLabel)
    }

    @Test
    fun `should toggle between fit and fill modes`() {
        assertEquals(VideoContentMode.FILL, VideoContentMode.FIT.next())
        assertEquals(VideoContentMode.FIT, VideoContentMode.FILL.next())
    }
}
