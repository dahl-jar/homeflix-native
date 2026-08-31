package app.homeflix.tv.core.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AndroidSessionStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = AndroidSessionStore(context)

    @Before
    fun setUp() {
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun shouldPersistEncryptedSession() {
        val session =
            StoredSession(
                accessToken = "access-token",
                userId = "user-one",
                userName = "Darrow",
                primaryImageTag = "image-one",
            )

        store.save(session)

        assertEquals(session, store.load())
        assertNotEquals(session.accessToken, store.encryptedPayloadForAudit())
    }

    @Test
    fun shouldClearSession() {
        store.save(
            StoredSession(
                accessToken = "access-token",
                userId = "user-one",
                userName = "Darrow",
            ),
        )

        store.clear()

        assertNull(store.load())
    }

    @Test
    fun shouldClearUnreadableSession() {
        val cipher = FailingSessionCipher()
        val corruptibleStore = AndroidSessionStore(context, cipher)
        corruptibleStore.save(
            StoredSession(
                accessToken = "access-token",
                userId = "user-one",
                userName = "Darrow",
            ),
        )
        cipher.shouldFail = true

        assertNull(corruptibleStore.load())
        assertNull(corruptibleStore.encryptedPayloadForAudit())
    }
}

private class FailingSessionCipher : SessionCipher {
    var shouldFail = false

    override fun encrypt(payload: String): EncryptedSession =
        EncryptedSession(initializationVector = "iv", ciphertext = payload)

    override fun decrypt(session: EncryptedSession): String {
        if (shouldFail) error("unreadable")
        return session.ciphertext
    }
}
