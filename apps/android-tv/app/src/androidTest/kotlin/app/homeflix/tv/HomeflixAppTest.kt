package app.homeflix.tv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class HomeflixAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shouldLaunchHomeflixApp() {
        composeRule.onNodeWithText("HOMEFLIX").assertIsDisplayed()
    }
}
