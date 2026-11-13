// ============================================================================
//  APP MODULE BUILD FILE
// ============================================================================
plugins {
    alias(libs.plugins.android.application)  // Makes this an Android app. Kotlin support is BUILT IN since AGP 9.
    alias(libs.plugins.kotlin.compose)       // Jetpack Compose compiler, and pins the Kotlin version.
}

// ---------------------------------------------------------------------------
//  Firebase is OPTIONAL at build time.
//  The google-services plugin FAILS the build if google-services.json is
//  missing, so we only apply it when the file is actually present. That lets
//  the project build and run out of the box, and switch to real Firebase the
//  moment you drop the file in. See README "Enabling Firebase".
// ---------------------------------------------------------------------------
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

android {
    namespace = "com.neuroverse.authapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.neuroverse.authapp"
        minSdk = 26          // Android 8.0. Required for the Keystore AES/GCM APIs we use.
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Lets a debug and a release build live on the same device at once.
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true      // R8: strip unused code
            isShrinkResources = true    // R8: strip unused resources
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            // Several libraries ship identical licence files; drop the duplicates.
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Since AGP 9 the Kotlin compiler is configured here, NOT in android.kotlinOptions.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // --- Android framework niceties -----------------------------------------
    implementation(libs.androidx.core.ktx)

    // --- Lifecycle / MVVM ---------------------------------------------------
    implementation(libs.androidx.lifecycle.runtime.ktx)       // lifecycleScope
    implementation(libs.androidx.lifecycle.viewmodel)         // viewModel() in Compose
    implementation(libs.androidx.lifecycle.runtime.compose)   // collectAsStateWithLifecycle()

    // --- Compose UI ---------------------------------------------------------
    implementation(platform(libs.androidx.compose.bom))       // BOM first: pins all versions below
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- Coroutines (all async work) ----------------------------------------
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)             // Task<T>.await() for Firebase

    // --- Firebase Authentication --------------------------------------------
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // --- Debug-only tooling --------------------------------------------------
    debugImplementation(libs.androidx.ui.tooling)             // @Preview pane, Layout Inspector

    // --- Unit tests (plain JVM, no device needed) ----------------------------
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
