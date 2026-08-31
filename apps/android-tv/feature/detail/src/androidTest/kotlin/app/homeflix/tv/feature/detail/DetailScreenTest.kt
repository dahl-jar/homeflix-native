package app.homeflix.tv.feature.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.designsystem.HomeflixTheme
import app.homeflix.tv.core.designsystem.TvNavProfile
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldFocusPlayButton() {
        setDetail(FakeDetailGateway(item = movie()))

        composeRule.onNodeWithContentDescription("Play Movie One").assertIsFocused()
    }

    @Test
    fun shouldFireOnPlaySelected() {
        var playedId: String? = null
        setDetail(FakeDetailGateway(item = movie()), onPlaySelected = { playedId = it })

        composeRule
            .onNodeWithContentDescription("Play Movie One")
            .performKeyInput { pressKey(Key.DirectionCenter) }

        composeRule.runOnIdle {
            assertEquals("item-one", playedId)
        }
    }

    @Test
    fun shouldShowActionButtons() {
        setDetail(FakeDetailGateway(item = movie()))

        composeRule.onNodeWithContentDescription("Trailer").assertExists()
        composeRule.onNodeWithContentDescription("Mark Played").assertExists()
        composeRule.onNodeWithContentDescription("Restart").assertExists()
    }

    @Test
    fun shouldShowEpisodesForSeries() {
        setDetail(FakeDetailGateway(item = series()))

        composeRule.onNodeWithText("Season 1").assertExists()
        composeRule.onNodeWithText("1. Episode One").assertExists()
    }

    @Test
    fun shouldPushSimilarFromRail() {
        var pushedId: String? = null
        setDetail(FakeDetailGateway(item = movie()), onMediaSelected = { pushedId = it })

        composeRule
            .onNodeWithContentDescription("Similar One card")
            .performClick()

        composeRule.runOnIdle {
            assertEquals("similar-one", pushedId)
        }
    }

    @Test
    fun shouldShowSkeletonWhileLoading() {
        setDetail(PendingDetailGateway())

        composeRule.onNodeWithContentDescription("Loading details").assertExists()
    }

    private fun setDetail(
        gateway: DetailGateway,
        onMediaSelected: (String) -> Unit = {},
        onPlaySelected: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            HomeflixTheme {
                DetailScreenUnderTest(gateway, onMediaSelected, onPlaySelected)
            }
        }
    }
}

@Composable
private fun DetailScreenUnderTest(
    gateway: DetailGateway,
    onMediaSelected: (String) -> Unit,
    onPlaySelected: (String) -> Unit,
) {
    DetailScreen(
        gateway = gateway,
        userId = "user-one",
        itemId = "item-one",
        profile = TvNavProfile(name = "Darrow", avatarUrl = null),
        libraries = emptyList(),
        onHomeSelected = {},
        onLibrarySelected = {},
        onProfileSelected = {},
        onMediaSelected = onMediaSelected,
        onPlaySelected = onPlaySelected,
    )
}

private class PendingDetailGateway : DetailGateway {
    override suspend fun fetchDetail(
        userId: String,
        itemId: String,
    ): DetailContent = awaitCancellation()

    override suspend fun fetchSimilar(
        userId: String,
        itemId: String,
    ): List<MediaItem> = awaitCancellation()

    override suspend fun fetchSeasons(
        userId: String,
        seriesId: String,
    ): List<DetailSeason> = awaitCancellation()

    override suspend fun fetchEpisodes(
        userId: String,
        seriesId: String,
        seasonId: String,
    ): List<MediaItem> = awaitCancellation()
}

private class FakeDetailGateway(
    private val item: MediaItem,
) : DetailGateway {
    override suspend fun fetchDetail(
        userId: String,
        itemId: String,
    ): DetailContent =
        DetailContent(item = item, cast = listOf(CastMember(id = "person-one", name = "Darrow", imageUrl = null)))

    override suspend fun fetchSimilar(
        userId: String,
        itemId: String,
    ): List<MediaItem> = listOf(mediaItem(id = "similar-one", name = "Similar One"))

    override suspend fun fetchSeasons(
        userId: String,
        seriesId: String,
    ): List<DetailSeason> = listOf(DetailSeason(id = "season-one", name = "Season 1", indexNumber = 1))

    override suspend fun fetchEpisodes(
        userId: String,
        seriesId: String,
        seasonId: String,
    ): List<MediaItem> =
        listOf(
            mediaItem(id = "episode-one", name = "Episode One", type = "Episode", indexNumber = 1),
        )
}

private fun movie(): MediaItem = mediaItem(id = "item-one", name = "Movie One")

private fun series(): MediaItem = mediaItem(id = "item-one", name = "Series One", type = "Series")

private fun mediaItem(
    id: String,
    name: String,
    type: String = "Movie",
    indexNumber: Int? = null,
): MediaItem =
    MediaItem(
        id = id,
        name = name,
        type = type,
        seriesId = null,
        year = 2026,
        overview = "A private-library title about loyalty and rivals.",
        genres = listOf("Drama"),
        primaryImageUrl = null,
        backdropImageUrl = null,
        playedPercentage = null,
        indexNumber = indexNumber,
        runTimeTicks = 32_400_000_000,
    )
