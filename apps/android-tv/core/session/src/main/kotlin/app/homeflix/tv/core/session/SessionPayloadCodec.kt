package app.homeflix.tv.core.session

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object SessionPayloadCodec {
    private val json = Json

    fun encode(session: StoredSession): String = json.encodeToString(session)

    fun decode(payload: String): StoredSession =
        try {
            json.decodeFromString<StoredSession>(payload)
        } catch (failure: SerializationException) {
            throw IllegalArgumentException("Invalid stored session", failure)
        }
}
