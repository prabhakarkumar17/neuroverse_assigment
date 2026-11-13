package com.neuroverse.authapp.data.session

import android.content.Context
import com.neuroverse.authapp.data.model.User
import com.neuroverse.authapp.data.security.KeystoreCrypto
import org.json.JSONObject
import androidx.core.content.edit

/**
 * Persists the signed-in user across app restarts (spec 3.4), encrypted with a
 * Keystore-backed key (spec 4, 5).
 *
 * WHAT IS AND IS NOT STORED
 * -------------------------
 * Stored (encrypted): user id, name, email, verified flag.
 * NOT stored, ever: the password, or any derivative of it.
 *
 * We also do not persist the Firebase ID token. That surprises people, so:
 * Firebase's SDK keeps its own refresh token in its own private storage and
 * mints short-lived (1 hour) ID tokens on demand. Writing a second copy of a
 * token we do not need would widen the attack surface for no benefit — the
 * fewer places a credential lives, the better. For a CUSTOM backend you would
 * store the refresh token here, which is precisely why [saveToken] exists.
 *
 * WHY SharedPreferences AND NOT DataStore
 * ---------------------------------------
 * DataStore is the modern recommendation and is async by design. But session
 * restore has to happen synchronously BEFORE the first frame — otherwise the
 * Login screen flashes for a moment and is then replaced by Home, which looks
 * broken. Reading one small encrypted string synchronously at startup is the
 * right call here. The data is encrypted, so the storage engine underneath is
 * an implementation detail.
 */
class SessionStore(context: Context) {

    private companion object {
        const val PREFS_NAME = "neuroverse_session"
        const val KEY_SESSION = "session_blob"
        const val KEY_TOKEN = "auth_token"
        const val KEYSTORE_ALIAS = "neuroverse_session_key"

        // JSON field names for the encrypted payload.
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_VERIFIED = "verified"
    }

    // applicationContext, never the Activity: this object outlives any screen,
    // and holding an Activity here would leak the whole view hierarchy.
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val crypto = KeystoreCrypto(KEYSTORE_ALIAS)

    /** Encrypts and stores [user] as the active session. */
    fun saveSession(user: User) {
        val json = JSONObject().apply {
            put(FIELD_ID, user.id)
            put(FIELD_NAME, user.name)
            put(FIELD_EMAIL, user.email)
            put(FIELD_VERIFIED, user.isEmailVerified)
        }.toString()

        prefs.edit { putString(KEY_SESSION, crypto.encrypt(json)) }
    }

    /**
     * Returns the stored user, or null if there is no valid session.
     *
     * Null covers: never logged in, logged out, tampered ciphertext, or a key
     * invalidated by a device change. Every one of those means "show Login",
     * so collapsing them into null is correct rather than lossy.
     */
    fun loadSession(): User? {
        val encrypted = prefs.getString(KEY_SESSION, null) ?: return null
        val json = crypto.decrypt(encrypted) ?: return null

        return try {
            val obj = JSONObject(json)
            User(
                id = obj.getString(FIELD_ID),
                name = obj.getString(FIELD_NAME),
                email = obj.getString(FIELD_EMAIL),
                isEmailVerified = obj.optBoolean(FIELD_VERIFIED, false)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Stores an opaque auth/refresh token, encrypted.
     * Unused by the Firebase path (see the class comment); present for the
     * custom-backend path the spec also allows.
     */
    fun saveToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, crypto.encrypt(token)) }
    }

    /** Reads back the token saved by [saveToken], or null. */
    fun loadToken(): String? =
        prefs.getString(KEY_TOKEN, null)?.let { crypto.decrypt(it) }

    /**
     * Wipes all local authentication state (spec 3.5, 5: "Logout shall clear
     * all locally stored authentication information").
     *
     * `clear()` removes every key in this preferences file, so a field added
     * later is covered automatically and cannot be forgotten here.
     */
    fun clear() {
        prefs.edit { clear() }
    }
}
