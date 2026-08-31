package app.homeflix.tv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.feature.auth.AuthGateway
import app.homeflix.tv.feature.auth.AuthProfile
import app.homeflix.tv.feature.auth.AuthenticatedUser
import app.homeflix.tv.feature.home.HomeContent
import app.homeflix.tv.feature.home.HomeGateway
import org.junit.Rule
import org.junit.Test

class HomeflixRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldShowHomeAfterAuthentication() {
        composeRule.setContent {
            HomeflixApp(
                server = "http://server",
                authGateway = AppAuthGateway(),
                homeGateway = AppHomeGateway(),
            )
        }

        composeRule.onNodeWithContentDescription("Darrow profile").performClick()

        composeRule.waitUntil(HOME_TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithContentDescription("Featured One card")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Featured One card").assertIsDisplayed()
    }
}

private const val HOME_TIMEOUT_MILLIS = 5_000L

private class AppAuthGateway : AuthGateway {
    override suspend fun fetchPublicProfiles(): List<AuthProfile> =
        listOf(AuthProfile(id = "user-one", name = "Darrow", hasPassword = false))

    override suspend fun authenticate(
        username: String,
        password: String,
    ): AuthenticatedUser =
        AuthenticatedUser(
            accessToken = "access-token",
            userId = "user-one",
            userName = username,
        )
}

private class AppHomeGateway : HomeGateway {
    override suspend fun fetchHome(userId: String): HomeContent =
        HomeContent(
            featured =
                listOf(
                    MediaItem(
                        id = "featured-one",
                        name = "Featured One",
                        type = "Movie",
                        seriesId = null,
                        year = 2026,
                        overview = "Overview",
                        genres = listOf("Drama"),
                        primaryImageUrl = null,
                        backdropImageUrl = null,
                        playedPercentage = null,
                    ),
                ),
            rails = emptyList(),
        )
}
