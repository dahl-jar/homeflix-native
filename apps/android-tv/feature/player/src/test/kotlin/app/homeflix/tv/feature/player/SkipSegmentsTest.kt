package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SkipSegmentsTest {
    @Test
    fun `should keep supported ordered segments`() {
        val segments =
            normalizeSegments(
                listOf(
                    SegmentDto(id = "outro", type = "Outro", startTicks = 300_000_000_000, endTicks = 320_000_000_000),
                    SegmentDto(id = "intro", type = "Intro", startTicks = 0, endTicks = 900_000_000),
                    SegmentDto(id = "ad", type = "Commercial", startTicks = 0, endTicks = 50_000_000_000),
                ),
            )

        assertEquals(listOf("intro", "outro"), segments.map(SkipSegment::id))
        assertEquals(SegmentType.INTRO, segments.first().type)
    }

    @Test
    fun `should drop segments shorter than one second or invalid`() {
        val segments =
            normalizeSegments(
                listOf(
                    SegmentDto(id = "blip", type = "Intro", startTicks = 5_000_000, endTicks = 14_000_000),
                    SegmentDto(id = "negative", type = "Intro", startTicks = -10, endTicks = 20_000_000_000),
                    SegmentDto(id = "missing-end", type = "Intro", startTicks = 0, endTicks = null),
                    SegmentDto(id = "", type = "Intro", startTicks = 0, endTicks = 20_000_000_000),
                ),
            )

        assertEquals(emptyList<SkipSegment>(), segments)
    }

    @Test
    fun `should keep recap segment lasting exactly one second`() {
        val segments =
            normalizeSegments(
                listOf(SegmentDto(id = "recap", type = "Recap", startTicks = 5_000_000, endTicks = 15_000_000)),
            )

        assertEquals(SegmentType.RECAP, segments.single().type)
    }

    @Test
    fun `should find active segment excluding dismissed`() {
        val segments =
            normalizeSegments(
                listOf(
                    SegmentDto(id = "intro", type = "Intro", startTicks = 0, endTicks = 900_000_000_000),
                ),
            )

        assertEquals("intro", activeSegment(segments, positionTicks = 0, dismissedIds = emptySet())?.id)
        assertNull(activeSegment(segments, positionTicks = 100, dismissedIds = setOf("intro")))
        assertNull(activeSegment(segments, positionTicks = 900_000_000_000, dismissedIds = emptySet()))
    }
}
