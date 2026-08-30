package app.homeflix.tv.core.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ServerCandidatesTest {
    @Test
    fun `should normalize server candidates`() {
        val candidates = parseServerCandidates(" http://one:8096/, ,https://two.example/// ")

        assertEquals(listOf("http://one:8096", "https://two.example"), candidates)
    }

    @Test
    fun `should select first reachable server`() {
        val selected =
            resolveServer(listOf("http://one", "http://two")) { candidate ->
                candidate == "http://two"
            }

        assertEquals("http://two", selected)
    }

    @Test
    fun `should return null when servers are unreachable`() {
        assertNull(resolveServer(listOf("http://one")) { false })
    }
}
