package app.homeflix.tv.feature.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import app.homeflix.tv.core.designsystem.HomeflixTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldFocusFirstContinueCard() {
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Episode One card").assertIsFocused()
        composeRule.onNodeWithContentDescription("Hero Episode One").assertExists()
        composeRule.onNodeWithContentDescription("Hero metadata").assertExists()
    }

    @Test
    fun shouldPositionFirstRailBelowHero() {
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        val viewportHeight =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .boundsInRoot.height
        val firstRailTop =
            composeRule
                .onNodeWithContentDescription("Episode One card")
                .fetchSemanticsNode()
                .boundsInRoot.top

        assertTrue(
            "First rail is above hero boundary",
            firstRailTop >= viewportHeight * MIN_FIRST_RAIL_TOP_FRACTION,
        )
    }

    @Test
    fun shouldFillHeroWithArtwork() {
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        val viewportWidth =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .boundsInRoot.width
        val artworkBounds =
            composeRule
                .onNodeWithContentDescription("Hero artwork")
                .fetchSemanticsNode()
                .boundsInRoot

        assertTrue("Hero artwork is not full-bleed", artworkBounds.width >= viewportWidth * MIN_ARTWORK_WIDTH_FRACTION)
        assertEquals("Hero artwork does not start at the top", 0f, artworkBounds.top, TOP_EDGE_TOLERANCE)
    }

    @Test
    fun shouldShowLeftNavigationRail() {
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        val profileRight =
            composeRule
                .onNodeWithContentDescription("Switch profile")
                .fetchSemanticsNode()
                .boundsInRoot.right
        val cardLeft =
            composeRule
                .onNodeWithContentDescription("Episode One card")
                .fetchSemanticsNode()
                .boundsInRoot.left

        composeRule.onNodeWithContentDescription("Home selected").assertExists()
        assertTrue("Profile is outside left navigation", profileRight < cardLeft)
    }

    @Test
    fun shouldMoveFocusToProfileRail() {
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Episode One card").performKeyInput {
            pressKey(Key.DirectionLeft)
        }

        composeRule.onNodeWithContentDescription("Switch profile").assertIsFocused()
    }

    @Test
    fun shouldKeepFocusOnCardsWhenPressingUp() {
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Episode One card").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        composeRule.onNodeWithContentDescription("Episode Two card").performKeyInput {
            pressKey(Key.DirectionUp)
        }

        composeRule.onNodeWithContentDescription("Episode Two card").assertIsFocused()
    }

    @Test
    fun shouldReturnFocusFromProfileToCards() {
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Episode One card").performKeyInput {
            pressKey(Key.DirectionLeft)
        }
        composeRule.onNodeWithContentDescription("Switch profile").assertIsFocused()
        composeRule.onNodeWithContentDescription("Switch profile").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        composeRule.onNodeWithContentDescription("Episode One card").assertIsFocused()

        composeRule.onNodeWithContentDescription("Episode One card").performKeyInput {
            pressKey(Key.DirectionLeft)
        }
        composeRule.onNodeWithContentDescription("Switch profile").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.onNodeWithContentDescription("Episode One card").assertIsFocused()
    }

    @Test
    fun shouldUpdateHeroAndSelectionOnFocus() {
        var selectedId: String? = null
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = { selectedId = it },
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Episode One card").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.onNodeWithContentDescription("Featured One card").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        composeRule.onNodeWithContentDescription("Featured Two card").assertIsFocused()
        composeRule.onNodeWithContentDescription("Hero Featured Two").assertExists()
        composeRule.onNodeWithContentDescription("Featured Two card").performKeyInput {
            pressKey(Key.Enter)
        }

        composeRule.runOnIdle {
            assertEquals("featured-two", selectedId)
        }
    }

    @Test
    fun shouldMoveFocusToNextRail() {
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Episode One card").performKeyInput {
            pressKey(Key.DirectionDown)
        }

        composeRule.onNodeWithContentDescription("Featured One card").assertIsFocused()
    }

    @Test
    fun shouldPinFocusedCardAtRailStart() {
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Episode One card").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        composeRule.onNodeWithContentDescription("Episode Two card").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        composeRule.onNodeWithContentDescription("Episode Three card").assertIsFocused()

        val viewportWidth =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .boundsInRoot.width
        val focusedLeft =
            composeRule
                .onNodeWithContentDescription("Episode Three card")
                .fetchSemanticsNode()
                .boundsInRoot.left

        assertTrue(
            "Focused card is not pinned near the rail start",
            focusedLeft <= viewportWidth * MAX_PINNED_CARD_LEFT_FRACTION,
        )
    }

    @Test
    fun shouldExposePlaybackProgress() {
        composeRule.setContent {
            HomeflixTheme {
                HomeCatalog(
                    content = homeContent(),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("45% watched").assertExists()
    }

    @Test
    fun shouldShowEmptyLibrary() {
        composeRule.setContent {
            HomeflixTheme {
                HomeScreen(
                    gateway = FakeHomeGateway(HomeContent(emptyList(), emptyList())),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Your library is ready.").assertExists()
    }

    @Test
    fun shouldShowLoadError() {
        composeRule.setContent {
            HomeflixTheme {
                HomeScreen(
                    gateway = FakeHomeGateway(failure = IllegalStateException("offline")),
                    viewer = viewer(),
                    onMediaSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Can’t load Homeflix.").assertExists()
    }
}

private class FakeHomeGateway(
    private val content: HomeContent = HomeContent(emptyList(), emptyList()),
    private val failure: Throwable? = null,
) : HomeGateway {
    override suspend fun fetchHome(userId: String): HomeContent {
        failure?.let { throw it }
        return content
    }
}

private fun viewer(): HomeViewer =
    HomeViewer(
        id = "user-one",
        name = "Darrow",
        avatarUrl = null,
    )

private fun homeContent(): HomeContent =
    HomeContent(
        featured =
            listOf(
                mediaItem(id = "featured-one", name = "Featured One"),
                mediaItem(id = "featured-two", name = "Featured Two"),
            ),
        rails =
            listOf(
                HomeRail(
                    id = "continue",
                    title = "Continue watching",
                    items = continueItems(),
                    variant = HomeRailVariant.Poster,
                ),
                posterRail(id = "movies", itemId = "movie-one"),
                posterRail(id = "shows", itemId = "series-one"),
            ),
    )

private fun posterRail(
    id: String,
    itemId: String,
): HomeRail =
    HomeRail(
        id = id,
        title = "Recently Added",
        items = listOf(mediaItem(id = itemId, name = itemId)),
        variant = HomeRailVariant.Poster,
    )

private fun continueItems(): List<HomeMediaItem> {
    val ordinals = listOf("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight")
    return ordinals.mapIndexed { index, ordinal ->
        mediaItem(
            id = "episode-${ordinal.lowercase()}",
            name = "Episode $ordinal",
            type = "Episode",
            playedPercentage = if (index == 0) 45f else null,
        )
    }
}

private fun mediaItem(
    id: String,
    name: String,
    type: String = "Movie",
    playedPercentage: Float? = null,
): HomeMediaItem =
    HomeMediaItem(
        id = id,
        name = name,
        type = type,
        seriesId = if (type == "Episode") "series-one" else null,
        year = 2026,
        overview = FAKE_OVERVIEW,
        genres = listOf("Drama", "Adventure"),
        primaryImageUrl = null,
        backdropImageUrl = null,
        playedPercentage = playedPercentage,
    )

private const val FAKE_OVERVIEW =
    "A private-library title follows two friends across a dangerous world as they search for answers " +
        "and face rivals who test their loyalty."
private const val MIN_FIRST_RAIL_TOP_FRACTION = 0.5f
private const val MAX_PINNED_CARD_LEFT_FRACTION = 0.25f
private const val MIN_ARTWORK_WIDTH_FRACTION = 0.98f
private const val TOP_EDGE_TOLERANCE = 0.5f
