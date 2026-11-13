# NeuroVerse Authentication System

A secure email-and-password authentication app for Android, built in **Kotlin** with
**Jetpack Compose**, **MVVM**, **Coroutines** and **Android Keystore**-backed encrypted
session storage.

Delivered against the Project Task Document *"Authentication System Application Using
Kotlin"* — NeuroVerse Software Solutions LLP, Reg. No. **ACT-3831**.

| | |
|---|---|
| **Application ID** | `com.neuroverse.authapp` (debug installs as `.debug`) |
| **Screens** | Welcome · Register · Login · Forgot Password · Home · Profile |
| **Tests** | 46 unit tests, all passing, no emulator required |
| **Builds** | Debug 14 MB · Release 2.0 MB (R8) |

---

## Table of contents

1. [Quick start](#1-quick-start)
2. [Requirements](#2-requirements)
3. [Dependencies](#3-dependencies)
4. [Project structure](#4-project-structure)
5. [Enabling Firebase](#5-enabling-firebase)
6. [Running the app](#6-running-the-app)
7. [Testing](#7-testing)
8. [Features against the specification](#8-features-against-the-specification)
9. [Security](#9-security)
10. [Documentation](#10-documentation)
11. [Known limitations](#11-known-limitations)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Quick start

```bash
git clone <your-repo-url>
cd "Kotlin Demo App"

./gradlew assembleDebug      # build
./gradlew test               # run 46 unit tests
./gradlew installDebug       # install onto a running emulator/device
```

Or open the folder in Android Studio, wait for Gradle sync, and press **Run**.

> **It runs with zero configuration.** With no `google-services.json` the app uses a
> fully-functional offline backend with real cryptography, so you can build, run and
> demonstrate every flow immediately. Add the Firebase config when you want real
> reset emails — see [§5](#5-enabling-firebase). No code changes either way.

No `JAVA_HOME` setup is needed. The build is verified on **JDK 17** and on **JDK 25**
(the JDK bundled with Android Studio), so the Run button works out of the box.

---

## 2. Requirements

| Tool | Version | Notes |
|---|---|---|
| Android Studio | 2026.2+ | Or just the Android SDK + the Gradle wrapper |
| Android SDK Platform | **API 37** | `compileSdk` / `targetSdk` |
| SDK Build Tools | 36.0.0+ | |
| JDK | 17 or 25 | Both verified. Android Studio ships its own. |
| Emulator / device | **API 26+** | `minSdk = 26` (Android 8.0) |

`minSdk` is 26 because the app uses the Android Keystore AES/GCM APIs introduced in
Android 8.0. That still covers ~99% of active devices.

---

## 3. Dependencies

All versions are declared in **one** place — [`gradle/libs.versions.toml`](gradle/libs.versions.toml)
(a Gradle *version catalog*) — and referenced from build files as `libs.*`. Never hardcode
a version in a `build.gradle.kts`.

### 3.1 Build plugins

| Plugin | Version | Purpose |
|---|---|---|
| `com.android.application` | **9.5.1** | Android Gradle Plugin. **Since AGP 9, Kotlin support is built in.** |
| `org.jetbrains.kotlin.plugin.compose` | **2.4.21** | Compose compiler; also pins the Kotlin version |
| `com.google.gms.google-services` | **4.6.0** | Reads `google-services.json`. **Applied conditionally** — only when that file exists. |

> ⚠️ There is deliberately **no** `org.jetbrains.kotlin.android` plugin. Since AGP 9.0 it
> is built into the Android plugin, and adding it back **fails the build**:
> `The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support
> since AGP 9.0.` Nearly every tutorial online still shows the old form — strip that line
> when adapting one.

### 3.2 Build toolchain

| Component | Version |
|---|---|
| Gradle (via wrapper) | **9.9.1** |
| Kotlin | **2.4.21** |
| Java source/target compatibility | **17** |
| Kotlin `jvmTarget` | **17** |

### 3.3 AndroidX core

| Dependency | Version | Purpose |
|---|---|---|
| `androidx.core:core-ktx` | 1.19.1 | Kotlin extensions over the Android framework |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.11.0 | `lifecycleScope` |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.11.0 | `viewModel()` inside Compose |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.11.0 | `collectAsStateWithLifecycle()` |
| `androidx.activity:activity-compose` | 1.13.0 | `setContent { }`, `enableEdgeToEdge()` |
| `androidx.navigation:navigation-compose` | 2.10.2 | `NavHost`, routing, the auth guard |

### 3.4 Jetpack Compose

Versions come from the **BOM** (Bill of Materials), so the individual artifacts are
declared with no version at all. This guarantees the dozen interdependent Compose
artifacts stay mutually consistent.

| Dependency | Version | Purpose |
|---|---|---|
| `androidx.compose:compose-bom` | **2026.09.00** | Pins every `androidx.compose.*` version |
| `androidx.compose.ui:ui` | *(from BOM)* | Core Compose runtime |
| `androidx.compose.ui:ui-graphics` | *(from BOM)* | Graphics primitives |
| `androidx.compose.ui:ui-tooling-preview` | *(from BOM)* | `@Preview` annotation |
| `androidx.compose.material3:material3` | *(from BOM)* | Material 3 components |
| `androidx.compose.ui:ui-tooling` | *(from BOM)* | **debug only** — Preview pane, Layout Inspector |

> No `material-icons-extended` dependency. The three icons needed (eye, eye-off, back
> arrow) are hand-written vector drawables in `res/drawable/`, which avoids pulling a
> large frozen artifact in for three glyphs.

### 3.5 Coroutines

| Dependency | Version | Purpose |
|---|---|---|
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | `Dispatchers.Main`, structured concurrency |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | 1.11.0 | `Task<T>.await()` for Firebase |

### 3.6 Firebase

Also BOM-managed.

| Dependency | Version | Purpose |
|---|---|---|
| `com.google.firebase:firebase-bom` | **35.2.0** | Pins every `com.google.firebase.*` version |
| `com.google.firebase:firebase-auth` | *(from BOM)* | Email/password auth, reset emails, server-side scrypt hashing |

### 3.7 Testing

| Dependency | Version | Scope |
|---|---|---|
| `junit:junit` | 4.13.2 | `testImplementation` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.11.0 | `testImplementation` — `runTest`, `StandardTestDispatcher` |

### 3.8 Android permissions

Declared in [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml). Both are *normal*
permissions, granted at install with no runtime prompt.

| Permission | Why |
|---|---|
| `android.permission.INTERNET` | HTTPS calls to Firebase |
| `android.permission.ACCESS_NETWORK_STATE` | Distinguish "offline" from "server error" |

Nothing else is requested. Every permission not requested is one that cannot be misused.

---

## 4. Project structure

```
Kotlin Demo App/
├── settings.gradle.kts          modules + repositories (runs first)
├── build.gradle.kts             root: declares plugins, applies none
├── gradle.properties            JVM memory, caching, parallelism
├── gradle/libs.versions.toml    THE version catalog
├── local.properties             your SDK path — gitignored
├── gradlew                      Gradle wrapper — always use this
├── README.md
├── docs/
│   ├── Android_Kotlin_Training_Guide.pdf   40-page training guide
│   ├── ARCHITECTURE.md                     architecture + auth process
│   └── TEST_RESULTS.md                     test evidence
└── app/
    ├── build.gradle.kts         SDK levels, build types, dependencies
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/neuroverse/authapp/
        │   │   ├── AuthApplication.kt      builds the object graph at startup
        │   │   ├── MainActivity.kt         the single Activity
        │   │   ├── core/validation/        PURE Kotlin → fast unit tests
        │   │   ├── data/
        │   │   │   ├── model/              User, AuthError, AuthOutcome
        │   │   │   ├── security/           KeystoreCrypto, PasswordHasher
        │   │   │   ├── session/            SessionStore (encrypted)
        │   │   │   ├── local/              LocalCredentialStore
        │   │   │   └── repository/         AuthRepository + 2 implementations
        │   │   ├── di/                     ServiceLocator
        │   │   └── ui/
        │   │       ├── navigation/         Destinations, AuthNavGraph (auth guard)
        │   │       ├── components/         AppTextField, PasswordTextField,
        │   │       │                       PrimaryButton, MessageBanner, AuthScaffold
        │   │       ├── theme/               Color, Type, Theme
        │   │       ├── session/            SessionViewModel
        │   │       └── welcome/ register/ login/ forgot/ home/ profile/
        │   └── res/
        │       ├── drawable/               vector icons
        │       ├── values/                 strings, colors, themes
        │       ├── values-night/           dark-mode colours
        │       ├── mipmap-anydpi-v26/      adaptive launcher icon
        │       └── xml/                    backup rules
        └── test/java/com/neuroverse/authapp/
            ├── MainDispatcherRule.kt       replaces Dispatchers.Main in tests
            ├── fake/FakeAuthRepository.kt  in-memory backend for tests
            ├── core/validation/ValidatorsTest.kt
            └── ui/  Login / Register / ForgotPassword / Session ViewModel tests
```

---

## 5. Enabling Firebase

The app ships with a working offline backend so it runs with no setup. To switch to real
Firebase Authentication — real reset emails, server-side scrypt hashing, atomic
duplicate-email rejection:

1. Create a project at [console.firebase.google.com](https://console.firebase.google.com).
2. **Add app → Android**, package name **`com.neuroverse.authapp`**.
   *(For debug builds also add `com.neuroverse.authapp.debug`, or temporarily remove
   `applicationIdSuffix` from `app/build.gradle.kts`.)*
3. In the console: **Authentication → Sign-in method → Email/Password → Enable**.
4. Download `google-services.json` and place it at **`app/google-services.json`**.
5. Rebuild: `./gradlew clean assembleDebug`

**No code changes are needed.** Confirm which backend is active in Logcat at startup:

```
I ServiceLocator: google-services.json found -> using Firebase Authentication
W ServiceLocator: No google-services.json -> using offline LocalAuthRepository
```

### How the switch works

`app/build.gradle.kts` applies the `google-services` plugin **only if** the file exists —
otherwise the plugin would fail the build. At runtime,
`FirebaseApp.initializeApp()` returns `null` when no configuration is present, and
`ServiceLocator` falls back to `LocalAuthRepository`. **One file decides the backend.**

`google-services.json` is gitignored. It contains no secret — Firebase security comes from
server-side rules, not from hiding that file — but it is environment-specific.

---

## 6. Running the app

### From Android Studio
Open the project folder → wait for Gradle sync → select a device → **Run**.

### From the terminal

```bash
export PATH=$PATH:~/Library/Android/sdk/platform-tools:~/Library/Android/sdk/emulator

emulator -list-avds                              # list virtual devices
emulator -avd <name> -gpu host &                 # boot one
adb devices                                      # must say 'device', not 'offline'

./gradlew installDebug
adb shell am start -n com.neuroverse.authapp.debug/com.neuroverse.authapp.MainActivity

# follow the app's logs
adb logcat --pid=$(adb shell pidof com.neuroverse.authapp.debug)
```

### Everyday Gradle commands

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew installDebug       # build + install
./gradlew test               # 46 unit tests (~1s, no emulator)
./gradlew assembleRelease    # shrunk release APK
./gradlew clean              # delete build outputs
./gradlew app:dependencies   # print the full dependency tree
```

APKs are written to `app/build/outputs/apk/`.

---

## 7. Testing

```bash
./gradlew test
```

| Test class | Tests |
|---|---:|
| `ValidatorsTest` | 15 |
| `LoginViewModelTest` | 11 |
| `RegisterViewModelTest` | 9 |
| `ForgotPasswordViewModelTest` | 6 |
| `SessionViewModelTest` | 5 |
| **Total** | **46** |

All pass in under a second with **no emulator**, because `Validators` imports nothing from
Android and the ViewModels depend only on the `AuthRepository` interface — substituted in
tests with an in-memory `FakeAuthRepository`.

HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`

Full evidence for all ten scenarios required by spec §7, including on-device commands and
their output, is in **[docs/TEST_RESULTS.md](docs/TEST_RESULTS.md)**.

---

## 8. Features against the specification

| Spec | Requirement | Status |
|---|---|---|
| §3.1 | User registration — name, email, password, validation, duplicate check | Done |
| §3.2 | Login with clear errors and a password show/hide toggle | Done |
| §3.3 | Forgot Password on the Login screen, secure reset flow | Done (real emails require Firebase) |
| §3.4 | Session management — secure token storage, restore on restart, redirect | Done |
| §3.5 | Logout — ends the session, clears local data, returns to Login | Done |
| §3.6 | All six screens | Done |
| §4 | Kotlin · Compose · MVVM · Coroutines · Keystore · Firebase · Git | Done |
| §5 | All nine security requirements | Done — see [§9](#9-security) |
| §7 | All ten test scenarios | Done — see [TEST_RESULTS.md](docs/TEST_RESULTS.md) |

---

## 9. Security

Full reasoning in [docs/ARCHITECTURE.md §5](docs/ARCHITECTURE.md) and Part 9 of the
training guide. Summary:

- **Passwords are never stored in plaintext.** Firebase hashes server-side with scrypt.
  The offline path stores only PBKDF2-HMAC-SHA256 at **210,000 iterations** with a unique
  16-byte random salt, compared in constant time.
- **Sessions are encrypted with AES-256/GCM** using a key generated inside the **Android
  Keystore** — on most devices in hardware. The key material never enters the app process,
  so the ciphertext is inert off the device.
- **GCM, not CBC:** tampering makes decryption *throw* rather than return plausible
  garbage. A fresh random IV is generated per encryption and stored alongside the
  ciphertext.
- **No account enumeration.** Login returns an identical error for an unknown email and a
  wrong password; password reset returns an identical response whether or not the account
  exists. Two unit tests enforce this and will fail if a "more helpful" message is added.
- **No secrets in source.** No API keys are hardcoded; `google-services.json` is gitignored.
- **Logout clears everything local** via `prefs.edit { clear() }` — verified as `<map />`.
- **Back after logout cannot reach a protected screen:** the back stack is cleared with
  `popUpTo(0) { inclusive = true }`.
- **`allowBackup="false"`** — the session key is device-bound and not backed up, so a
  restored copy would be undecryptable anyway.
- **Only two permissions** requested.

> **Stated plainly:** spec §5 requires server-side password hashing. The **Firebase path
> satisfies this**; the offline fallback hashes on-device and does not. The offline mode
> is a development and demonstration affordance, not a production backend.

---

## 10. Documentation

| Document | Contents |
|---|---|
| **[docs/Android_Kotlin_Training_Guide.pdf](docs/Android_Kotlin_Training_Guide.pdf)** | 40-page training guide: Android fundamentals, Kotlin for Java developers, coroutines, Compose, MVVM, Gradle, applied security, testing, and a Q&A section |
| **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** | Architecture and the authentication process, flow by flow (deliverable 10) |
| **[docs/TEST_RESULTS.md](docs/TEST_RESULTS.md)** | Evidence for all ten required scenarios (deliverable 8) |
| Source comments | Every build file and Kotlin source is commented to be read alongside the guide |

---

## 11. Known limitations

1. **Offline mode cannot send reset emails.** The link is logged to Logcat instead. The
   flow is satisfied; delivery is not. Firebase fixes this.
2. **Offline accounts are device-local** and are lost on uninstall.
3. **Offline hashing happens on-device**, which does not meet spec §5's server-side clause.
4. **No Compose UI tests.** Screens were verified manually. `testTag`s are already in place
   so these can be added.
5. **`KeystoreCrypto` / `PasswordHasher` have no unit tests** — they need instrumented
   tests (`android.util.Base64` and `AndroidKeyStore` are JVM stubs). Verified instead by
   inspecting encrypted output on-device.
6. **Some UI strings are inline** rather than in `strings.xml`. Deliberate for readability
   while learning; extract them before localising.
7. **The Firebase path has not been executed end to end**, as no `google-services.json` was
   supplied. The code is reviewed but untested against the live service.

---

## 12. Troubleshooting

**`The 'org.jetbrains.kotlin.android' plugin is no longer required…`**
You added that plugin (probably by copying a tutorial). Remove it — see [§3.1](#31-build-plugins).

**`File google-services.json is missing`**
The `google-services` plugin was applied without the file. This project applies it
conditionally, so you likely added it unconditionally; revert that, or add the file.

**Emulator shows `offline` and never boots**
It is hung. Kill and restart:
```bash
pkill -f qemu-system-aarch64 && adb kill-server && adb start-server
emulator -avd <name> -gpu host &
```

**`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**
A build with a different signing key is installed. `adb uninstall com.neuroverse.authapp.debug`

**Gradle sync fails after changing versions**
`./gradlew --stop && ./gradlew clean build --refresh-dependencies`

**Reset the app to a clean install state (clears accounts and session)**
```bash
adb shell pm clear com.neuroverse.authapp.debug
```
