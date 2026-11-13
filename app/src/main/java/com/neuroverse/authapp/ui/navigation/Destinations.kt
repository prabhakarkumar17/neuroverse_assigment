package com.neuroverse.authapp.ui.navigation

/**
 * Every screen in the app, as a closed set of route strings.
 *
 * WHY AN OBJECT OF CONSTANTS AND NOT RAW STRINGS AT THE CALL SITE
 * --------------------------------------------------------------
 * Navigation-Compose matches destinations by string. A typo in `navigate("hme")`
 * compiles perfectly and crashes at runtime. Routing every navigate() call
 * through these constants turns that class of bug into a compile error.
 */
object Destinations {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val PROFILE = "profile"
}
