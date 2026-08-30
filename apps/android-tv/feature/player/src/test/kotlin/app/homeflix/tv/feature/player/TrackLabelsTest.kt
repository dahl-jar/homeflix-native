package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrackLabelsTest {
    @Test
    fun `should label audio with language and layout`() {
        val labels =
            playbackTrackLabels(
                listOf(
                    stream(language = "eng", channels = 6),
                    stream(language = "nor", channels = 2),
                ),
                TrackKind.AUDIO,
            )

        assertEquals(listOf("English · 5.1", "Norwegian · Stereo"), labels)
    }

    @Test
    fun `should label subtitles with qualifiers`() {
        val labels =
            playbackTrackLabels(
                listOf(
                    stream(language = "en", title = "English SDH"),
                    stream(language = "en", isForced = true),
                ),
                TrackKind.SUBTITLE,
            )

        assertEquals(listOf("English · SDH", "English · Forced"), labels)
    }

    @Test
    fun `should disambiguate duplicates with delivery then counter`() {
        val labels =
            playbackTrackLabels(
                listOf(
                    stream(language = "en", isExternal = true),
                    stream(language = "en", isExternal = false),
                    stream(language = "en", isExternal = false),
                ),
                TrackKind.SUBTITLE,
            )

        assertEquals(listOf("English · External", "English · Embedded 1", "English · Embedded 2"), labels)
    }

    @Test
    fun `should fall back to language found in title`() {
        val labels = playbackTrackLabels(listOf(stream(language = null, title = "Japanese 5.1")), TrackKind.AUDIO)

        assertEquals(listOf("Japanese"), labels)
    }

    @Test
    fun `should exclude commentary and signs tracks`() {
        assertFalse(isSelectableTrack(stream(title = "Director Commentary"), TrackKind.AUDIO))
        assertFalse(isSelectableTrack(stream(title = "Signs & Songs"), TrackKind.SUBTITLE))
        assertTrue(isSelectableTrack(stream(title = "Signs & Songs"), TrackKind.AUDIO))
        assertTrue(isSelectableTrack(stream(language = "eng"), TrackKind.SUBTITLE))
    }

    private fun stream(
        language: String? = null,
        title: String? = null,
        channels: Int? = null,
        isForced: Boolean = false,
        isExternal: Boolean? = null,
    ): MediaStreamDto =
        MediaStreamDto(
            index = 0,
            type = "Audio",
            language = language,
            displayTitle = null,
            title = title,
            channels = channels,
            isForced = isForced,
            isHearingImpaired = false,
            isExternal = isExternal,
        )
}
