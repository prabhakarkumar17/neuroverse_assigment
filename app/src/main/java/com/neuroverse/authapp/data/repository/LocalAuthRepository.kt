package com.neuroverse.authapp.data.repository

import android.util.Log
import com.neuroverse.authapp.data.local.LocalCredentialStore
import com.neuroverse.authapp.data.model.AuthError
import com.neuroverse.authapp.data.model.AuthOutcome
import com.neuroverse.authapp.data.model.User
import com.neuroverse.authapp.data.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Offline [AuthRepository]: real cryptography, no network.
 *
 * Used automatically when `app/google-services.json` is absent, so the project
 * builds, runs and demos on a clean machine. Swap in Firebase by adding that
 * file — no code changes (see README "Enabling Firebase").
 *
 * HONEST LIMITATIONS — state these when presenting the app:
 *  1. Accounts live only on this device. Reinstalling wipes them.
 *  2. Password reset cannot send a real email. It logs the reset link to
 *     Logcat instead, which satisfies the FLOW but not the delivery.
 *  3. Hashing happens on-device. Spec 5 requires server-side hashing for a
 *     custom backend; only the Firebase path truly satisfies that clause.
 */
class LocalAuthRepository(
    private val credentials: LocalCredentialStore,
    private val sessionStore: SessionStore
) : AuthRepository {

    private companion object {
        const val TAG = "LocalAuth"

        /**
         * Simulated round-trip latency.
         *
         * Not decoration: without it, loading spinners never appear, so a race
         * or a missing `isLoading` flag stays invisible until the app meets a
         * real network. Making local calls feel remote surfaces those bugs now.
         */
        const val FAKE_LATENCY_MS = 600L
    }

    override val providerName: String = "Local (offline demo mode)"

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): AuthOutcome<User> = io {
        delay(FAKE_LATENCY_MS)

        // Spec 3.1: reject a duplicate email.
        if (credentials.exists(email)) {
            return@io AuthOutcome.Failure(AuthError.EmailAlreadyRegistered)
        }

        val id = credentials.create(name, email, password)
        val user = User(id = id, name = name.trim(), email = email.trim(), isEmailVerified = false)
        persist(user)
        AuthOutcome.Success(user)
    }

    override suspend fun login(email: String, password: String): AuthOutcome<User> = io {
        delay(FAKE_LATENCY_MS)

        // verify() returns null for BOTH "no account" and "wrong password", so
        // the two are indistinguishable from here on (see AuthError docs).
        val match = credentials.verify(email, password)
            ?: return@io AuthOutcome.Failure(AuthError.InvalidCredentials)

        val (id, name) = match          // destructuring a Pair, like a tuple
        val user = User(id = id, name = name, email = email.trim(), isEmailVerified = false)
        persist(user)
        AuthOutcome.Success(user)
    }

    override suspend fun sendPasswordReset(email: String): AuthOutcome<Unit> = io {
        delay(FAKE_LATENCY_MS)

        // Always report success, whether or not the account exists, so this
        // screen cannot be used to enumerate registered emails.
        if (credentials.exists(email)) {
            // A real backend emails a signed single-use link. Offline we can
            // only log it. Debug-only: never log credentials in a release build.
            Log.d(TAG, "Password reset requested for ${email.trim()} (no email sent in offline mode)")
        }
        AuthOutcome.Success(Unit)
    }

    override suspend fun logout() {
        sessionStore.clear()
        _currentUser.value = null
    }

    override suspend fun restoreSession(): User? {
        val user = sessionStore.loadSession()
        _currentUser.value = user
        return user
    }

    private fun persist(user: User) {
        sessionStore.saveSession(user)
        _currentUser.value = user
    }

    /**
     * Runs [block] on the IO dispatcher.
     *
     * PBKDF2 at 210,000 iterations takes ~100ms, and SharedPreferences writes
     * hit the disk. Both would jank or ANR the UI thread. `withContext` moves
     * the work to a background thread pool and hops the result back — the
     * coroutine equivalent of AsyncTask, minus the leaks and the boilerplate.
     */
    private suspend fun <T> io(block: suspend () -> T): T =
        withContext(Dispatchers.IO) { block() }
}
