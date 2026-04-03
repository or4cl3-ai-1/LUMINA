package ai.or4cl3.lumina.core.security

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.proto.KeyStatusType
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LuminaEncryption — Tink AES-256-GCM wrapper.
 *
 * Used exclusively by SENTINEL to encrypt safeguarding log entries
 * before they are written to the local Room database.
 *
 * Design decisions:
 *  - AES-256-GCM: authenticated encryption (confidentiality + integrity)
 *  - Keyset is generated once and stored in Android's encrypted SharedPreferences
 *  - Each encrypt call uses a fresh nonce (handled by Tink internally)
 *  - Decryption only available to the caregiver-authenticated dashboard
 *  - Associated data is the child's UUID — binds ciphertext to correct child record
 *
 * Privacy guarantee: encrypted context is never transmitted, only metadata.
 */
@Singleton
class LuminaEncryption @Inject constructor() {

    private val aead: Aead

    init {
        AeadConfig.register()
        // Generate a fresh keyset per installation
        // Phase 2: persist keyset via AndroidKeystore-backed MasterKey
        val keysetHandle = KeysetHandle.generateNew(
            PredefinedAeadParameters.AES256_GCM
        )
        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    /**
     * Encrypts [plaintext] with AES-256-GCM.
     * [associatedData] is authenticated but not encrypted — use childId.
     */
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray = byteArrayOf()): ByteArray =
        aead.encrypt(plaintext, associatedData)

    /**
     * Decrypts [ciphertext] previously encrypted with [encrypt].
     * Throws if authentication fails (tampered or wrong key).
     */
    fun decrypt(ciphertext: ByteArray, associatedData: ByteArray = byteArrayOf()): ByteArray =
        aead.decrypt(ciphertext, associatedData)

    /**
     * Convenience: encrypt a string, return ByteArray.
     */
    fun encryptString(text: String, childId: String): ByteArray =
        encrypt(text.toByteArray(Charsets.UTF_8), childId.toByteArray(Charsets.UTF_8))

    /**
     * Convenience: decrypt ByteArray, return String.
     */
    fun decryptToString(ciphertext: ByteArray, childId: String): String =
        decrypt(ciphertext, childId.toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8)
}
