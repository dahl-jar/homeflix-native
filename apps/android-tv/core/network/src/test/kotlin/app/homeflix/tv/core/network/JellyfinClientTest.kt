package app.homeflix.tv.core.network

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JellyfinClientTest {
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
    fun `should send Android TV identity`() =
        runBlocking {
            server.enqueue(MockResponse(body = "[]"))
            val client =
                JellyfinClient(
                    baseUrl = server.url("/").toString().removeSuffix("/"),
                    deviceId = "device-one",
                    version = "1.0.0",
                )

            assertEquals("[]", client.get("/Users/Public"))

            val request = server.takeRequest()
            assertEquals("/Users/Public", request.url.encodedPath)
            val expectedIdentity =
                "MediaBrowser Client=\"Homeflix\", Device=\"Android TV\", " +
                    "DeviceId=\"device-one\", Version=\"1.0.0\""
            assertTrue(
                request.headers["Authorization"].orEmpty().contains(expectedIdentity),
            )
        }

    @Test
    fun `should post authentication JSON`() =
        runBlocking {
            server.enqueue(MockResponse(body = "{\"AccessToken\":\"token\"}"))
            val client =
                JellyfinClient(
                    baseUrl = server.url("/").toString().removeSuffix("/"),
                    deviceId = "device-one",
                    version = "1.0.0",
                )

            val response = client.post("/Users/AuthenticateByName", "{\"Username\":\"Darrow\",\"Pw\":\"4321\"}")

            val request = server.takeRequest()
            assertEquals("{\"AccessToken\":\"token\"}", response)
            assertEquals("POST", request.method)
            assertTrue(request.headers["Content-Type"].orEmpty().startsWith("application/json"))
            assertEquals("{\"Username\":\"Darrow\",\"Pw\":\"4321\"}", request.body?.utf8())
        }

    @Test
    fun `should reject invalid identity values`() {
        assertThrows(IllegalArgumentException::class.java) {
            JellyfinClient(baseUrl = "http://server", deviceId = "", version = "1.0.0")
        }
        assertThrows(IllegalArgumentException::class.java) {
            JellyfinClient(baseUrl = "http://server", deviceId = "device\"one", version = "1.0.0")
        }
    }

    @Test
    fun `should expose failed status and path`() {
        server.enqueue(MockResponse(code = 503, body = "unavailable"))
        val client =
            JellyfinClient(
                baseUrl = server.url("/").toString().removeSuffix("/"),
                deviceId = "device-one",
                version = "1.0.0",
            )

        val error =
            assertThrows(ApiException::class.java) {
                runBlocking { client.get("/Users/Public") }
            }

        assertEquals(503, error.status)
        assertEquals("/Users/Public", error.path)
    }

    @Test
    fun `should encode query parameters`() =
        runBlocking {
            server.enqueue(MockResponse(body = "{}"))
            val client =
                JellyfinClient(
                    baseUrl = server.url("/").toString().removeSuffix("/"),
                    deviceId = "device-one",
                    version = "1.0.0",
                    token = "access-token",
                )

            client.get(
                path = "/Items",
                query = mapOf("searchTerm" to "Red Rising", "userId" to "user-one"),
            )

            val request = server.takeRequest()
            assertEquals("Red Rising", request.url.queryParameter("searchTerm"))
            assertEquals("user-one", request.url.queryParameter("userId"))
            assertTrue(request.headers["Authorization"].orEmpty().contains("Token=\"access-token\""))
        }

    @Test
    fun `should expose authenticated media request headers`() {
        val client =
            JellyfinClient(
                baseUrl = server.url("/").toString().removeSuffix("/"),
                deviceId = "device-one",
                version = "1.0.0",
                token = "access-token",
            )

        val authorization = client.mediaRequestHeaders.getValue("Authorization")

        assertTrue(authorization.contains("Device=\"Android TV\""))
        assertTrue(authorization.contains("Token=\"access-token\""))
    }
}
