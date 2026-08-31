package app.homeflix.tv.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.homeflix.tv.core.designsystem.HomeflixTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ServerSetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldSubmitTypedUrl() {
        var submitted: String? = null
        composeRule.setContent {
            HomeflixTheme {
                ServerSetupScreen(
                    initialUrl = "",
                    onConnect = { url ->
                        submitted = url
                        null
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Server address").performTextInput("http://server:8096")
        composeRule.onNodeWithContentDescription("Connect").performClick()
        composeRule.waitForIdle()

        assertEquals("http://server:8096", submitted)
    }

    @Test
    fun shouldShowErrorWhenUnreachable() {
        composeRule.setContent {
            HomeflixTheme {
                ServerSetupScreen(
                    initialUrl = "http://server:8096",
                    onConnect = { ServerConnectError.Unreachable },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Connect").performClick()

        composeRule.onNodeWithText("Can’t reach a server at this address").assertIsDisplayed()
    }

    @Test
    fun shouldGoBackWhenBackAvailable() {
        var wentBack = false
        composeRule.setContent {
            HomeflixTheme {
                ServerSetupScreen(
                    initialUrl = "http://server:8096",
                    onConnect = { null },
                    onBack = { wentBack = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(wentBack)
    }
}
