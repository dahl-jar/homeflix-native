package app.homeflix.tv.core.session

import app.homeflix.tv.core.network.JsonApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class SessionValidatorTest {
    @Test
    fun `should accept current user response`() =
        runBlocking {
            val client = StubClient(response = "{\"Id\":\"user-one\"}")

            assertTrue(SessionValidator.validate(client))
        }

    @Test
    fun `should reject failed current user response`() =
        runBlocking {
            val client = StubClient(failure = IOException("unavailable"))

            assertFalse(SessionValidator.validate(client))
        }

    @Test
    fun `should preserve cancellation`() {
        val client = StubClient(failure = CancellationException("cancelled"))

        assertThrows(CancellationException::class.java) {
            runBlocking { SessionValidator.validate(client) }
        }
    }
}

private class StubClient(
    private val response: String = "",
    private val failure: Throwable? = null,
) : JsonApiClient {
    override suspend fun get(path: String): String {
        failure?.let { throw it }
        return response
    }

    override suspend fun post(
        path: String,
        body: String,
    ): String = error("unexpected POST")
}
