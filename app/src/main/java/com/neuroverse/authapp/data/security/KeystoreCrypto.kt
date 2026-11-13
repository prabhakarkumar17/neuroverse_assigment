package com.neuroverse.authapp.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256/GCM encryption whose key lives in the ANDROID KEYSTORE.
 *
 * This satisfies spec section 4 ("Android Keystore-backed secure storage") and
 * section 5 ("Authentication tokens shall be stored securely").
 *
 * WHY THE KEYSTORE IS THE WHOLE POINT
 * -----------------------------------
 * Encrypting data is easy; the hard part is "where do you keep the key?".
 * A key hardcoded in the APK, or stored in SharedPreferences next to the data,
 * is no protection at all — anyone who can read the data can read the key.
 *
 * The Android Keystore solves this: the key is generated INSIDE the system
 * keystore (on most modern devices, inside a hardware security module / Trusted
 * Execution Environment) and the key material is never returned to our process.
 * We hold only an opaque handle. We ask the system to encrypt and decrypt FOR
 * us. Even with a rooted device and a full filesystem dump, the attacker gets
 * ciphertext and a key reference that is useless off that device.
 *
 * WHY GCM AND NOT CBC
 * -------------------
 * GCM is an AEAD mode: it authenticates as well as encrypts. If a single byte
 * of ciphertext is altered, decryption THROWS instead of returning garbage.
 * That turns silent tampering into a loud failure, which is what you want for
 * an auth token. CBC would decrypt modified data happily.
 *
 * THE IV RULE
 * -----------
 * GCM requires a unique initialisation vector per encryption with the same key.
 * Reusing an IV with GCM is catastrophic — it leaks the authentication key, not
 * just the plaintext. So we never choose an IV ourselves: we let the Cipher
 * generate a fresh random one on every encrypt, and we store it ALONGSIDE the
 * ciphertext. An IV is not secret; it only has to be unique.
 *
 * Java comparison: this is the same JCA API as Java SE (Cipher, KeyGenerator,
 * SecretKey). The Android-specific parts are the "AndroidKeyStore" provider
 * name and KeyGenParameterSpec.
 */
class KeystoreCrypto(private val keyAlias: String) {

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128      // Authentication tag length; 128 is the maximum and the default.
        const val IV_LENGTH = 12          // 96 bits: the size GCM is specified and optimised for.
    }

    /**
     * Returns the existing key for this alias, creating it on first use.
     *
     * Note the key is created once and then reused for the lifetime of the
     * install. Uninstalling the app deletes it, which is correct: the encrypted
     * session data goes away at the same time.
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

        // Already generated on a previous run? Reuse it.
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)
            ?.let { return it.secretKey }

        // First run: generate inside the keystore. The raw bytes never reach us.
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // We do NOT require user authentication (fingerprint/PIN) to use
                // the key. Requiring it would force a biometric prompt on every
                // app launch just to read the session. That is a deliberate
                // usability trade-off, documented here so it is a decision
                // rather than an oversight.
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    /**
     * Encrypts [plainText] and returns a single Base64 string containing
     * `IV || ciphertext`, safe to put in SharedPreferences.
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)

        // No IV passed in: the provider generates a secure random one for us.
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Prepend the IV so decrypt() can recover it. IVs are not secret.
        val combined = iv + cipherText
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Reverses [encrypt].
     *
     * Returns null rather than throwing when the value cannot be decrypted —
     * which happens legitimately when the key was invalidated (user added a
     * lock screen, restored a backup to a new device). The caller treats null
     * as "no valid session" and sends the user to the Login screen, which is
     * the safe default.
     */
    fun decrypt(encoded: String): String? = try {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)

        // Split the stored blob back into its two parts.
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val cipherText = combined.copyOfRange(IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))

        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    } catch (e: Exception) {
        // Covers AEADBadTagException (tampering), KeyPermanentlyInvalidatedException,
        // and malformed Base64. All mean the same thing to us: unusable session.
        null
    }

    /** Deletes the key, making every value encrypted with it permanently unreadable. */
    fun deleteKey() {
        runCatching {
            KeyStore.getInstance(PROVIDER).apply { load(null) }.deleteEntry(keyAlias)
        }
    }
}
