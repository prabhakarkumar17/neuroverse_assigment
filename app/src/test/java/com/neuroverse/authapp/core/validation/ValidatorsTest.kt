package com.neuroverse.authapp.core.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers spec test scenario 5: "Handle invalid email and password inputs".
 *
 * These are pure JVM tests — no Android, no emulator. They run in milliseconds,
 * which is only possible because Validators imports nothing from the framework.
 *
 * Kotlin note: backtick-quoted method names are legal and standard in Kotlin
 * tests. They make a failing test read as a sentence in the report.
 */
class ValidatorsTest {

    // ----------------------------------------------------------- email

    @Test
    fun `valid email addresses are accepted`() {
        listOf(
            "user@example.com",
            "first.last@example.co.uk",
            "user+tag@example.org",
            "user_name@sub.domain.example.com",
            "u@e.io"
        ).forEach { email ->
            assertTrue("expected '$email' to be valid", Validators.validateEmail(email).isValid)
        }
    }

    @Test
    fun `invalid email addresses are rejected`() {
        listOf(
            "",                 // empty
            "   ",              // whitespace only
            "notanemail",       // no @
            "@example.com",     // no local part
            "user@",            // no domain
            "user@example",     // no TLD
            "user@.com",        // empty domain label
            "user name@e.com"   // space in local part
        ).forEach { email ->
            assertFalse("expected '$email' to be invalid", Validators.validateEmail(email).isValid)
        }
    }

    @Test
    fun `email is trimmed before validation`() {
        // Users paste addresses with trailing spaces constantly.
        assertTrue(Validators.validateEmail("  user@example.com  ").isValid)
    }

    @Test
    fun `empty email reports a required message`() {
        assertEquals("Email is required.", Validators.validateEmail("").errorMessage)
    }

    // -------------------------------------------------------- password

    @Test
    fun `password meeting all rules is accepted`() {
        assertTrue(Validators.validatePassword("password1").isValid)
        assertTrue(Validators.validatePassword("Str0ngPassphrase").isValid)
    }

    @Test
    fun `password shorter than minimum is rejected`() {
        assertFalse(Validators.validatePassword("pass1").isValid)
        // Boundary: exactly one below the limit must fail...
        assertFalse(Validators.validatePassword("passwor").isValid)
        // ...and exactly at the limit must pass.
        assertTrue(Validators.validatePassword("passwor1").isValid)
    }

    @Test
    fun `password without a digit is rejected`() {
        assertFalse(Validators.validatePassword("passwordonly").isValid)
    }

    @Test
    fun `password without a letter is rejected`() {
        assertFalse(Validators.validatePassword("12345678").isValid)
    }

    @Test
    fun `password is not trimmed`() {
        // A space is a legitimate password character. Silently stripping it
        // would lock the user out when they later type it correctly.
        assertTrue(Validators.validatePassword(" passwor1").isValid)
    }

    @Test
    fun `login only requires a password to be present`() {
        // Crucially NOT the registration rules — see Validators for why.
        assertTrue(Validators.validatePasswordPresence("x").isValid)
        assertFalse(Validators.validatePasswordPresence("").isValid)
    }

    // ---------------------------------------------------- confirmation

    @Test
    fun `matching confirmation is accepted`() {
        assertTrue(Validators.validatePasswordConfirmation("password1", "password1").isValid)
    }

    @Test
    fun `mismatched confirmation is rejected`() {
        val result = Validators.validatePasswordConfirmation("password1", "password2")
        assertFalse(result.isValid)
        assertEquals("Passwords do not match.", result.errorMessage)
    }

    @Test
    fun `empty confirmation is rejected`() {
        assertFalse(Validators.validatePasswordConfirmation("password1", "").isValid)
    }

    // ------------------------------------------------------------ name

    @Test
    fun `valid names are accepted`() {
        assertTrue(Validators.validateName("Prabhakar Kumar").isValid)
        assertTrue(Validators.validateName("Jo").isValid)   // boundary: exactly 2
    }

    @Test
    fun `invalid names are rejected`() {
        assertFalse(Validators.validateName("").isValid)
        assertFalse(Validators.validateName("   ").isValid)       // whitespace only
        assertFalse(Validators.validateName("J").isValid)         // boundary: 1 char
        assertFalse(Validators.validateName("x".repeat(51)).isValid)  // over the max
    }
}
