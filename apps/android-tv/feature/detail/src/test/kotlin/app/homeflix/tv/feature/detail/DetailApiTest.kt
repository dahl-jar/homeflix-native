package app.homeflix.tv.feature.detail

import app.homeflix.tv.core.network.JsonApiClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DetailApiTest {
    @Test
    fun `should parse detail with actor cast`() =
        runBlocking {
            val api = DetailApi(baseUrl = "http://server/", client = DetailRecordingClient())

            val detail = api.fetchDetail(userId = "user-one", itemId = "item-one")

            assertEquals("Item One", detail.item.name)
            assertEquals(2021, detail.item.year)
            assertEquals(listOf("Darrow"), detail.cast.map(CastMember::name))
            assertEquals(
                "http://server/Items/person-one/Images/Primary?tag=cast-tag&maxWidth=200&quality=90",
                detail.cast.first().imageUrl,
            )
        }

    @Test
    fun `should omit cast image without tag`() =
        runBlocking {
            val api = DetailApi(baseUrl = "http://server", client = DetailRecordingClient(castImageTag = null))

            val detail = api.fetchDetail(userId = "user-one", itemId = "item-one")

            assertEquals(null, detail.cast.first().imageUrl)
        }

    @Test
    fun `should parse seasons`() =
        runBlocking {
            val api = DetailApi(baseUrl = "http://server", client = DetailRecordingClient())

            val seasons = api.fetchSeasons(userId = "user-one", seriesId = "series-one")

            assertEquals(listOf("specials", "season-one"), seasons.map(DetailSeason::id))
            assertEquals(listOf(0, 1), seasons.map(DetailSeason::indexNumber))
        }

    @Test
    fun `should request episodes for season`() =
        runBlocking {
            val client = DetailRecordingClient()
            val api = DetailApi(baseUrl = "http://server", client = client)

            val episodes = api.fetchEpisodes(userId = "user-one", seriesId = "series-one", seasonId = "season-one")

            assertEquals(listOf("episode-one"), episodes.map { episode -> episode.id })
            val request = client.requests.last()
            assertEquals("/Shows/series-one/Episodes", request.path)
            assertEquals("season-one", request.query["seasonId"])
        }
}

private class DetailRecordingClient(
    private val castImageTag: String? = "cast-tag",
) : JsonApiClient {
    data class Request(
        val path: String,
        val query: Map<String, String>,
    )

    val requests = mutableListOf<Request>()

    override suspend fun get(path: String): String = get(path, emptyMap())

    override suspend fun get(
        path: String,
        query: Map<String, String>,
    ): String {
        requests += Request(path, query)
        return when {
            path == "/Users/user-one/Items/item-one" -> detailPayload()
            path == "/Shows/series-one/Seasons" -> seasonsPayload()
            path == "/Shows/series-one/Episodes" -> episodesPayload()
            path == "/Items/item-one/Similar" -> episodesPayload()
            else -> error("unexpected path $path")
        }
    }

    override suspend fun post(
        path: String,
        body: String,
    ): String = error("unused")

    private fun detailPayload(): String {
        val tagField = castImageTag?.let { tag -> """, "PrimaryImageTag": "$tag"""" } ?: ""
        return """
            {
                "Id": "item-one",
                "Name": "Item One",
                "Type": "Movie",
                "ProductionYear": 2021,
                "People": [
                    {"Id": "person-one", "Name": "Darrow", "Type": "Actor"$tagField},
                    {"Id": "person-two", "Name": "Goblin", "Type": "Director"}
                ]
            }
            """.trimIndent()
    }

    private fun seasonsPayload(): String =
        """
        {
            "Items": [
                {"Id": "specials", "Name": "Specials", "IndexNumber": 0},
                {"Id": "season-one", "Name": "Season 1", "IndexNumber": 1}
            ]
        }
        """.trimIndent()

    private fun episodesPayload(): String =
        """
        {
            "Items": [
                {"Id": "episode-one", "Name": "Episode One", "Type": "Episode", "IndexNumber": 1}
            ]
        }
        """.trimIndent()
}
