package app.homeflix.tv.core.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SessionPayloadCodecTest {
    @Test
    fun `should preserve session fields`() {
        val session =
            StoredSession(
                accessToken = "access-token",
                userId = "user-one",
                userName = "Darrow",
                primaryImageTag = "image-one",
            )

        val decoded = SessionPayloadCodec.decode(SessionPayloadCodec.encode(session))

        assertEquals(session, decoded)
    }

    @Test
    fun `should reject malformed session`() {
        assertThrows(IllegalArgumentException::class.java) {
            SessionPayloadCodec.decode("not-json")
        }
    }
}
