package com.neuroverse.authapp

import android.app.Application
import com.neuroverse.authapp.di.ServiceLocator

/**
 * Application class — the first object Android creates for this process, before
 * any Activity exists, and the last to be destroyed.
 *
 * Registered via `android:name=".AuthApplication"` in AndroidManifest.xml.
 *
 * Its only job here is to build the object graph eagerly, so the repository
 * (and therefore the Firebase-vs-offline decision) is ready before the first
 * screen asks for it.
 *
 * WARNING, and the most common Android memory leak: an Application instance
 * lives for the whole process. Never store an Activity, a View, or a Context
 * from one in here — it can never be garbage collected, so the entire screen
 * leaks for as long as the app runs.
 */
class AuthApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // `this` is the Application context: process-scoped, which is exactly
        // what the repository and its stores need.
        ServiceLocator.authRepository(this)
    }
}
