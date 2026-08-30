package app.homeflix.tv.feature.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProfileSelectionTest {
    @Test
    fun `should request pin for protected profile`() {
        val profile = AuthProfile(id = "owen", name = "Owen", hasPassword = true)

        val action = ProfileSelection.select(profile)

        assertEquals(ProfileSelectionAction.RequestPin(profile), action)
    }

    @Test
    fun `should authenticate passwordless profile without pin`() {
        val profile = AuthProfile(id = "guest", name = "Guest", hasPassword = false)

        val action = ProfileSelection.select(profile)

        assertEquals(ProfileSelectionAction.Authenticate(profile, pin = ""), action)
    }
}
