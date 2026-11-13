package com.neuroverse.authapp.data.local

import android.content.Context
import androidx.core.content.edit
import com.neuroverse.authapp.data.security.PasswordHasher
import org.json.JSONObject
import java.util.UUID

/**
 * On-device account storage for the offline demo mode.
 *
 * Stores one JSON record per email: `{ id, name, passwordHash }`.
 * The password itself is NEVER stored — only the PBKDF2 output (spec 5).
 *
 * This is NOT a production account database and does not pretend to be. It
 * exists so the app runs, and so all ten test scenarios in spec section 7 can
 * be demonstrated, without requiring a Firebase project first.
 */
class LocalCredentialStore(context: Context) {

    private companion object {
        const val PREFS_NAME = "neuroverse_local_accounts"
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_HASH = "passwordHash"
    }

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Emails are case-insensitive identifiers, so normalise before using as a key. */
    private fun key(email: String) = email.trim().lowercase()

    /** True when an account already exists for [email] (spec 3.1). */
    fun exists(email: String): Boolean = prefs.contains(key(email))

    /**
     * Creates an account and returns its new id.
     * Caller must have checked [exists] first.
     */
    fun create(name: String, email: String, password: String): String {
        val id = UUID.randomUUID().toString()
        val record = JSONObject().apply {
            put(FIELD_ID, id)
            put(FIELD_NAME, name.trim())
            put(FIELD_HASH, PasswordHasher.hash(password))
        }
        prefs.edit { putString(key(email), record.toString()) }
        return id
    }

    /**
     * Verifies [password] against the stored hash.
     *
     * @return the account's `id to name` on success, or null on any failure
     *   (no such account, wrong password, corrupt record). The caller turns all
     *   of those into one indistinguishable InvalidCredentials error.
     */
    fun verify(email: String, password: String): Pair<String, String>? {
        val raw = prefs.getString(key(email), null) ?: return null
        return try {
            val record = JSONObject(raw)
            if (!PasswordHasher.verify(password, record.getString(FIELD_HASH))) return null
            record.getString(FIELD_ID) to record.getString(FIELD_NAME)
        } catch (e: Exception) {
            null
        }
    }

    /** Replaces the stored hash. Used by the simulated reset flow. */
    fun updatePassword(email: String, newPassword: String): Boolean {
        val raw = prefs.getString(key(email), null) ?: return false
        return try {
            val record = JSONObject(raw)
                .put(FIELD_HASH, PasswordHasher.hash(newPassword))
            prefs.edit { putString(key(email), record.toString()) }
            true
        } catch (e: Exception) {
            false
        }
    }
}
