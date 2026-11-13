package com.neuroverse.authapp.fake

import com.neuroverse.authapp.data.model.AuthError
import com.neuroverse.authapp.data.model.AuthOutcome
import com.neuroverse.authapp.data.model.User
import com.neuroverse.authapp.data.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [AuthRepository] for tests.
 *
 * THIS CLASS IS THE PAYOFF FOR THE REPOSITORY INTERFACE. Because the ViewModels
 * depend on `AuthRepository` and not on Firebase, the entire authentication
 * layer can be replaced with 80 lines of plain Kotlin. These tests need no
 * emulator, no network, no Firebase project, and run in milliseconds.
 *
 * It also lets tests force conditions that are hard to produce for real — a
 * network failure, or rate limiting — via [nextError].
 */
class FakeAuthRepository : AuthRepository {

    override val providerName: String = "Fake"

    /** email (lowercased) -> password */
    private val accounts = mutableMapOf<String, String>()

    /** email (lowercased) -> display name */
    private val names = mutableMapOf<String, String>()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    /** Set this to force the NEXT call to fail with a specific error. */
    var nextError: AuthError? = null

    /**
     * Optional gate that suspends register/login until the test completes it.
     *
     * WHY: a fake with no suspension point starts and finishes inside a single
     * dispatcher step, so a test can never observe the in-flight state. Holding
     * this open lets a test assert "the button is disabled while loading" —
     * which is the behaviour that prevents double-submission — and only then
     * let the call finish. See `openGate`/`releaseGate`.
     */
    private var gate: CompletableDeferred<Unit>? = null

    /** Makes the next register/login hang until [releaseGate] is called. */
    fun openGate() {
        gate = CompletableDeferred()
    }

    /** Lets a gated call proceed to completion. */
    fun releaseGate() {
        gate?.complete(Unit)
        gate = null
    }

    /** Suspends while a gate is open. No-op when there is none. */
    private suspend fun awaitGate() {
        gate?.await()
    }

    /** Records how many reset emails were requested, for assertions. */
    var passwordResetRequests: Int = 0
        private set

    /** Pre-seeds an account, for "user already exists" tests. */
    fun seedAccount(name: String, email: String, password: String) {
        accounts[email.lowercase()] = password
        names[email.lowercase()] = name
    }

    /** Consumes and returns a forced error, if one was set. */
    private fun takeForcedError(): AuthError? = nextError.also { nextError = null }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): AuthOutcome<User> {
        awaitGate()
        takeForcedError()?.let { return AuthOutcome.Failure(it) }

        val key = email.trim().lowercase()
        if (accounts.containsKey(key)) {
            return AuthOutcome.Failure(AuthError.EmailAlreadyRegistered)
        }

        accounts[key] = password
        names[key] = name.trim()

        val user = User(id = "id-$key", name = name.trim(), email = email.trim())
        _currentUser.value = user
        return AuthOutcome.Success(user)
    }

    override suspend fun login(email: String, password: String): AuthOutcome<User> {
        awaitGate()
        takeForcedError()?.let { return AuthOutcome.Failure(it) }

        val key = email.trim().lowercase()
        // Note: a missing account and a wrong password are the same failure,
        // mirroring the real repositories' non-disclosure behaviour.
        if (accounts[key] != password) {
            return AuthOutcome.Failure(AuthError.InvalidCredentials)
        }

        val user = User(id = "id-$key", name = names[key].orEmpty(), email = email.trim())
        _currentUser.value = user
        return AuthOutcome.Success(user)
    }

    override suspend fun sendPasswordReset(email: String): AuthOutcome<Unit> {
        takeForcedError()?.let { return AuthOutcome.Failure(it) }
        passwordResetRequests++
        // Succeeds whether or not the account exists, by design.
        return AuthOutcome.Success(Unit)
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override suspend fun restoreSession(): User? = _currentUser.value

    /** Simulates "the app was killed and reopened with a saved session". */
    fun simulateSavedSession(user: User) {
        _currentUser.value = user
    }
}
