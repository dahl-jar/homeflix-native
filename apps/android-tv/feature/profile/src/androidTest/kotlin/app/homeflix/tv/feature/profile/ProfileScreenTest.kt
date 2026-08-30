package app.homeflix.tv.feature.profile

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import app.homeflix.tv.core.designsystem.HomeflixTheme
import app.homeflix.tv.core.designsystem.TvNavProfile
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldFocusSwitchButton() {
        setProfile(onSwitchProfile = {})

        composeRule.onNodeWithContentDescription("Sign out").assertIsFocused()
    }

    @Test
    fun shouldSignOutFromSwitch() {
        var signedOut = false
        setProfile(onSwitchProfile = { signedOut = true })

        composeRule
            .onNodeWithContentDescription("Sign out")
            .performKeyInput { pressKey(Key.DirectionCenter) }

        assertTrue(signedOut)
    }

    private fun setProfile(onSwitchProfile: () -> Unit) {
        composeRule.setContent {
            HomeflixTheme {
                ProfileScreen(
                    details =
                        ProfileDetails(
                            name = "Owen",
                            avatarUrl = null,
                            serverAddress = "192.168.1.20:8096",
                            appVersion = "1.0.0",
                        ),
                    profile = TvNavProfile(name = "Owen", avatarUrl = null),
                    libraries = emptyList(),
                    onHomeSelected = {},
                    onLibrarySelected = {},
                    onSwitchProfile = onSwitchProfile,
                )
            }
        }
    }
}
