package com.neuroverse.authapp.data.model

/**
 * Every way authentication can fail, as a CLOSED set of types.
 *
 * Java comparison: you would reach for an `enum` here, but an enum cannot carry
 * different data per constant. A Kotlin `sealed interface` can: it is a type
 * hierarchy the compiler knows the complete membership of, so a `when` over it
 * needs no `else` branch. Add a new error type and every `when` that handles
 * AuthError stops compiling until you handle it too. That is the point.
 *
 * SECURITY (spec section 5): these are deliberately COARSE. We never tell the
 * caller "that email exists but the password is wrong" on login, because that
 * confirms which emails are registered to an attacker. Login failures all
 * collapse into [InvalidCredentials].
 */
sealed interface AuthError {

    /** The human-readable message shown to the user. Safe to display. */
    val message: String

    /** Email/password combination did not match. Intentionally vague. */
    data object InvalidCredentials : AuthError {
        override val message = "Incorrect email or password."
    }

    /** Registration blocked because the email is taken (spec 3.1). */
    data object EmailAlreadyRegistered : AuthError {
        override val message = "An account with this email already exists."
    }

    /** No account for this email. Only used where disclosure is already implied. */
    data object AccountNotFound : AuthError {
        override val message = "No account found with this email."
    }

    /** Provider rejected the password as too weak. */
    data object WeakPassword : AuthError {
        override val message = "Password is too weak. Use at least 8 characters."
    }

    /** Device offline or request timed out (spec test scenario 10). */
    data object Network : AuthError {
        override val message = "Network unavailable. Check your connection and try again."
    }

    /** Too many attempts; the provider is rate-limiting us. */
    data object TooManyRequests : AuthError {
        override val message = "Too many attempts. Please wait a moment and try again."
    }

    /** A client-side field failed validation. Carries its own text. */
    data class Validation(override val message: String) : AuthError

    /**
     * Anything we did not anticipate.
     *
     * `message` is a generic string on purpose: the real exception text may leak
     * internals, so it is logged, never shown. The `cause` is kept for logging.
     */
    data class Unknown(val cause: Throwable? = null) : AuthError {
        override val message = "Something went wrong. Please try again."
    }
}
