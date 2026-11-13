# Test Results

Deliverable 8 of the Project Task Document (NeuroVerse, Reg. ACT-3831).
Covers the ten test scenarios required by spec §7.

**Date of run:** 14 November 2026
**Device:** Android Emulator, Medium Phone, **API 37** (arm64-v8a), Android Studio 2026.2
**Build:** debug (`com.neuroverse.authapp.debug`), backend = offline `LocalAuthRepository`
**Toolchain:** AGP 9.5.1 · Gradle 9.9.1 · Kotlin 2.4.21 · JDK 17 (also verified on JDK 25)

---

## 1. Automated test suite

```
$ ./gradlew test
BUILD SUCCESSFUL
```

| Test class | Tests | Failures | Errors |
|---|---:|---:|---:|
| `ValidatorsTest` | 15 | 0 | 0 |
| `LoginViewModelTest` | 11 | 0 | 0 |
| `RegisterViewModelTest` | 9 | 0 | 0 |
| `ForgotPasswordViewModelTest` | 6 | 0 | 0 |
| `SessionViewModelTest` | 5 | 0 | 0 |
| **Total** | **46** | **0** | **0** |

Run time: **0.66 s** — no emulator required, because `Validators` imports nothing from
Android and the ViewModels depend only on the `AuthRepository` interface (substituted with
an in-memory `FakeAuthRepository`).

HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`

## 2. Build verification

| Build | Result | Output |
|---|---|---|
| `./gradlew assembleDebug` | PASS | `app-debug.apk`, 14 MB |
| `./gradlew assembleRelease` | PASS | `app-release-unsigned.apk`, **2.0 MB** (R8 removed ~86%) |
| `./gradlew test` | PASS | 46/46 |

The release build is verified because R8 can strip code reached only by reflection,
producing release-only crashes that never appear in debug.

---

## 3. The ten required scenarios (spec §7)

Each scenario was verified **twice**: by automated test, and by hand on the emulator.

### Scenario 1 — Register a new user successfully — **PASS**

*Automated:* `RegisterViewModelTest.scenario 1 - registering a new user succeeds`

*Manual:* Registered `Prabhakar Kumar / prabhakar@neuroverse.com / Secure2026pass`.
The app navigated to Home showing *"Hello, Prabhakar Kumar"*, the avatar initial `P`,
and `Status: Authenticated`.

Stored account record read back off the device:

```
$ adb shell run-as com.neuroverse.authapp.debug \
      cat /data/data/com.neuroverse.authapp.debug/shared_prefs/neuroverse_local_accounts.xml

{"id":"1823d9dd-adde-4c8c-9c54-ac1cd8c24448",
 "name":"Prabhakar Kumar",
 "passwordHash":"210000:IrmjXRMNX6+QKuyXd7cNUA==:RFNahglW+pwqoU1idAV5+BG7Nn+FUnWu9aFpA/ozZRY="}
```

The plaintext password appears nowhere — only `iterations:salt:hash` (spec §5, bullet 1).

### Scenario 2 — Prevent duplicate email registration — **PASS**

*Automated:* `RegisterViewModelTest.scenario 2 - registering an existing email is rejected`

*Manual:* Attempted to register `Someone Else` against the **same** email. The form
rejected it with *"An account with this email already exists."* rendered in red and
attached to the **email field** (not the form banner), so it points at the field the user
must change.

### Scenario 3 — Log in with valid credentials — **PASS**

*Automated:* `LoginViewModelTest.scenario 3 - login with valid credentials succeeds`,
plus `email is matched case-insensitively`

*Manual:* After logging out, signed in with the correct password. Navigated to Home with
the display name correctly restored from the stored account record. A new encrypted
`session_blob` was written.

### Scenario 4 — Reject invalid credentials — **PASS**

*Automated:* `LoginViewModelTest.scenario 4 - wrong password is rejected` and
`scenario 4 - unknown email is rejected with the SAME message as a wrong password`

*Manual:* Signed in with `prabhakar@neuroverse.com` / `WrongPass123`. An error banner
appeared reading *"Incorrect email or password."*

> **Security note.** The message is identical for a wrong password and for an email that
> is not registered. Distinguishing them would turn the login screen into an account
> enumeration oracle. A dedicated unit test asserts the two messages are equal and will
> fail if a "more helpful" message is ever introduced.

### Scenario 5 — Handle invalid email and password inputs — **PASS**

*Automated:* all 15 `ValidatorsTest` cases, plus
`RegisterViewModelTest.scenario 5 - invalid email blocks submission without calling the backend`,
`scenario 5 - weak password blocks submission`,
`all field errors are reported at once, not one at a time`,
`editing a field clears its error`,
and `LoginViewModelTest.scenario 5 - malformed email is caught before any backend call`.

Boundary values are covered explicitly: a 7-character password fails and an 8-character
one passes; a 1-character name fails and 2 passes; a 51-character name fails.

*Manual:* The Create account / Sign in buttons stay **disabled** until every field has
content, which was visible as a greyed-out button on both empty forms.

### Scenario 6 — Complete the password-reset flow — **PASS**

*Automated:* `ForgotPasswordViewModelTest` — 6 tests including
`scenario 6 - an UNKNOWN email reports the same success message` and
`success message is deliberately non-committal about whether mail was sent`

*Manual:* Login → *Forgot password?* → entered the registered address → *Send reset link*.
A green banner appeared:

> *"If an account exists for this email, a password reset link has been sent. Please check
> your inbox."*

Logcat confirmed the flow reached the repository:

```
D LocalAuth: Password reset requested for prabhakar@neuroverse.com (no email sent in offline mode)
```

> **Limitation, stated plainly.** In offline mode no email can be sent; the link is logged
> instead. The *flow* is satisfied, the *delivery* is not. With `google-services.json`
> present, `FirebaseAuthRepository.sendPasswordReset()` sends a real single-use expiring
> link. This is the main reason Firebase is the production path.

### Scenario 7 — Maintain the session after restarting the application — **PASS**

*Automated:* `SessionViewModelTest.scenario 7 - a saved session is restored on startup`,
plus `state starts as Loading, never as SignedOut`

*Manual:* The process was **killed outright**, not backgrounded:

```
$ adb shell am force-stop com.neuroverse.authapp.debug
$ adb shell pidof com.neuroverse.authapp.debug
(no output — process gone)
$ adb shell am start -n com.neuroverse.authapp.debug/com.neuroverse.authapp.MainActivity
```

The app reopened **directly on Home**, still authenticated. No Login-screen flash was
observed, because `AuthState.Loading` renders a spinner until the restore completes.

Encrypted session as stored on disk:

```
$ adb shell run-as com.neuroverse.authapp.debug \
      cat /data/data/com.neuroverse.authapp.debug/shared_prefs/neuroverse_session.xml

<string name="session_blob">JiHklY8PwFH/kI9sbVrDZzt0UypVQVXr+yxZBlP3wfrwWGRmklQTWuQkyrZE
Irhe2OuvRf2/L94NuODZFDDDe7m43rWaenMuLjoBlxq8UxRs2DIG/agJ8xccuA762S2Rd67Bc3Z86b+s...</string>
```

Opaque. No email, name or id is recoverable without the device's Keystore.

### Scenario 8 — Prevent unauthenticated users from accessing protected screens — **PASS**

*Automated:* `SessionViewModelTest.scenario 8 - with no saved session the state is SignedOut`

*Manual:* After logging out, pressing the system **Back** button exited to the launcher
rather than returning to Home:

```
$ adb shell input keyevent 4
$ adb shell dumpsys activity activities | grep topResumedActivity
topResumedActivity=ActivityRecord{... com.google.android.apps.nexuslauncher/.NexusLauncherActivity}
```

The back stack is cleared by `popUpTo(0) { inclusive = true }`, so there is physically
nothing behind the Welcome screen to navigate back into.

### Scenario 9 — Log out successfully and clear the local session — **PASS**

*Automated:* `SessionViewModelTest.scenario 9 - logout clears the session and signs the user out`

*Manual:* Tapped *Log out* on Home → a confirmation dialog appeared (*"Log out? You will
need to sign in again to access your account."*) → confirmed.

Session file immediately after logout:

```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map />
```

Completely empty (spec §3.5, §5). The **accounts** file correctly survives — logging out
must not delete the account:

```
$ adb shell run-as ... cat .../neuroverse_local_accounts.xml | grep -o passwordHash
passwordHash
```

### Scenario 10 — Handle network errors and authentication failures — **PASS**

*Automated:* exercised in all three ViewModel test classes by forcing errors through
`FakeAuthRepository.nextError`:

- `RegisterViewModelTest.scenario 10 - network failure surfaces a readable error`
- `LoginViewModelTest.scenario 10 - network error is reported and loading is cleared`
- `LoginViewModelTest.rate limiting is reported distinctly`
- `ForgotPasswordViewModelTest.network failure is reported`

Each asserts both that a readable message is shown **and** that `isLoading` is cleared —
a loading flag left set on a failure path permanently disables the button, which is the
classic version of this bug.

`FirebaseAuthRepository.mapException()` translates Firebase's exception hierarchy into the
app's own `AuthError` set, so no provider-shaped exception or raw message ever reaches the
UI.

---

## 4. Additional verification beyond spec §7

| Check | Result |
|---|---|
| Password show/hide toggle (spec §3.2) | **PASS** — revealed `Secure2026pass`, icon switched to eye-off |
| Backend auto-selection | **PASS** — logcat: `No google-services.json -> using offline LocalAuthRepository` |
| All six screens render (spec §3.6) | **PASS** — Welcome, Register, Login, Forgot, Home, Profile |
| Dark mode | **PASS** — `adb shell cmd uimode night yes`; correct palette, readable contrast |
| Submit disabled while a request is in flight | **PASS** — asserted by the gated loading test |
| No crashes during the full manual walkthrough | **PASS** — logcat clean of `FATAL` / `AndroidRuntime` |
| Release build (R8) | **PASS** — 2.0 MB, builds and shrinks without error |
| Profile data matches stored record | **PASS** — displayed User ID equals the stored UUID |

---

## 5. Known gaps

Stated deliberately rather than omitted.

1. **No Compose UI tests.** The screens were verified manually on the emulator. The
   `testTag` values already present in `AppTextField` and `PasswordTextField` exist so
   these tests can address fields by a stable handle; adding
   `createAndroidComposeRule` tests is the clear next step.
2. **`KeystoreCrypto` and `PasswordHasher` have no unit tests.** Both depend on
   `android.util.Base64` and the `AndroidKeyStore` provider, which are stubs on the JVM.
   They require instrumented tests. They were instead verified by inspecting the encrypted
   output on a real device (scenarios 1 and 7 above).
3. **No tests against live Firebase.** That requires a real project and network access and
   belongs in a separate integration suite. The Firebase code path was reviewed but has
   not been executed end to end, because no `google-services.json` was supplied.
4. **Offline mode hashes on the device**, which does not satisfy spec §5's server-side
   hashing clause. Only the Firebase path does.
