package app.homeflix.tv.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class HomeHeroMetadataTest {
    @Test
    fun `should format episode segments`() {
        val episode =
            PRIMARY_ITEM.copy(
                type = "Episode",
                parentIndexNumber = 1,
                indexNumber = 2,
                runTimeTicks = 41 * TICKS_PER_MINUTE,
                officialRating = "TV-14",
                communityRating = 7.4f,
            )

        val segments = HomeHeroMetadata.segments(episode, NOW)

        assertEquals(
            listOf("S1 E2", "2022", "41m", "TV-14", "7.4", "Ends at 12:07 PM"),
            segments,
        )
    }

    @Test
    fun `should format hour runtime`() {
        val movie = PRIMARY_ITEM.copy(runTimeTicks = 101 * TICKS_PER_MINUTE)

        val segments = HomeHeroMetadata.segments(movie, NOW)

        assertEquals(listOf("2022", "1h 41m", "Ends at 1:07 PM"), segments)
    }

    @Test
    fun `should show remaining only when started`() {
        val unstarted = PRIMARY_ITEM.copy(runTimeTicks = 41 * TICKS_PER_MINUTE, playbackPositionTicks = 0L)
        val started =
            PRIMARY_ITEM.copy(runTimeTicks = 41 * TICKS_PER_MINUTE, playbackPositionTicks = 9 * TICKS_PER_MINUTE)

        val unstartedSegments = HomeHeroMetadata.segments(unstarted, NOW)
        val startedSegments = HomeHeroMetadata.segments(started, NOW)

        assertEquals(listOf("2022", "41m", "Ends at 12:07 PM"), unstartedSegments)
        assertEquals(listOf("2022", "41m", "32m left", "Ends at 11:58 AM"), startedSegments)
    }

    @Test
    fun `should omit episode label without both indices`() {
        val missingEpisode = PRIMARY_ITEM.copy(type = "Episode", parentIndexNumber = 1, indexNumber = null)
        val missingSeason = PRIMARY_ITEM.copy(type = "Episode", parentIndexNumber = null, indexNumber = 2)

        assertEquals(listOf("2022"), HomeHeroMetadata.segments(missingEpisode, NOW))
        assertEquals(listOf("2022"), HomeHeroMetadata.segments(missingSeason, NOW))
    }

    @Test
    fun `should hide remaining when fully played`() {
        val finished =
            PRIMARY_ITEM.copy(runTimeTicks = 41 * TICKS_PER_MINUTE, playbackPositionTicks = 41 * TICKS_PER_MINUTE)

        val segments = HomeHeroMetadata.segments(finished, NOW)

        assertEquals(listOf("2022", "41m", "Ends at 12:07 PM"), segments)
    }

    @Test
    fun `should drop null segments`() {
        val bare = PRIMARY_ITEM.copy(year = null)

        val segments = HomeHeroMetadata.segments(bare, NOW)

        assertEquals(emptyList<String>(), segments)
    }
}

private val NOW: ZonedDateTime = ZonedDateTime.of(2026, 8, 30, 11, 26, 0, 0, ZoneOffset.UTC)
private const val TICKS_PER_MINUTE = 600_000_000L

private val PRIMARY_ITEM =
    HomeMediaItem(
        id = "item-one",
        name = "Item One",
        type = "Movie",
        seriesId = null,
        year = 2022,
        overview = null,
        genres = emptyList(),
        primaryImageUrl = null,
        backdropImageUrl = null,
        playedPercentage = null,
    )
