package com.neuroverse.authapp.data.repository

import com.neuroverse.authapp.data.model.AuthOutcome
import com.neuroverse.authapp.data.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * THE most important type in this app.
 *
 * Spec section 9 requires that "authentication logic shall be kept separate from
 * the user interface". This interface IS that separation. The UI and the
 * ViewModels depend only on this; they contain no reference to Firebase, to the
 * Keystore, or to any storage API. Swapping the entire authentication backend
 * means writing one new class that implements this interface and changing one
 * line in [com.neuroverse.authapp.di.ServiceLocator].
 *
 * Two implementations ship:
 *   - [FirebaseAuthRepository] — real Firebase, used when google-services.json exists
 *   - [LocalAuthRepository]    — offline, device-only, so the app always runs
 * Tests use a third (FakeAuthRepository) with no Android dependencies at all.
 *
 * KOTLIN NOTE — `suspend`
 * -----------------------
 * Every network call below is a `suspend` function. In Java you would return a
 * Future, or take a callback, and end up nesting callbacks three deep for a
 * register-then-login-then-fetch sequence. A suspend function is written and
 * read top-to-bottom like blocking code, but does not block the thread: at each
 * suspension point the coroutine releases its thread and resumes later. The
 * compiler rewrites the function into a state machine to do it.
 *
 * The signature is also a CONTRACT: `suspend` tells every caller "this may take
 * time, you must call me from a coroutine". You cannot accidentally run it on
 * the main thread from ordinary code — that is a compile error, not a runtime
 * ANR.
 */
interface AuthRepository {

    /**
     * The currently signed-in user, or null when signed out.
     *
     * A [StateFlow] rather than a plain getter: it is observable and always has
     * a current value. The UI collects it and recomposes automatically whenever
     * auth state changes, so a logout triggered anywhere (including by the
     * provider expiring the session) navigates every screen without the
     * ViewModels having to notify each other.
     */
    val currentUser: StateFlow<User?>

    /** Registers a new account (spec 3.1). Fails with EmailAlreadyRegistered if taken. */
    suspend fun register(name: String, email: String, password: String): AuthOutcome<User>

    /** Signs in an existing user (spec 3.2). Fails with InvalidCredentials. */
    suspend fun login(email: String, password: String): AuthOutcome<User>

    /**
     * Sends a password-reset message (spec 3.3).
     *
     * Succeeds even when no account exists for [email]. That is deliberate, not
     * a bug: replying "no such account" turns this screen into a free tool for
     * discovering which emails are registered. The user sees the same
     * confirmation either way.
     */
    suspend fun sendPasswordReset(email: String): AuthOutcome<Unit>

    /** Signs out and clears all local session data (spec 3.5). */
    suspend fun logout()

    /**
     * Restores a previously saved session at startup (spec 3.4).
     * Returns the restored user, or null if there was nothing valid to restore.
     */
    suspend fun restoreSession(): User?

    /** Human-readable name of the active backend, shown on the Home screen. */
    val providerName: String
}
