package app.homeflix.tv.feature.home

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HomeContractTest {
    @Test
    fun `should drop HTTP recent items`() {
        val envelope =
            """
            {"Items":[
                {"Id":"local-one","Name":"Local","Path":"/media/local.mkv"},
                {"Id":"remote-one","Name":"Remote","Path":"http://remote/media.mkv"}
            ]}
            """.trimIndent()
        val latest =
            """
            [
                {"Id":"local-two","Name":"Local","Path":"/media/local.mkv"},
                {"Id":"remote-two","Name":"Remote","Path":"https://remote/media.mkv"}
            ]
            """.trimIndent()

        val movieIds = HomeContract.recentItems(Json, "http://server", envelope).map(HomeMediaItem::id)
        val latestIds = HomeContract.latestItems(Json, "http://server", latest).map(HomeMediaItem::id)

        assertEquals(listOf("local-one"), movieIds)
        assertEquals(listOf("local-two"), latestIds)
    }

    @Test
    fun `should map item artwork`() {
        val payload =
            """
            {"Items":[{
                "Id":"movie-one","Name":"Movie","Type":"Movie",
                "ImageTags":{"Primary":"primary-tag"},
                "BackdropImageTags":["backdrop-tag"]
            }]}
            """.trimIndent()

        val item = HomeContract.items(Json, "http://server/", payload).single()

        assertEquals(
            "http://server/Items/movie-one/Images/Primary?tag=primary-tag&maxWidth=440&quality=90",
            item.primaryImageUrl,
        )
        assertEquals(
            "http://server/Items/movie-one/Images/Backdrop/0?tag=backdrop-tag&maxWidth=1280&quality=90",
            item.backdropImageUrl,
        )
    }

    @Test
    fun `should use series artwork`() {
        val payload =
            """
            {"Items":[{
                "Id":"episode-one","Name":"Episode","Type":"Episode",
                "SeriesId":"series-one","SeriesPrimaryImageTag":"series-tag"
            }]}
            """.trimIndent()

        val item = HomeContract.items(Json, "http://server", payload).single()
        val seriesArtwork =
            "http://server/Items/series-one/Images/Primary?tag=series-tag&maxWidth=440&quality=90"

        assertEquals(seriesArtwork, item.primaryImageUrl)
        assertEquals(seriesArtwork, item.backdropImageUrl)
    }

    @Test
    fun `should map hero metadata fields`() {
        val payload =
            """
            {"Items":[{
                "Id":"episode-one","Name":"Winter","Type":"Episode",
                "SeriesName":"Item One","IndexNumber":2,"ParentIndexNumber":1,
                "RunTimeTicks":24600000000,"OfficialRating":"TV-14","CommunityRating":7.4,
                "UserData":{"PlaybackPositionTicks":5400000000,"PlayedPercentage":21.9}
            }]}
            """.trimIndent()

        val item = HomeContract.items(Json, "http://server", payload).single()

        assertEquals("Item One", item.seriesName)
        assertEquals(2, item.indexNumber)
        assertEquals(1, item.parentIndexNumber)
        assertEquals(24_600_000_000L, item.runTimeTicks)
        assertEquals(5_400_000_000L, item.playbackPositionTicks)
        assertEquals("TV-14", item.officialRating)
        assertEquals(7.4f, item.communityRating)
    }

    @Test
    fun `should omit missing artwork`() {
        val payload =
            """
            {"Items":[{
                "Id":"episode-one","Name":"Episode","Type":"Episode","SeriesId":"series-one"
            }]}
            """.trimIndent()

        val item = HomeContract.items(Json, "http://server", payload).single()

        assertNull(item.primaryImageUrl)
        assertNull(item.backdropImageUrl)
    }
}
