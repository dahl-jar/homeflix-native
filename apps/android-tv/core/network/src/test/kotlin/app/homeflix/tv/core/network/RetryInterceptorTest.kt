package app.homeflix.tv.core.network

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException

private const val SERVER_ERROR = 500

class RetryInterceptorTest {
    @Test
    fun `should retry server errors until success`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse(code = SERVER_ERROR, body = "busy"))
            server.enqueue(MockResponse(body = "ok"))
            server.start()

            val response = clientWithRetry().newCall(request(server)).execute()

            assertEquals(200, response.code)
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun `should retry connection failures until success`() {
        MockWebServer().use { server ->
            server.enqueue(disconnect())
            server.enqueue(MockResponse(body = "ok"))
            server.start()

            val response = clientWithRetry().newCall(request(server)).execute()

            assertEquals(200, response.code)
        }
    }

    @Test
    fun `should rethrow when connection failures exhaust retries`() {
        MockWebServer().use { server ->
            repeat(3) { server.enqueue(disconnect()) }
            server.start()

            assertThrows(IOException::class.java) {
                clientWithRetry().newCall(request(server)).execute()
            }
        }
    }

    @Test
    fun `should give up after max retries`() {
        MockWebServer().use { server ->
            repeat(3) { server.enqueue(MockResponse(code = SERVER_ERROR, body = "busy")) }
            server.start()

            val response = clientWithRetry().newCall(request(server)).execute()

            assertEquals(SERVER_ERROR, response.code)
            assertEquals(3, server.requestCount)
        }
    }

    private fun clientWithRetry(): OkHttpClient =
        OkHttpClient
            .Builder()
            .retryOnConnectionFailure(false)
            .addInterceptor(RetryInterceptor(sleep = {}))
            .build()

    private fun disconnect(): MockResponse =
        MockResponse
            .Builder()
            .onRequestStart(SocketEffect.CloseSocket())
            .build()

    private fun request(server: MockWebServer): Request = Request.Builder().url(server.url("/image")).build()
}
