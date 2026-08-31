package app.homeflix.tv.feature.home

import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.network.RecordingJsonApiClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class HomeApiTest {
    @Test
    fun `should map home responses`() =
        runBlocking {
            val client = HomeRecordingClient()
            val api = HomeApi(baseUrl = "http://server", client = client)

            val content = api.fetchHome(userId = "user-one")

            assertEquals(listOf("featured-one", "featured-two"), content.featured.map(MediaItem::id))
            assertEquals(
                listOf("Continue watching", "Recently added in Movies", "Recently added in Shows"),
                content.rails.map(HomeRail::title),
            )
            assertEquals(HomeRailVariant.Poster, content.rails.first().variant)
            assertEquals(
                50f,
                content.rails
                    .first()
                    .items
                    .first()
                    .playedPercentage,
            )
            assertTrue(client.requests.any { request -> request.path == "/HomeFlix/Recommendations" })
            assertTrue(client.requests.any { request -> request.path == "/UserItems/Resume" })
            assertTrue(client.requests.any { request -> request.path == "/UserViews" })
            assertTrue(
                client.requests.any { request ->
                    request.path == "/Items" && request.query["parentId"] == "movies-view"
                },
            )
            assertTrue(
                client.requests.any { request ->
                    request.path == "/Users/user-one/Items/Latest" && request.query["parentId"] == "shows-view"
                },
            )
        }

    @Test
    fun `should retain rows after library failure`() =
        runBlocking {
            val api =
                HomeApi(
                    baseUrl = "http://server",
                    client = HomeRecordingClient(failedParentId = "shows-view"),
                )

            val content = api.fetchHome(userId = "user-one")

            assertEquals(
                listOf("Continue watching", "Recently added in Movies"),
                content.rails.map(HomeRail::title),
            )
        }

    @Test
    fun `should omit failed optional feeds`() =
        runBlocking {
            val api =
                HomeApi(
                    baseUrl = "http://server",
                    client = HomeRecordingClient(failedPaths = setOf("/HomeFlix/Recommendations", "/UserItems/Resume")),
                )

            val content = api.fetchHome(userId = "user-one")

            assertTrue(content.featured.isEmpty())
            assertEquals(
                listOf("Recently added in Movies", "Recently added in Shows"),
                content.rails.map(HomeRail::title),
            )
        }
}

private class HomeRecordingClient(
    private val failedParentId: String? = null,
    private val failedPaths: Set<String> = emptySet(),
) : RecordingJsonApiClient() {
    override fun respond(
        path: String,
        query: Map<String, String>,
    ): String {
        if (path in failedPaths || failedParentId != null && query["parentId"] == failedParentId) {
            throw IOException("unavailable")
        }
        return response(path, query)
    }

    private fun response(
        path: String,
        query: Map<String, String>,
    ): String =
        when {
            path == "/HomeFlix/Recommendations" ->
                """[{"ItemId":"featured-two","Rank":2},{"ItemId":"featured-one","Rank":1}]"""

            path == "/Items" && query.containsKey("ids") ->
                """{"Items":[${itemJson("featured-one")},${itemJson("featured-two")}]}"""

            path == "/UserItems/Resume" ->
                """{"Items":[${itemJson("resume-one", playedPercentage = 50f)}]}"""

            path == "/UserViews" ->
                """
                {"Items":[{"Id":"movies-view","Name":"Movies","CollectionType":"movies"},
                {"Id":"shows-view","Name":"Shows","CollectionType":"tvshows"}]}
                """.trimIndent()

            path == "/Items" && query["parentId"] == "movies-view" ->
                """{"Items":[${itemJson("movie-one")}]}"""

            path == "/Users/user-one/Items/Latest" && query["parentId"] == "shows-view" ->
                """[${itemJson("series-one", type = "Series")}]"""

            else -> error("unexpected request: $path $query")
        }
}

private fun itemJson(
    id: String,
    type: String = "Movie",
    playedPercentage: Float? = null,
): String {
    val userData = playedPercentage?.let { "\"UserData\":{\"PlayedPercentage\":$it}," }.orEmpty()
    return """
        {"Id":"$id","Name":"$id","Type":"$type",$userData
        "ProductionYear":2026,"Overview":"Overview","Genres":["Drama"],
        "ImageTags":{"Primary":"primary-$id"},"BackdropImageTags":["backdrop-$id"]}
        """.trimIndent()
}
