package app.homeflix.tv.core.session

import android.content.Context

interface SessionStore {
    fun save(session: StoredSession)

    fun load(): StoredSession?

    fun clear()
}

class AndroidSessionStore(
    context: Context,
    private val cipher: SessionCipher = AndroidKeystoreSessionCipher(),
) : SessionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun save(session: StoredSession) {
        val encrypted = cipher.encrypt(SessionPayloadCodec.encode(session))
        check(
            preferences
                .edit()
                .putString(CIPHERTEXT_KEY, encrypted.ciphertext)
                .putString(INITIALIZATION_VECTOR_KEY, encrypted.initializationVector)
                .commit(),
        ) { "Unable to persist session" }
    }

    override fun load(): StoredSession? =
        encryptedSession()?.let { encrypted ->
            runCatching {
                SessionPayloadCodec.decode(cipher.decrypt(encrypted))
            }.getOrElse {
                clear()
                null
            }
        }

    private fun encryptedSession(): EncryptedSession? {
        val ciphertext = preferences.getString(CIPHERTEXT_KEY, null)
        val initializationVector = preferences.getString(INITIALIZATION_VECTOR_KEY, null)
        return if (ciphertext == null || initializationVector == null) {
            null
        } else {
            EncryptedSession(
                initializationVector = initializationVector,
                ciphertext = ciphertext,
            )
        }
    }

    override fun clear() {
        check(preferences.edit().clear().commit()) { "Unable to clear session" }
    }

    internal fun encryptedPayloadForAudit(): String? = preferences.getString(CIPHERTEXT_KEY, null)

    private companion object {
        const val PREFERENCES_NAME = "homeflix-session"
        const val CIPHERTEXT_KEY = "ciphertext"
        const val INITIALIZATION_VECTOR_KEY = "initialization-vector"
    }
}
