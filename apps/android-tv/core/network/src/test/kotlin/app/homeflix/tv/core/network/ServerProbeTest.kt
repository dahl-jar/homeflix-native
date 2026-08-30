package app.homeflix.tv.core.network

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ServerProbeTest {
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun `should accept successful system response`() {
        server.enqueue(MockResponse(code = 200, body = "{}"))

        assertTrue(probeJellyfinServer(server.url("/").toString()))
    }

    @Test
    fun `should reject failed system response`() {
        server.enqueue(MockResponse(code = 503, body = "unavailable"))

        assertFalse(probeJellyfinServer(server.url("/").toString()))
    }
}
