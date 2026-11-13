package com.neuroverse.authapp.data.model

/**
 * An authenticated user, as the rest of the app sees them.
 *
 * Java comparison: this one line replaces a POJO with a constructor, six
 * getters, equals(), hashCode() and toString(). A Kotlin `data class` generates
 * all of those from the primary constructor.
 *
 * NOTE what is NOT here: no password, no password hash, no raw auth token.
 * Those never travel into the UI layer (spec section 5). The token lives only
 * inside the session store, behind the Keystore.
 */
data class User(
    /** Stable unique id. Firebase calls this the "uid". */
    val id: String,

    /** Display name collected at registration (spec 3.1). */
    val name: String,

    /** Email address, also the login identifier. */
    val email: String,

    /** Whether the provider has confirmed the address via a verification link. */
    val isEmailVerified: Boolean = false
) {
    /**
     * First letter of the name, for the avatar circle on the profile screen.
     * A computed property: no backing field, recalculated on each read — the
     * equivalent of a Java getter with no instance variable behind it.
     */
    val initial: String
        get() = name.trim().firstOrNull()?.uppercase() ?: "?"
}
