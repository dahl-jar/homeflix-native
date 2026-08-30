package app.homeflix.tv.feature.auth

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import app.homeflix.tv.core.designsystem.HomeflixTheme
import org.junit.Rule
import org.junit.Test

class PinScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldFocusFirstKeypadDigit() {
        composeRule.setContent {
            HomeflixTheme {
                PinScreen(
                    onBack = {},
                    onPinSubmitted = { true },
                )
            }
        }

        composeRule.onNodeWithText("Enter your PIN to access this profile.").assertExists()
        composeRule.onNodeWithContentDescription("1").assertIsFocused()
    }
}
