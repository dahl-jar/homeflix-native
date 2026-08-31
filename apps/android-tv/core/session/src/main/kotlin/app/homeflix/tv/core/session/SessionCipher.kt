package app.homeflix.tv.core.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedSession(
    val initializationVector: String,
    val ciphertext: String,
)

interface SessionCipher {
    fun encrypt(payload: String): EncryptedSession

    fun decrypt(session: EncryptedSession): String
}

class AndroidKeystoreSessionCipher : SessionCipher {
    override fun encrypt(payload: String): EncryptedSession {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, loadOrCreateKey())
        return EncryptedSession(
            initializationVector = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(cipher.doFinal(payload.encodeToByteArray()), Base64.NO_WRAP),
        )
    }

    override fun decrypt(session: EncryptedSession): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            loadOrCreateKey(),
            GCMParameterSpec(
                AUTHENTICATION_TAG_BITS,
                Base64.decode(session.initializationVector, Base64.NO_WRAP),
            ),
        )
        return cipher
            .doFinal(Base64.decode(session.ciphertext, Base64.NO_WRAP))
            .decodeToString()
    }

    private fun loadOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEY_STORE_NAME).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_NAME)
        generator.init(
            KeyGenParameterSpec
                .Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_STORE_NAME = "AndroidKeyStore"
        const val KEY_ALIAS = "homeflix-session-key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AUTHENTICATION_TAG_BITS = 128
        const val KEY_SIZE_BITS = 256
    }
}
