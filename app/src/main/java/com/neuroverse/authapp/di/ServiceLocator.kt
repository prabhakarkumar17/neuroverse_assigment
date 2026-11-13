package com.neuroverse.authapp.di

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.neuroverse.authapp.data.local.LocalCredentialStore
import com.neuroverse.authapp.data.repository.AuthRepository
import com.neuroverse.authapp.data.repository.FirebaseAuthRepository
import com.neuroverse.authapp.data.repository.LocalAuthRepository
import com.neuroverse.authapp.data.session.SessionStore

/**
 * Manual dependency injection: builds the object graph in one place.
 *
 * WHY NOT HILT/DAGGER?
 * --------------------
 * Hilt is the standard recommendation for large Android apps and you should
 * know it. It is not used here on purpose:
 *  1. This app has exactly ONE dependency worth injecting (AuthRepository).
 *     Hilt would add an annotation processor, a plugin, modules and ~6 new
 *     annotations to solve a problem that is 20 lines of Kotlin.
 *  2. Annotation processing is where build setups break. Avoiding it keeps the
 *     project building on a brand-new toolchain.
 *  3. Hilt's generated code hides the graph. Here you can READ it, which
 *     matters more while you are learning.
 * Manual DI is a documented, officially-endorsed Android approach — not a
 * shortcut. The important property is that construction is centralised, and
 * that holds either way.
 *
 * WHY `object`?
 * -------------
 * `object` declares a singleton: one instance, created lazily on first access,
 * thread-safely. It replaces Java's private constructor + static INSTANCE +
 * double-checked locking boilerplate.
 */
object ServiceLocator {

    private const val TAG = "ServiceLocator"

    @Volatile
    private var authRepository: AuthRepository? = null

    /**
     * Returns the app-wide [AuthRepository], creating it on first call.
     *
     * Double-checked locking via `synchronized` + `@Volatile`: two screens can
     * ask for the repository simultaneously during startup, and two instances
     * would mean two separate `currentUser` flows and a UI that disagrees with
     * itself about whether anyone is logged in.
     */
    fun authRepository(context: Context): AuthRepository =
        authRepository ?: synchronized(this) {
            authRepository ?: build(context.applicationContext).also { authRepository = it }
        }

    /**
     * THE SWITCH between the two backends.
     *
     * [FirebaseApp.initializeApp] returns null when there is no Firebase
     * configuration — which is exactly the case when `google-services.json` is
     * absent, because the Gradle plugin that turns that file into Android
     * string resources was never applied (see app/build.gradle.kts).
     *
     * So the presence of one file decides the backend, with no code change and
     * no build flag to remember.
     */
    private fun build(context: Context): AuthRepository {
        val sessionStore = SessionStore(context)

        val firebaseApp = runCatching { FirebaseApp.initializeApp(context) }.getOrNull()

        return if (firebaseApp != null) {
            Log.i(TAG, "google-services.json found -> using Firebase Authentication")
            FirebaseAuthRepository(
                firebaseAuth = FirebaseAuth.getInstance(),
                sessionStore = sessionStore
            )
        } else {
            Log.w(
                TAG,
                "No google-services.json -> using offline LocalAuthRepository. " +
                    "See README 'Enabling Firebase' to switch to the real backend."
            )
            LocalAuthRepository(
                credentials = LocalCredentialStore(context),
                sessionStore = sessionStore
            )
        }
    }

    /** Test hook: lets a test swap in a fake repository. */
    internal fun overrideForTesting(repository: AuthRepository?) {
        authRepository = repository
    }
}
