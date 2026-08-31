package app.homeflix.tv.feature.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProfileModelsTest {
    @Test
    fun `should strip http scheme`() {
        assertEquals("192.0.2.20:8096", profileServerAddress("http://192.0.2.20:8096"))
    }

    @Test
    fun `should strip https scheme and trailing slash`() {
        assertEquals("flix.example.com", profileServerAddress("https://flix.example.com/"))
    }
}
