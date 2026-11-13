package com.neuroverse.authapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neuroverse.authapp.ui.forgot.ForgotPasswordScreen
import com.neuroverse.authapp.ui.home.HomeScreen
import com.neuroverse.authapp.ui.login.LoginScreen
import com.neuroverse.authapp.ui.profile.ProfileScreen
import com.neuroverse.authapp.ui.register.RegisterScreen
import com.neuroverse.authapp.ui.session.SessionViewModel
import com.neuroverse.authapp.ui.welcome.WelcomeScreen

/**
 * The navigation graph, and the app's single security boundary for screen
 * access (spec 2 "Protect screens that require authentication", 3.4 "Redirect
 * unauthenticated users to the Login Screen").
 *
 * HOW PROTECTION WORKS HERE
 * -------------------------
 * There is no `if (loggedIn)` check inside Home or Profile. Instead this graph
 * watches [SessionViewModel.authState] and reacts:
 *   - becomes SignedOut -> pop everything, go to Welcome
 *   - becomes SignedIn  -> pop everything, go to Home
 *
 * The key is `popUpTo(0) { inclusive = true }`, which CLEARS THE ENTIRE BACK
 * STACK. Without it, pressing the system Back button after logging out would
 * return to Home with a dead session — the single most common way an auth flow
 * leaks a protected screen. With the stack cleared there is physically nothing
 * behind the Login screen to go back to.
 */
@Composable
fun AuthNavGraph(
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory)
) {
    val authState by sessionViewModel.authState.collectAsStateWithLifecycle()

    // --- Startup: show a spinner until the session has been restored --------
    // Rendering the graph before we know the answer is what causes the Login
    // screen to flash for returning users.
    if (authState is SessionViewModel.AuthState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isSignedIn = authState is SessionViewModel.AuthState.SignedIn

    /**
     * The guard. Keyed on [isSignedIn] so it runs exactly once per transition,
     * not on every recomposition.
     */
    LaunchedEffect(isSignedIn) {
        val target = if (isSignedIn) Destinations.HOME else Destinations.WELCOME
        val current = navController.currentDestination?.route

        // Already in the right section? Do nothing. Navigating to the screen we
        // are already on would rebuild it and lose its state.
        val alreadyCorrect = when {
            isSignedIn -> current == Destinations.HOME || current == Destinations.PROFILE
            else -> current in setOf(
                Destinations.WELCOME, Destinations.LOGIN,
                Destinations.REGISTER, Destinations.FORGOT_PASSWORD
            )
        }

        if (!alreadyCorrect) {
            navController.navigate(target) {
                // Wipe the whole stack: nothing to Back into. See class comment.
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        // Start in the right place from the first frame, rather than starting at
        // Welcome and immediately redirecting.
        startDestination = if (isSignedIn) Destinations.HOME else Destinations.WELCOME
    ) {

        // ---------------- Public (signed-out) destinations ------------------

        composable(Destinations.WELCOME) {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate(Destinations.LOGIN) },
                onNavigateToRegister = { navController.navigate(Destinations.REGISTER) }
            )
        }

        composable(Destinations.LOGIN) {
            LoginScreen(
                // No navigate() call here: logging in changes authState, and the
                // guard above does the routing. One mechanism, not two.
                onLoginSuccess = { },
                onNavigateToRegister = {
                    navController.navigate(Destinations.REGISTER) {
                        // Replace Login rather than stacking Register on top of
                        // it, so Login <-> Register toggling cannot build a
                        // 20-deep back stack.
                        popUpTo(Destinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Destinations.FORGOT_PASSWORD)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.REGISTER) {
            RegisterScreen(
                onRegistrationSuccess = { },   // handled by the guard
                onNavigateToLogin = {
                    navController.navigate(Destinations.LOGIN) {
                        popUpTo(Destinations.REGISTER) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        // ---------------- Protected (signed-in) destinations ----------------

        composable(Destinations.HOME) {
            // Read the state fresh here. During the brief moment after logout
            // and before the guard navigates away, this is null — rendering
            // nothing is correct, and is also why Home takes a non-null User
            // and can never show a blank or placeholder profile.
            val user = (authState as? SessionViewModel.AuthState.SignedIn)?.user
            if (user != null) {
                HomeScreen(
                    user = user,
                    providerName = sessionViewModel.providerName,
                    onNavigateToProfile = { navController.navigate(Destinations.PROFILE) },
                    onLogout = sessionViewModel::logout
                )
            }
        }

        composable(Destinations.PROFILE) {
            val user = (authState as? SessionViewModel.AuthState.SignedIn)?.user
            if (user != null) {
                ProfileScreen(
                    user = user,
                    providerName = sessionViewModel.providerName,
                    onBack = { navController.popBackStack() },
                    onLogout = sessionViewModel::logout
                )
            }
        }
    }
}
