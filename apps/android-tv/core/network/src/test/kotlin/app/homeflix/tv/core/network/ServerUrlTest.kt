package app.homeflix.tv.core.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ServerUrlTest {
    @Test
    fun `should keep http and https urls`() {
        assertEquals("http://one:8096", normalizeServerUrl("http://one:8096"))
        assertEquals("https://two.example", normalizeServerUrl("https://two.example"))
    }

    @Test
    fun `should trim whitespace and trailing slashes`() {
        assertEquals("https://two.example", normalizeServerUrl(" https://two.example/// "))
    }

    @Test
    fun `should reject url without scheme`() {
        assertNull(normalizeServerUrl("two.example:8096"))
    }

    @Test
    fun `should reject scheme without host`() {
        assertNull(normalizeServerUrl("https://"))
    }

    @Test
    fun `should reject blank input`() {
        assertNull(normalizeServerUrl("   "))
    }
}
