package app.homeflix.tv.feature.profile

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
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

    @Test
    fun shouldChangeServer() {
        var changed = false
        setProfile(onSwitchProfile = {}, onChangeServer = { changed = true })

        composeRule.onNodeWithContentDescription("Change server").performClick()

        assertTrue(changed)
    }

    private fun setProfile(
        onSwitchProfile: () -> Unit,
        onChangeServer: () -> Unit = {},
    ) {
        composeRule.setContent {
            HomeflixTheme {
                ProfileScreen(
                    details =
                        ProfileDetails(
                            name = "Alex",
                            avatarUrl = null,
                            serverAddress = "192.0.2.20:8096",
                            appVersion = "1.0.0",
                        ),
                    profile = TvNavProfile(name = "Alex", avatarUrl = null),
                    libraries = emptyList(),
                    onHomeSelected = {},
                    onLibrarySelected = {},
                    onSwitchProfile = onSwitchProfile,
                    onChangeServer = onChangeServer,
                )
            }
        }
    }
}
