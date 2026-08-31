package app.homeflix.tv.feature.detail

import app.homeflix.tv.core.catalog.MediaItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DetailFormatTest {
    @Test
    fun `should format runtime with hours`() {
        assertEquals("2h 14m", runtimeText(80_400_000_000))
    }

    @Test
    fun `should format runtime under an hour`() {
        assertEquals("54m", runtimeText(32_400_000_000))
    }

    @Test
    fun `should build chips from year runtime and rating`() {
        val full = mediaItem(year = 2021, runTimeTicks = 80_400_000_000, officialRating = "NO-15")

        assertEquals(listOf("2021", "2h 14m", "NO-15"), detailChips(full))
        assertEquals(listOf("2021"), detailChips(mediaItem(year = 2021)))
    }

    @Test
    fun `should format star text to one decimal`() {
        assertEquals("7.9", starText(mediaItem(communityRating = 7.94f)))
        assertEquals(null, starText(mediaItem()))
    }

    @Test
    fun `should pick first real season`() {
        val seasons =
            listOf(
                DetailSeason(id = "specials", name = "Specials", indexNumber = 0),
                DetailSeason(id = "season-one", name = "Season 1", indexNumber = 1),
            )

        assertEquals(1, defaultSeasonIndex(seasons))
        assertEquals(0, defaultSeasonIndex(seasons.take(1)))
    }

    @Test
    fun `should label resume when partly played`() {
        assertEquals("Resume", playLabel(mediaItem(playbackPositionTicks = 1)))
        assertEquals("Play", playLabel(mediaItem()))
    }
}

private fun mediaItem(
    year: Int? = null,
    runTimeTicks: Long? = null,
    officialRating: String? = null,
    communityRating: Float? = null,
    playbackPositionTicks: Long? = null,
): MediaItem =
    MediaItem(
        id = "item-one",
        name = "Item One",
        type = "Movie",
        seriesId = null,
        year = year,
        overview = null,
        genres = emptyList(),
        primaryImageUrl = null,
        backdropImageUrl = null,
        playedPercentage = null,
        runTimeTicks = runTimeTicks,
        playbackPositionTicks = playbackPositionTicks,
        officialRating = officialRating,
        communityRating = communityRating,
    )
