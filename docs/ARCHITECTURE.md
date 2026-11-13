# Architecture & Authentication Process

Deliverable 10 of the Project Task Document (NeuroVerse, Reg. ACT-3831).
For the full teaching version of this material, see
[Android_Kotlin_Training_Guide.pdf](Android_Kotlin_Training_Guide.pdf).

---

## 1. Architectural overview

The app follows **MVVM** (spec §4) with a strict dependency rule: dependencies point
inward only. The UI knows its ViewModel; the ViewModel knows the `AuthRepository`
*interface*; nothing above the data layer knows that Firebase, the Android Keystore or
`SharedPreferences` exist.

```
┌──────────────────────────────────────────────────────────────┐
│  UI  (Jetpack Compose)                                       │
│  WelcomeScreen  RegisterScreen  LoginScreen                  │
│  ForgotPasswordScreen  HomeScreen  ProfileScreen             │
│  AuthNavGraph  ← routing + the authentication guard          │
└───────────────────────────┬──────────────────────────────────┘
                            │  state down / events up
┌───────────────────────────┴──────────────────────────────────┐
│  ViewModels                                                  │
│  SessionViewModel (app-wide auth state)                      │
│  RegisterViewModel  LoginViewModel  ForgotPasswordViewModel  │
└───────────────────────────┬──────────────────────────────────┘
                            │  depends ONLY on the interface
┌───────────────────────────┴──────────────────────────────────┐
│  AuthRepository  « interface »            ← THE SEAM         │
├──────────────────────────────────────────────────────────────┤
│  FirebaseAuthRepository    │  LocalAuthRepository            │
│  (google-services.json     │  (offline; PBKDF2 on device)    │
│   present)                 │                                 │
└───────────┬────────────────┴──────────────┬─────────────────┘
            │                               │
┌───────────┴──────────┐       ┌────────────┴─────────────┐
│  SessionStore        │       │  LocalCredentialStore    │
│  encrypted session   │       │  PBKDF2 password hashes  │
└───────────┬──────────┘       └──────────────────────────┘
            │
┌───────────┴──────────────────────────────────────────────────┐
│  KeystoreCrypto — AES-256/GCM, key held in Android Keystore  │
└──────────────────────────────────────────────────────────────┘
```

### Why the repository is an interface

Spec §9 requires that "authentication logic shall be kept separate from the user
interface". The interface **is** that separation, and it is enforced by the import graph
rather than by convention: `com.google.firebase` appears in exactly two files, neither of
which is a screen or a ViewModel.

Three implementations exist:

| Implementation | Active when | Depends on |
|---|---|---|
| `FirebaseAuthRepository` | `app/google-services.json` exists | Firebase Auth SDK |
| `LocalAuthRepository` | Otherwise — so the app always runs | Keystore + PBKDF2 |
| `FakeAuthRepository` | All 46 unit tests | Nothing (in-memory Kotlin) |

This is what allows the entire ViewModel layer to be unit-tested on the JVM in ~1.3
seconds with no emulator, and what makes a future migration to a custom REST backend a
one-class change.

---

## 2. Layer responsibilities

| Layer | Package | Responsibility |
|---|---|---|
| UI | `ui/**` | Layout only. Stateless content composables + thin stateful wrappers. |
| Navigation | `ui/navigation` | Routing **and** the auth guard that protects screens. |
| ViewModel | `ui/*/XViewModel.kt` | Screen state (`UiState`), validation orchestration, calling the repository. |
| Validation | `core/validation` | Pure Kotlin, zero Android imports — hence instantly unit-testable. |
| Repository | `data/repository` | The auth contract and its implementations. |
| Session | `data/session` | Encrypted session persistence (spec §3.4). |
| Security | `data/security` | Keystore AES-GCM crypto; PBKDF2 password hashing. |
| DI | `di` | Builds the object graph; chooses the backend at runtime. |

---

## 3. The authentication process

### 3.1 Startup and backend selection

```
1. OS creates the process → AuthApplication.onCreate()
2. ServiceLocator.authRepository(this)
3. FirebaseApp.initializeApp(context)
       ├── non-null → FirebaseAuthRepository   (google-services.json present)
       └── NULL     → LocalAuthRepository      (no config → offline mode)
4. MainActivity.onCreate() → setContent { AuthNavGraph() }
5. SessionViewModel.init → restoreSession()
6. authState = Loading → spinner shown (NOT the Login screen)
7. restore completes → SignedIn(user) | SignedOut
8. Guard routes to Home or Welcome
```

Without `google-services.json`, the Gradle plugin that converts it into Android string
resources is never applied, so `initializeApp` finds no configuration and returns `null`.
**The presence of one file selects the backend** — no build flag, no code change.

Step 6 matters: modelling "not known yet" as its own state is what prevents the Login
screen flashing before a returning user is routed to Home.

### 3.2 Registration (spec §3.1)

```
RegisterScreen → viewModel.register()
   │
   ├─ validateName / validateEmail / validatePassword / validateConfirmation
   │     all four run → every error is reported at once
   │     any invalid → state.copy(errors…) → STOP (no network call)
   │
   ▼ all valid
 state.copy(isLoading = true)          ← button disables; no double-submit
   │
   ▼
 AuthRepository.register(name, email, password)
   │
   ├── Firebase: createUserWithEmailAndPassword → updateProfile(displayName)
   │             → sendEmailVerification (best effort; failure must not fail signup)
   │             ↳ password hashed server-side with scrypt
   │
   └── Local:    credentials.exists(email)? → EmailAlreadyRegistered
                 PasswordHasher.hash(password)  ↳ PBKDF2-HMAC-SHA256, 210k iterations
   │
   ▼
 SessionStore.saveSession(user)
     JSON → KeystoreCrypto.encrypt() (AES-256/GCM) → SharedPreferences
   │
   ▼
 _currentUser.value = user   → StateFlow emits
   │
   ├─ SessionViewModel → authState = SignedIn
   └─ RegisterViewModel → isRegistrationSuccessful = true
   │
   ▼
 Guard navigates to HOME with popUpTo(0) { inclusive = true }
```

The ViewModel never calls `navigate()`. It sets a boolean; the UI reacts via a keyed
`LaunchedEffect` and then clears the flag. A ViewModel holding a `NavController` would
stop being unit-testable.

### 3.3 Login (spec §3.2)

Same shape, with two deliberate differences:

1. **Password validation is presence-only.** Applying registration strength rules would
   lock out accounts created before the rules tightened, and would leak that a guess was
   rejected by format rather than by the credential check.
2. **Failures are indistinguishable.** "No such account" and "wrong password" both become
   `InvalidCredentials` → *"Incorrect email or password."* See §5.

### 3.4 Password reset (spec §3.3)

The app requests a reset and never sets the new password itself. The link goes to the
user's email and the password is set on the provider's own verified page — possession of
the inbox is the proof of identity. An in-app "enter your new password" form would have
nothing to verify against.

Success is reported **whether or not the account exists**, with deliberately conditional
wording ("*If* an account exists for this email…").

### 3.5 Session restore (spec §3.4)

```
Process killed (memory reclaimed, or `adb shell am force-stop`)
   │
   ▼ user reopens
 repository.restoreSession()
   ├── Firebase: firebaseAuth.currentUser
   │       null → sessionStore.clear() → null
   │       else → merge with cached display name
   └── Local:   SharedPreferences → Base64 → KeystoreCrypto.decrypt()
                   ok   → User
                   fail → null   (tampered ciphertext / key invalidated)
   │
   ▼
 authState = SignedIn(user) | SignedOut
```

Every decryption failure collapses to `null`, and `null` means "show Login". **Failing
closed** is the correct default for an auth system.

### 3.6 Logout (spec §3.5)

```
Tap "Log out" → AlertDialog confirms (destructive action)
   │
   ▼
 repository.logout()
   ├─ firebaseAuth.signOut()   clears Firebase's token cache
   ├─ sessionStore.clear()     prefs.edit { clear() } ← wipes EVERY key
   └─ _currentUser.value = null
   │
   ▼
 Guard navigates to WELCOME with popUpTo(0) { inclusive = true }
```

`popUpTo(0) { inclusive = true }` clears the **entire** back stack. Without it, pressing
Back after logout returns to Home with a dead session — the most common way an auth flow
leaks a protected screen. Verified: after logout, Back exits to the launcher.

`clear()` rather than removing named keys means a field added to the session later is
wiped automatically and cannot be forgotten.

### 3.7 Screen protection (spec §2, §3.4)

There is **no** `if (loggedIn)` check inside `HomeScreen` or `ProfileScreen`. The guard
lives in `AuthNavGraph`:

```kotlin
LaunchedEffect(isSignedIn) {
    val target = if (isSignedIn) Destinations.HOME else Destinations.WELCOME
    if (!alreadyCorrect) {
        navController.navigate(target) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
}
```

Centralising it means **a new protected screen is protected by default**, rather than only
if someone remembers to add a check.

---

## 4. State management

Each screen exposes one immutable `UiState` data class via a read-only `StateFlow`:

```kotlin
private val _uiState = MutableStateFlow(LoginUiState())
val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
```

A single immutable object swapped atomically can only ever represent a state that actually
occurred. Separate flows per field would allow the UI to observe impossible combinations
(e.g. `isLoading = true` alongside a success message) because each updates independently.

Derived values such as `canSubmit` are computed properties, so they cannot drift out of
sync with the fields they depend on.

---

## 5. Security design decisions

| Decision | Rationale |
|---|---|
| PBKDF2-HMAC-SHA256, 210k iterations, 16-byte random salt | OWASP floor; deliberately slow. Iteration count embedded in the stored hash so it can be raised without invalidating accounts. |
| Constant-time hash comparison | A short-circuiting compare leaks how many leading bytes were guessed correctly via timing. |
| AES-256/**GCM**, not CBC | GCM is AEAD: tampering makes decryption *throw* rather than return plausible garbage. |
| Key in the Android Keystore | Key material never enters the app process; on most devices it lives in hardware. Ciphertext is inert off the device. |
| Fresh random IV per encryption, stored with the ciphertext | IV reuse under GCM leaks the authentication key, not just the plaintext. IVs are unique, not secret. |
| Identical login error for unknown email and wrong password | Otherwise login becomes an account-enumeration oracle. |
| Conditional password-reset wording | Same reason. |
| Firebase ID token **not** persisted | The SDK keeps its own refresh token and mints 1-hour ID tokens on demand; a second copy widens the attack surface for no benefit. |
| `allowBackup="false"` | The session is encrypted with a device-bound Keystore key that is never backed up, so a restored copy would be undecryptable anyway. |
| Only `INTERNET` + `ACCESS_NETWORK_STATE` permissions | Every permission not requested is one that cannot be misused. |

### Honest limitation

Spec §5 requires passwords to be hashed **on the server**. The offline mode hashes on the
device and therefore does **not** meet that clause; it exists so the app runs and can be
demonstrated with no backend. **The Firebase path is the one that satisfies §5** —
passwords leave the device over HTTPS and Firebase hashes them with scrypt server-side.

### What client-side validation is and is not

Every rule in `Validators.kt` is a **UX convenience**, not a security control. An attacker
does not use the UI. The provider re-validates everything, and that is the half that
enforces anything. Spec §5 asks for validation on both sides because the two halves do
different jobs.

---

## 6. Technology choices

| Requirement (spec §4) | Choice | Note |
|---|---|---|
| Programming language | Kotlin 2.4.21 | |
| Platform | Android, minSdk 26, target/compileSdk 37 | minSdk 26 required by the Keystore AES/GCM APIs used |
| User interface | Jetpack Compose + Material 3 | Spec allowed Compose or XML |
| Architecture | MVVM | |
| Asynchronous operations | Kotlin Coroutines + StateFlow | |
| Local secure storage | Android Keystore, AES-256/GCM | |
| Authentication | Firebase Auth, with offline fallback | Confirmed with the client before development |
| Version control | Git | |

Dependency injection is **manual** (`ServiceLocator`) rather than Hilt. The app has one
dependency worth injecting; Hilt would add an annotation processor, a plugin and modules
to solve a 20-line problem, and its generated code hides the object graph. Manual DI is a
documented, officially-endorsed Android approach — the property that matters, centralised
construction, holds either way.
