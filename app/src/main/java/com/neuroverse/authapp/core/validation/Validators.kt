package com.neuroverse.authapp.core.validation

/**
 * Client-side input validation (spec 2 "Validate user inputs", 3.1, 3.2).
 *
 * This file is deliberately PURE KOTLIN: it imports nothing from Android. That
 * is what lets it be unit-tested on the JVM in milliseconds with no emulator
 * (see ValidatorsTest). Anything that touches android.util.Patterns would drag
 * in the framework and force a slow instrumented test instead.
 *
 * Spec section 5 requires validation "on both the client and server sides".
 * This is the CLIENT half and it is a UX convenience only — it gives instant
 * feedback. The authentication provider re-validates everything server-side,
 * and that is the half that actually enforces the rules. Never treat passing
 * client validation as a security guarantee.
 */

/** Outcome of validating one field. */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult

    val errorMessage: String? get() = (this as? Invalid)?.message
    val isValid: Boolean get() = this is Valid
}

object Validators {

    /** Minimum password length. Firebase itself enforces 6; we require more. */
    const val MIN_PASSWORD_LENGTH = 8

    /** Longest name we accept, to bound storage and UI layout. */
    const val MAX_NAME_LENGTH = 50

    /**
     * Pragmatic email pattern: something, an @, a dotted domain, a 2+ letter TLD.
     *
     * Deliberately NOT fully RFC 5322 compliant — that regex is thousands of
     * characters and still accepts addresses no mail server would deliver to.
     * The only true test of an email address is sending mail to it, which is
     * exactly what the verification and reset flows do.
     *
     * Kotlin note: `"""..."""` is a raw string. No backslash escaping, so the
     * regex reads as written. In Java this needs every \ doubled.
     */
    private val EMAIL_REGEX = Regex(
        """^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$"""
    )

    /** Validates a display name (spec 3.1: name is required at registration). */
    fun validateName(raw: String): ValidationResult {
        val name = raw.trim()
        return when {
            name.isEmpty() ->
                ValidationResult.Invalid("Name is required.")
            name.length < 2 ->
                ValidationResult.Invalid("Name must be at least 2 characters.")
            name.length > MAX_NAME_LENGTH ->
                ValidationResult.Invalid("Name must be $MAX_NAME_LENGTH characters or fewer.")
            else -> ValidationResult.Valid
        }
    }

    /** Validates an email address (spec 2, 3.1, 3.2). */
    fun validateEmail(raw: String): ValidationResult {
        val email = raw.trim()
        return when {
            email.isEmpty() ->
                ValidationResult.Invalid("Email is required.")
            !EMAIL_REGEX.matches(email) ->
                ValidationResult.Invalid("Enter a valid email address.")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates a password for REGISTRATION.
     *
     * Note it is not `.trim()`ed: a leading or trailing space is a legitimate
     * part of a password, and silently stripping it would lock the user out
     * later when they type it correctly.
     */
    fun validatePassword(password: String): ValidationResult = when {
        password.isEmpty() ->
            ValidationResult.Invalid("Password is required.")
        password.length < MIN_PASSWORD_LENGTH ->
            ValidationResult.Invalid("Password must be at least $MIN_PASSWORD_LENGTH characters.")
        password.none { it.isDigit() } ->
            ValidationResult.Invalid("Password must contain at least one number.")
        password.none { it.isLetter() } ->
            ValidationResult.Invalid("Password must contain at least one letter.")
        else -> ValidationResult.Valid
    }

    /**
     * Validates a password for LOGIN — presence only.
     *
     * Applying the registration strength rules here would be a bug AND a leak:
     * an account created before the rules tightened could never sign in, and
     * "password too short" on a login screen tells an attacker their guess was
     * rejected by format rather than by the credential check.
     */
    fun validatePasswordPresence(password: String): ValidationResult =
        if (password.isEmpty()) ValidationResult.Invalid("Password is required.")
        else ValidationResult.Valid

    /** Validates that a confirmation field matches the original password. */
    fun validatePasswordConfirmation(password: String, confirmation: String): ValidationResult =
        when {
            confirmation.isEmpty() -> ValidationResult.Invalid("Please confirm your password.")
            confirmation != password -> ValidationResult.Invalid("Passwords do not match.")
            else -> ValidationResult.Valid
        }
}
