package app.homeflix.tv.feature.library

import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.network.RecordingJsonApiClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LibraryApiTest {
    @Test
    fun `should query items with filters`() =
        runBlocking {
            val client = LibraryRecordingClient()
            val api = LibraryApi(baseUrl = "http://server", client = client)
            val request =
                LibraryPageRequest(
                    libraryId = "movies-view",
                    selection = LibraryFilterSelection(genre = "Drama"),
                    startIndex = 100,
                    limit = 100,
                )

            val page = api.fetchPage(userId = "user-one", request = request)

            val recorded = client.requests.single()
            assertEquals("/Items", recorded.path)
            assertEquals(
                mapOf(
                    "userId" to "user-one",
                    "parentId" to "movies-view",
                    "recursive" to "true",
                    "includeItemTypes" to "Movie,Series",
                    "fields" to "PrimaryImageAspectRatio",
                    "startIndex" to "100",
                    "limit" to "100",
                    "sortBy" to "CommunityRating",
                    "sortOrder" to "Descending",
                    "genres" to "Drama",
                ),
                recorded.query,
            )
            assertEquals(listOf("movie-one", "movie-two"), page.items.map(MediaItem::id))
            assertEquals(214, page.totalRecordCount)
        }

    @Test
    fun `should parse filter options`() =
        runBlocking {
            val client = LibraryRecordingClient()
            val api = LibraryApi(baseUrl = "http://server", client = client)

            val options = api.fetchFilterOptions(userId = "user-one", libraryId = "movies-view")

            val recorded = client.requests.single()
            assertEquals("/Items/Filters", recorded.path)
            assertEquals(
                mapOf(
                    "userId" to "user-one",
                    "parentId" to "movies-view",
                    "includeItemTypes" to "Movie,Series",
                ),
                recorded.query,
            )
            assertEquals(listOf("Action", "Drama"), options.genres)
            assertEquals(listOf("2000s", "1990s"), options.decades.map(DecadeOption::label))
        }

    @Test
    fun `should map user views`() =
        runBlocking {
            val api = LibraryApi(baseUrl = "http://server", client = LibraryRecordingClient())

            val libraries = api.fetchLibraries(userId = "user-one")

            assertEquals(
                listOf(
                    LibrarySummary(id = "movies-view", name = "Movies", collectionType = "movies"),
                    LibrarySummary(id = "shows-view", name = "Shows", collectionType = "tvshows"),
                    LibrarySummary(id = "kids-view", name = "Kids", collectionType = null),
                ),
                libraries,
            )
        }
}

private class LibraryRecordingClient : RecordingJsonApiClient() {
    override fun respond(
        path: String,
        query: Map<String, String>,
    ): String = response(path)

    private fun response(path: String): String =
        when (path) {
            "/Items" ->
                """
                {"Items":[
                    {"Id":"movie-one","Name":"One"},
                    {"Id":"movie-two","Name":"Two"}
                ],"TotalRecordCount":214}
                """.trimIndent()

            "/Items/Filters" ->
                """{"Genres":["Action","Drama"],"Years":[1994,2003]}"""

            "/UserViews" ->
                """
                {"Items":[
                    {"Id":"movies-view","Name":"Movies","CollectionType":"movies"},
                    {"Id":"shows-view","Name":"Shows","CollectionType":"tvshows"},
                    {"Id":"kids-view","Name":"Kids"}
                ]}
                """.trimIndent()

            else -> error("unexpected request: $path")
        }
}
