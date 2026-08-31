package app.homeflix.tv.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import app.homeflix.tv.core.designsystem.HomeflixTheme
import org.junit.Rule
import org.junit.Test

class AuthScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldShowApiProfiles() {
        val profiles = authProfileFixtures()

        composeRule.setContent {
            HomeflixTheme {
                AuthScreen(
                    gateway = FakeAuthGateway(profiles = profiles),
                    onAuthenticated = {},
                )
            }
        }

        profiles.forEach { profile ->
            composeRule.onNodeWithContentDescription("${profile.name} profile").assertIsDisplayed()
        }
    }

    @Test
    fun shouldShowProfileLoadError() {
        composeRule.setContent {
            HomeflixTheme {
                AuthScreen(
                    gateway = FakeAuthGateway(failure = IllegalStateException("offline")),
                    onAuthenticated = {},
                )
            }
        }

        composeRule.onNodeWithText("Can’t reach the server.").assertIsDisplayed()
        composeRule.onNodeWithText("Check the server configuration and network").assertIsDisplayed()
    }
}

private class FakeAuthGateway(
    private val profiles: List<AuthProfile> = emptyList(),
    private val failure: Throwable? = null,
) : AuthGateway {
    override suspend fun fetchPublicProfiles(): List<AuthProfile> {
        failure?.let { throw it }
        return profiles
    }

    override suspend fun authenticate(
        username: String,
        password: String,
    ): AuthenticatedUser = AuthenticatedUser(accessToken = "token", userId = "id", userName = username)
}
