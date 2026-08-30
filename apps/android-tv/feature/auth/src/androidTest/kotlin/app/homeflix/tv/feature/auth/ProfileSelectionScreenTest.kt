package app.homeflix.tv.feature.auth

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import app.homeflix.tv.core.designsystem.HomeflixTheme
import org.junit.Rule
import org.junit.Test

class ProfileSelectionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldMoveFocusAcrossProfiles() {
        composeRule.setContent {
            HomeflixTheme {
                ProfileSelectionScreen(
                    profiles =
                        listOf(
                            AuthProfile(id = "one", name = "Darrow", hasPassword = true),
                            AuthProfile(id = "two", name = "Mustang", hasPassword = false),
                            AuthProfile(id = "three", name = "Goblin", hasPassword = false),
                        ),
                    onProfileSelected = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Darrow profile").assertIsFocused()
        composeRule.onNodeWithContentDescription("Darrow profile").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        composeRule.onNodeWithContentDescription("Mustang profile").assertIsFocused()
        composeRule.onNodeWithContentDescription("Mustang profile").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        composeRule.onNodeWithContentDescription("Goblin profile").assertIsFocused()
    }
}
