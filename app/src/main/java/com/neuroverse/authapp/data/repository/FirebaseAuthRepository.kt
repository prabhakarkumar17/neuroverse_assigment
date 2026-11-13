package com.neuroverse.authapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.neuroverse.authapp.data.model.AuthError
import com.neuroverse.authapp.data.model.AuthOutcome
import com.neuroverse.authapp.data.model.User
import com.neuroverse.authapp.data.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * [AuthRepository] backed by Firebase Authentication (spec 4).
 *
 * Active only when `app/google-services.json` is present — see
 * [com.neuroverse.authapp.di.ServiceLocator].
 *
 * WHAT FIREBASE GIVES US THAT WE WOULD OTHERWISE HAVE TO BUILD
 * -----------------------------------------------------------
 *  - Server-side scrypt password hashing (spec 5, last bullet)
 *  - Real password-reset emails with single-use expiring links (spec 3.3)
 *  - Atomic duplicate-email rejection (spec 3.1) — a client-side "is this
 *    email taken?" check can always lose a race between check and insert
 *  - HTTPS transport for everything (spec 5)
 *  - Token refresh and revocation
 *
 * HOW ERRORS ARE HANDLED
 * ----------------------
 * Firebase throws a dozen exception subclasses. We translate them into our own
 * small [AuthError] set in [mapException] so that nothing Firebase-shaped ever
 * reaches a ViewModel or the UI. That is what keeps this swappable, and it is
 * also a security control: raw provider messages can disclose account state.
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val sessionStore: SessionStore
) : AuthRepository {

    override val providerName: String = "Firebase Authentication"

    // Private mutable state, public read-only view. Callers can observe but not
    // modify — the Kotlin equivalent of exposing Collections.unmodifiableList().
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): AuthOutcome<User> = runAuth {
        val result = firebaseAuth
            .createUserWithEmailAndPassword(email.trim(), password)
            .await()

        val firebaseUser = result.user
            ?: return@runAuth AuthOutcome.Failure(AuthError.Unknown())

        // Firebase stores the display name on the user profile, separate from
        // account creation, so this is a second call.
        firebaseUser.updateProfile(
            userProfileChangeRequest { displayName = name.trim() }
        ).await()

        // Send a verification email. Failure here must NOT fail registration —
        // the account exists and the user can sign in; they can request another
        // verification mail later.
        runCatching { firebaseUser.sendEmailVerification().await() }

        val user = firebaseUser.toUser(fallbackName = name.trim())
        persist(user)
        AuthOutcome.Success(user)
    }

    override suspend fun login(email: String, password: String): AuthOutcome<User> = runAuth {
        val result = firebaseAuth
            .signInWithEmailAndPassword(email.trim(), password)
            .await()

        val firebaseUser = result.user
            ?: return@runAuth AuthOutcome.Failure(AuthError.Unknown())

        val user = firebaseUser.toUser()
        persist(user)
        AuthOutcome.Success(user)
    }

    override suspend fun sendPasswordReset(email: String): AuthOutcome<Unit> = runAuth {
        try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            // Account does not exist. Swallow it and report success anyway so
            // this screen cannot be used to enumerate registered addresses.
            // See the contract note on AuthRepository.sendPasswordReset.
        }
        AuthOutcome.Success(Unit)
    }

    override suspend fun logout() {
        firebaseAuth.signOut()   // clears Firebase's own token cache
        sessionStore.clear()     // clears ours (spec 3.5)
        _currentUser.value = null
    }

    override suspend fun restoreSession(): User? {
        // Firebase is the source of truth: it persists its own session and will
        // already have rehydrated it by the time we get here.
        val firebaseUser = firebaseAuth.currentUser

        if (firebaseUser == null) {
            // Signed out at the provider. Our cache must not outlive it.
            sessionStore.clear()
            _currentUser.value = null
            return null
        }

        // Prefer our cached copy for the display name: reload() needs the
        // network, and the name is cosmetic. Falls back to the live profile.
        val cached = sessionStore.loadSession()
        val user = firebaseUser.toUser(fallbackName = cached?.name ?: "")
        persist(user)
        return user
    }

    // ---------------------------------------------------------------- helpers

    private fun persist(user: User) {
        sessionStore.saveSession(user)
        _currentUser.value = user
    }

    /**
     * Maps a [FirebaseUser] onto our own [User].
     *
     * Firebase's displayName is null for accounts created before a profile
     * update lands, so we fall back to a cached name and finally to the email's
     * local part, which is always better than showing "null".
     */
    private fun FirebaseUser.toUser(fallbackName: String = ""): User {
        val resolvedName = displayName?.takeIf { it.isNotBlank() }
            ?: fallbackName.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')
            ?: "User"

        return User(
            id = uid,
            name = resolvedName,
            email = email.orEmpty(),
            isEmailVerified = isEmailVerified
        )
    }

    /**
     * Runs [block], converting any thrown exception into an [AuthOutcome.Failure].
     *
     * KOTLIN NOTE: the parameter is a `suspend` lambda, so `block()` can itself
     * call suspend functions like `.await()`. `inline` is deliberately NOT used
     * here because non-local returns from a suspend lambda inside try/catch get
     * subtle; clarity wins over one avoided allocation.
     */
    private suspend fun <T> runAuth(
        block: suspend () -> AuthOutcome<T>
    ): AuthOutcome<T> = try {
        block()
    } catch (e: Exception) {
        AuthOutcome.Failure(mapException(e))
    }

    /** Translates Firebase's exception zoo into our own vocabulary. */
    private fun mapException(e: Exception): AuthError = when (e) {
        is FirebaseAuthWeakPasswordException     -> AuthError.WeakPassword
        is FirebaseAuthUserCollisionException    -> AuthError.EmailAlreadyRegistered
        is FirebaseTooManyRequestsException      -> AuthError.TooManyRequests

        // Both of these mean "those credentials did not work". Firebase
        // distinguishes "no such user" from "wrong password"; we deliberately
        // do not, so login cannot be used to test whether an email is registered.
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials

        is IOException                           -> AuthError.Network
        else                                     -> AuthError.Unknown(e)
    }
}
