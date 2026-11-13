package com.neuroverse.authapp.data.model

/**
 * The result of any authentication call: either it worked, or it failed with a
 * typed [AuthError].
 *
 * Java comparison: in Java you would throw a checked exception, or return null
 * and set an error field. Both are easy to ignore. Here the ONLY way to read the
 * value is to deal with both cases, because `Success` and `Failure` are
 * different types.
 *
 * `out T` makes this covariant: an AuthOutcome<User> is usable where an
 * AuthOutcome<Any> is expected, the same as Java's `? extends T` wildcard.
 */
sealed interface AuthOutcome<out T> {

    data class Success<T>(val data: T) : AuthOutcome<T>

    data class Failure(val error: AuthError) : AuthOutcome<Nothing>

    /**
     * `Nothing` is Kotlin's bottom type: it has no instances and is a subtype of
     * every other type. That single trick lets one Failure class satisfy
     * AuthOutcome<User>, AuthOutcome<Unit>, AuthOutcome<anything> at once, with
     * no generic parameter of its own.
     */
    companion object
}

/** True when this outcome succeeded. Convenience for `is` checks in UI code. */
val AuthOutcome<*>.isSuccess: Boolean get() = this is AuthOutcome.Success

/** The error, or null when this outcome succeeded. */
val AuthOutcome<*>.errorOrNull: AuthError?
    get() = (this as? AuthOutcome.Failure)?.error
