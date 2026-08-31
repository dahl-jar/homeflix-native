package app.homeflix.tv.feature.library

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.catalog.MediaPage
import app.homeflix.tv.core.designsystem.HomeflixTheme
import app.homeflix.tv.core.designsystem.TvNavProfile
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldRenderGridItems() {
        composeRule.setContent {
            HomeflixTheme {
                libraryScreen(FakeLibraryGateway())
            }
        }

        composeRule.onNodeWithText("Movies").assertExists()
        composeRule.onNodeWithText("2").assertExists()
        composeRule.onNodeWithContentDescription("One card").assertExists()
        composeRule.onNodeWithContentDescription("Two card").assertExists()
    }

    @Test
    fun shouldReloadOnFilterChange() {
        val gateway = FakeLibraryGateway()
        composeRule.setContent {
            HomeflixTheme {
                libraryScreen(gateway)
            }
        }

        composeRule.onNodeWithContentDescription("Genre filter").performClick()
        composeRule.onNodeWithContentDescription("Drama option").performClick()

        composeRule.runOnIdle {
            assertEquals(
                "Drama",
                gateway.pageRequests
                    .last()
                    .selection.genre,
            )
            assertEquals(0, gateway.pageRequests.last().startIndex)
        }
    }

    @Test
    fun shouldShowSkeletonWhileLoading() {
        composeRule.setContent {
            HomeflixTheme {
                libraryScreen(FakeLibraryGateway(pagePending = true))
            }
        }

        composeRule.onNodeWithContentDescription("Loading Movies").assertExists()
    }

    @Test
    fun shouldShowRetryOnFailure() {
        composeRule.setContent {
            HomeflixTheme {
                libraryScreen(FakeLibraryGateway(pageFailure = true))
            }
        }

        composeRule.onNodeWithText("Can’t load this library.").assertExists()
        composeRule.onNodeWithContentDescription("Retry").assertExists()
    }
}

@androidx.compose.runtime.Composable
private fun libraryScreen(gateway: FakeLibraryGateway) {
    LibraryScreen(
        gateway = gateway,
        userId = "user-one",
        library = moviesLibrary(),
        libraries = listOf(moviesLibrary()),
        profile = TvNavProfile(name = "Darrow", avatarUrl = null),
        onHomeSelected = {},
        onLibrarySelected = {},
        onMediaSelected = {},
        onProfileSelected = {},
    )
}

private fun moviesLibrary(): LibrarySummary =
    LibrarySummary(
        id = "movies-view",
        name = "Movies",
        collectionType = "movies",
    )

private class FakeLibraryGateway(
    private val pageFailure: Boolean = false,
    private val pagePending: Boolean = false,
) : LibraryGateway {
    val pageRequests = mutableListOf<LibraryPageRequest>()

    override suspend fun fetchLibraries(userId: String): List<LibrarySummary> = listOf(moviesLibrary())

    override suspend fun fetchPage(
        userId: String,
        request: LibraryPageRequest,
    ): MediaPage {
        pageRequests += request
        if (pagePending) {
            awaitCancellation()
        }
        check(!pageFailure) { "library unavailable" }
        return MediaPage(
            items = listOf(mediaItem("item-one", "One"), mediaItem("item-two", "Two")),
            totalRecordCount = 2,
        )
    }

    override suspend fun fetchFilterOptions(
        userId: String,
        libraryId: String,
    ): LibraryFilterOptions =
        LibraryFilterOptions(
            genres = listOf("Drama"),
            decades = LibraryFilters.decadesFromYears(listOf(1994)),
        )
}

private fun mediaItem(
    id: String,
    name: String,
): MediaItem =
    MediaItem(
        id = id,
        name = name,
        type = "Movie",
        seriesId = null,
        year = 2026,
        overview = null,
        genres = emptyList(),
        primaryImageUrl = null,
        backdropImageUrl = null,
        playedPercentage = null,
    )
