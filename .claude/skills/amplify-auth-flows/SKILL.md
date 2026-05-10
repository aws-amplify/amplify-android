---
name: amplify-auth-flows
description: Reference map of every Cognito auth flow in aws-auth-cognito (sign-in factors, challenge handling, sign-up + auto sign-in, sign-out, fetch session, federation, delete user, passkeys, MFA/TOTP) — state paths, actions, terminal states, Hub events, exception types, fork divergence, and historical bugs. Use when implementing or debugging any auth flow, planning an upstream merge, or correlating production error logs to specific flow paths.
---

# Cognito auth flows — reference map

Canonical reference for `aws-auth-cognito` flow paths. Distilled from a code-cited read of `release_v2.36.0` plus the `feature/upgrade-to-2.34.0-with-multi-user` fork. Use this when designing a change, debugging, or matching a stack trace / log line to a flow path.

When upstream changes a flow, re-read the relevant resolver and update this file.

## AuthState shape (cheat sheet)

```
AuthState (top-level)
├─ NotConfigured
├─ ConfiguringAuth → ConfiguringAuthentication → ConfiguringAuthorization → Configured
└─ Configured
   ├─ authNState: AuthenticationState
   │   ├─ NotConfigured / Configured
   │   ├─ SigningIn(signInState: SignInState)
   │   ├─ SignedIn(signedInData, deviceMetadata)
   │   ├─ SigningOut(signOutState: SignOutState)
   │   ├─ SignedOut(signedOutData)
   │   ├─ FederatingToIdentityPool / FederatedToIdentityPool
   │   └─ Error(exception)
   ├─ authZState: AuthorizationState
   │   ├─ NotConfigured / Configured / SigningIn / SigningOut
   │   ├─ FetchingAuthSession(signedInData, fetchAuthSessionState)
   │   ├─ FetchingUnAuthSession(fetchAuthSessionState)
   │   ├─ RefreshingSession(existingCredential, refreshSessionState)
   │   ├─ DeletingUser(deleteUserState, amplifyCredential)
   │   ├─ StoringCredentials(amplifyCredential)
   │   ├─ SessionEstablished(amplifyCredential)
   │   ├─ FederatingToIdentityPool(federatedToken, fetchAuthSessionState, existingCredential)
   │   └─ Error(exception: SessionError)
   └─ authSignUpState: SignUpState
       ├─ NotStarted / InitiatingSignUp / AwaitingUserConfirmation / ConfirmingSignUp / SignedUp / Error
```

Concurrency rules:
- `SignedIn + SessionEstablished` — legal, the steady state.
- `SignedIn + FetchingAuthSession` — transient only.
- `SignedIn + RefreshingSession` — legal (refresh runs alongside SessionEstablished).
- `SigningIn + FetchingAuthSession` — illegal; fetch only triggers post-`SignInCompleted`.
- `FederatedToIdentityPool + SessionEstablished(IdentityPoolFederated)` — legal.

---

## 1. Sign-in factors (`SignInUseCase`)

File: `aws-auth-cognito/.../usecases/SignInUseCase.kt`

### 1.1 SRP — `USER_SRP_AUTH`
- **Path:** `SignedOut|Configured` → `SigningIn(SigningInWithSRP(SRPSignInState.InitiatingSRPA))` → (`ResolvingChallenge(WaitingForAnswer)` if MFA) → `RespondingPasswordVerifier` → `SignedIn`.
- **Resolvers:** `SignInState.Resolver` + `SRPSignInState.Resolver` + `SignInChallengeState.Resolver`.
- **Actions:** `signInActions.startSRPAuthAction` (`initiateSrpAuth`), `srpActions.verifyPasswordSRPAction` (`respondToAuthChallenge(PASSWORD_VERIFIER)`).
- **Terminal:** `SignedIn`. Use case detects via `mapNotNull { state -> state.authNState as? SignedIn }.first()`.
- **Hub:** `SIGNED_IN` (`AuthChannelEventName.SIGNED_IN`).
- **Exceptions (via `CognitoAuthExceptionConverter`):** `NotAuthorizedException`, `UserNotConfirmedException`, `PasswordResetRequiredException`, `FailedAttemptsLimitExceededException`.
- **Fork:** none.

### 1.2 Migration password — `USER_PASSWORD_AUTH`
- **Path:** `SignedOut` → `SigningIn(SigningInViaMigrateAuth(MigrateSignInState))` → `ResolvingChallenge` (if MFA) → `SignedIn`.
- **Action:** `signInActions.startMigrationAuthAction` → `initiateAuth(USER_PASSWORD_AUTH, {USERNAME, PASSWORD})`.
- **Note:** v2.30.x (`8b2ef912`) fixed a bug extracting username for this flow.

### 1.3 Custom auth — `CUSTOM_AUTH`, `CUSTOM_AUTH_WITHOUT_SRP`
- **Path:** `SigningIn(SigningInWithCustom(CustomSignInState))` → `ResolvingChallenge` → `SignedIn`.
- **Action:** `signInActions.startCustomAuthAction` → `initiateAuth(CUSTOM_AUTH)`.

### 1.4 Custom auth with SRP — `CUSTOM_AUTH_WITH_SRP`
- **Path:** `SigningIn(SigningInWithSRPCustom(SRPSignInState))` → `RespondingPasswordVerifier` → `ResolvingChallenge` → `SignedIn`.

### 1.5 USER_AUTH (factor selection)
- **Path:** `SigningIn(SigningInWithUserAuth)` → `ResolvingChallenge(challenge.name=SELECT_CHALLENGE)` → (user picks PASSWORD / PASSWORD_SRP / WEB_AUTHN) → corresponding factor flow → `SignedIn`.
- `SignInUseCase.getSignInData()` maps `preferredFirstFactor`:
  - `PASSWORD` → `MigrationAuthSignInData(USER_AUTH)`
  - `PASSWORD_SRP` → `SRPSignInData(USER_AUTH)`
  - else → `UserAuthSignInData(preferredChallenge=factor)` (Cognito prompts).

### 1.6 Hosted UI — `signInWithWebUI` (this is the **SSO / generic / enterprise login** path most apps use)
- **Path:** `SignedOut` → `SigningIn(SigningInWithHostedUI(HostedUISignInState.NotStarted))` → `AwaitingRedirect` (browser intent / Custom Tabs) → `FetchingToken` (PKCE code exchange) → `SignedIn`.
- **Resolver:** `HostedUISignInState.Resolver`. Redirect handled by `RealAWSCognitoAuthPlugin.handleWebUISignInResponse(intent)`.
- **Actions:** `signInActions.startHostedUIAuthAction` (launches OAuth redirect / Custom Tabs intent), `HostedUIEvent.FetchToken(callbackUri)` on return → `exchangeCodeForToken`.
- **Terminal:** `SignedIn` + `SessionEstablished`.
- **Hub:** `SIGNED_IN`.
- **Exceptions:** `HostedUISignOutException`, `UserCancelledException` (back button or browser closed), errors stored in `HostedUIErrorData`.
- **Known issues:**
  - `415a7676 fix(auth): UserCancelledException occurrence on sign-in after sign-out` — race between sign-out completion and re-launching hosted UI surfaced spurious cancel exceptions.
  - `8233677a fix(auth): Select first non http(s) redirect` — picks the right redirect URI from configuration; bug if multiple http(s) variants present.
  - `2dce10de Fix NPE in CustomTabsManagerActivity after process death` (fork) — process-death recovery in Custom Tabs.
- **Features:**
  - `b8242c7f Prefer Private Session support for WebUI Sign In's` — uses `PrivateSessionToken` to suppress persistent browser cookies (better for shared devices).
  - `694dac08 add support for cognito oidc parameters in managed login` — extra OIDC params (e.g. `idp_identifier`, `lang`) passed through.
- **Fork:** none directly, but multi-user means each user's hosted-UI redirect must land in a userId-scoped credential bucket.

### 1.7 Passkey / WebAuthn
**Registration** (`AssociateWebAuthnCredentialUseCase`):
1. `fetchAuthSession()` → access token,
2. `startWebAuthnRegistration(accessToken)` → `credentialCreationOptions`,
3. Android Credential Manager `createCredential()`,
4. `completeWebAuthnRegistration(credential, accessToken)`.

**Sign-in** (`SigningInWithWebAuthn(WebAuthnSignInState)`):
- **Path:** `FetchingCredentialOptions` → `AssertingCredentials` (biometric / device unlock) → `VerifyingCredentialsAndSigningIn` → `SignedIn`.
- **Actions:** `actions.fetchCredentialOptions` (`startWebAuthnAssertion`), `actions.assertCredentials` (CredentialManager `getAssertion`), `actions.verifyCredentialAndSignIn` (`verifyWebAuthnAssertion`).
- **Exceptions:** `WebAuthnFailedException`, `WebAuthnNotEnabledException`, `WebAuthnCredentialAlreadyExistsException`.
- **Known issues:**
  - `fa4e4a78` device-doesn't-support-passkeys exception handling.
  - `bfcbf8d4` passkey on Android < 13.
  - `d3975dca Prevent crash if KeyStore not available (ex: instant app)` — KeyStore check before passkey use.

---

## 2. Challenge handling (`ConfirmSignInUseCase` + `SignInChallengeState`)

File: `aws-auth-cognito/.../usecases/ConfirmSignInUseCase.kt`

| Challenge | Path | Action |
|---|---|---|
| `SMS_MFA`, `EMAIL_OTP`, `SOFTWARE_TOKEN_MFA` | `ResolvingChallenge(WaitingForAnswer) → Verifying → Verified → SignedIn` | `verifyChallengeAuthAction` (`respondToAuthChallenge`) |
| `SELECT_MFA_TYPE` | `WaitingForAnswer(SELECT_MFA_TYPE)` → user picks MFA type → `Verifying` → Cognito returns the chosen MFA challenge | `getMFATypeOrNull(answer)` validates input — `InvalidParameterException` if not one of three |
| `NEW_PASSWORD_REQUIRED` | `WaitingForAnswer(NEW_PASSWORD_REQUIRED)` → user supplies new password → `respondToAuthChallenge(NEW_PASSWORD_REQUIRED, {PASSWORD})` | |
| `CUSTOM_CHALLENGE` | `WaitingForAnswer(CUSTOM_CHALLENGE)` → app-defined response → `respondToAuthChallenge(CUSTOM_CHALLENGE, {ANSWER})` | |
| `SELECT_CHALLENGE` (USER_AUTH) | User picks factor; `WEB_AUTHN` → `InitiateWebAuthnSignIn`; `Password` → `WaitingForAnswer(PASSWORD)`; `PasswordSrp` → `WaitingForAnswer(PASSWORD_VERIFIER)` | |
| `DEVICE_SRP_AUTH` / `DEVICE_PASSWORD_VERIFIER` | `ResolvingDeviceSRP(DeviceSRPSignInState.InitiatingSRPA)` → device-scoped SRP | `signInActions.startDeviceSRPAuthAction` — uses stored device key. `59a08797 Update SignInChallengeCognitoActions to send DEVICE_KEY for MFA challenges` is the canonical fix. |
| `WEB_AUTHN` | Bridges into 1.7 sign-in flow | |
| `PASSWORD_VERIFIER` | SRP final: `srpActions.verifyPasswordSRPAction` → `SignInCompleted` (or further MFA) | |

**Exceptions:** `CodeMismatchException` (wrong code), `CodeExpiredException` (`ExpiredCodeException`), `LimitExceededException`, `EnableSoftwareTokenMFAException`.

---

## 3. Sign-up + auto sign-in

`SignUpUseCase`: `NotStarted → InitiatingSignUp → AwaitingUserConfirmation | SignedUp`. Direct SDK `signUp(...)`.

`ConfirmSignUpUseCase`: `AwaitingUserConfirmation → ConfirmingSignUp → SignedUp`. SDK `confirmSignUp(...)`.

`AutoSignInUseCase`: extracts `signUpData` from `authSignUpState`, builds `SignInData.AutoSignInData(username, session, metadata, userId)`, sends `AuthenticationEvent.SignInRequested(autoSignInData)`, awaits `SignedIn`.

**Hub:** `SIGNED_IN` (after auto sign-in).

**Historical bugs (all in v2.30.x):**
- `ef37f18c` — missing `SignedUp → SignedOut` transition meant auto sign-in never fired.
- `71771e43` — `SignUpData.session` was lost on wrong-OTP retry; user had to start over.
- `23d6e6d8` — `SIGNED_IN` Hub event was not emitted after auto sign-in.

---

## 4. Sign-out (`SignOutUseCase` — fork takes `userId`)

File: `aws-auth-cognito/.../usecases/SignOutUseCase.kt`

| Variant | Path | Action |
|---|---|---|
| Local (`globalSignOut=false`) | `SignedIn → SigningOut(SignOutState.NotStarted) → SigningOutLocally → SignedOut + AuthZState=Configured` | `signOutActions.localSignOutAction` → store `AmplifyCredential.Empty()` (no SDK call) |
| Global (`globalSignOut=true`) | `… → SigningOutGlobally → RevokingToken → SigningOutLocally → SignedOut` | `signOutActions.globalSignOutAction(userId)` → `globalSignOut(accessToken)` (errors captured in `GlobalSignOutErrorData`) |
| Hosted UI | `SigningOut(SigningOutHostedUI(signedInData, globalSignOut))` → OAuth logout intent → handled in `handleWebUISignInResponse` → `SigningOutGlobally|RevokingToken → SigningOutLocally → SignedOut` | If callback never returns → `Error(UserCancelledException)` |
| Cancel sign-in | `SigningIn(*) + AuthenticationEvent.CancelSignIn() → SignedOut` | `authenticationActions.initiateSignOutAction` |

**Hub:** `SIGNED_OUT`. **Exceptions:** `GlobalSignOutException`, `HostedUISignOutException`, `UserCancelledException`.

**Fork:** `execute(userId)` overload forwards `userId` into the state machine listener (`completeSignOutForUser(userId)`); credential store deletion is userId-scoped.

---

## 5. Fetch auth session (`FetchAuthSessionUseCase` — fork takes `userId`)

File: `aws-auth-cognito/.../usecases/FetchAuthSessionUseCase.kt`

- **Cached path:** `RealAWSCognitoAuthPlugin.fetchAuthSession()` — if `credential.isValid() && !forceRefresh`, returns immediately without state transition.
- **Refresh path:** `SessionEstablished + invalid` → `RefreshingSession(existingCredential, RefreshSessionState.NotStarted)` → `RefreshingUserPoolTokens` → `RefreshingAuthSession` → `Refreshed` → `StoringCredentials` → `SessionEstablished(refreshedCredential)`. Actions: `refreshUserPoolTokensAction` (`initiateAuth(REFRESH_TOKEN_AUTH)`), `refreshAuthSessionAction` (identity-pool credentials), `notifySessionRefreshedAction` (`persistCredentials`).
- **Identity-pool path:** `SigningIn → SignInCompleted → AuthZState.SigningIn → FetchingAuthSession → FetchingIdentity (getId) → FetchingAWSCredentials (getCredentialsForIdentity) → Fetched → StoringCredentials → SessionEstablished(UserAndIdentityPool)`.
- **Guest path:** `AuthZState.Configured + FetchUnAuthSession(userId=null) → FetchingUnAuthSession → FetchingAWSCredentials (no logins) → Fetched → SessionEstablished(IdentityPool)`.
- **Error states:** `AuthZState.Error(SessionError(exception, amplifyCredential))` — recoverable if `amplifyCredential` still has refresh material; else `SessionExpiredException`.

**Notable changes:**
- `3f72bcbd feat(auth): Refresh Token Rotation` (v2.30.0) — refresh response now contains a new refresh token; old token invalidated. Credential store must accept the rotated token.
- `d2674378 fix(auth): Resolve logout issue caused by JWT token serialization breaking change` (v2.30.4) — credential-store deserialization had a breaking change between SDK versions; reads now wrap in try/catch and fall back to `Empty`.
- `3750b653 chore: Add logging for fetch auth session failures` (v2.36.0) — fetch path emits diagnostic logs on failure.

**Fork:** `execute(userId)` overload retrieves userId-scoped credential via `ThreadSafeLifoMap`-backed credential store; events carry `userId` (`AuthorizationEvent.FetchUnAuthSession(userId)`, `RefreshSession(userId, credential)`).

---

## 6. Federation

### `FederateToIdentityPoolUseCase`
- **Path:** `(SignedOut|Error) + AuthorizationEvent.StartFederationToIdentityPool(token, identityId, existingCredential)` → `AuthN.FederatingToIdentityPool` + `AuthZ.FederatingToIdentityPool` → `FetchingIdentity → FetchingAWSCredentials → Fetched` → `FederatedToIdentityPool` + `SessionEstablished(IdentityPoolFederated(token, identityId, awsCredentials))`.
- **Action:** `authorizationActions.initializeFederationToIdentityPool` → `getCredentialsForIdentity(identityId, logins={federatedToken})`.

### `ClearFederationToIdentityPoolUseCase` (fork takes `userId`)
- **Path:** `FederatedToIdentityPool + ClearFederationToIdentityPool(userId)` → `SigningOut → SigningOutLocally → SignedOut → AuthZ.Configured`.
- **Action:** `authenticationActions.initiateSignOutAction(userId, SignOutRequested(SignOutData(userId)), signedInData=null, signOutAllUsers=false)`.
- **Hub:** `FEDERATION_TO_IDENTITY_POOL_CLEARED`.
- **Fork:** required `userId` parameter (two overloads — no-arg and `execute(userId)`). Recent fork commit `a1a15994` removed `clearFederationToIdentityPool` from `RealAWSCognitoAuthPlugin` — sign out is now the public path.

---

## 7. Get current user (`GetCurrentUserUseCase`)

No state-machine event. `requireSignedInState()` → return `AuthUser(signedInData.userId, signedInData.username)`.

**Bug history:** `0dbb4aaa Remove online check from getCurrentUser` (v2.30.0) — removed unnecessary remote validation. `f567b4be Return cached user id and username instead of refetching them` formalized the cached pattern. **Don't add network calls here.**

---

## 8. Delete user (`DeleteUserUseCase`)

- **Path:** `SessionEstablished + DeleteUserEvent.DeleteUser(accessToken, userId)` → `AuthZ.DeletingUser(DeleteUserState.NotStarted)` → `DeletingUser → UserDeleted → initiateSignOut(userId) → SigningOut → SignedOut → AuthZ.Configured`.
- **Action:** `deleteUserActions.initDeleteUserAction(accessToken)` → `deleteUser(accessToken)`.
- **Hub:** `USER_DELETED`.
- **Exceptions:** `UnauthorizedException`, `UserNotFoundException`, `InvalidStateException`.
- **Fork:** `userId` carried in `DeleteUserEvent.DeleteUser(accessToken, userId)`.

---

## 9. Operations that bypass the state machine (direct SDK)

These use cases call Cognito directly. Don't add state-machine events for them.

- **Password:** `ResetPasswordUseCase` (`forgotPassword`), `ConfirmResetPasswordUseCase` (`confirmForgotPassword`), `UpdatePasswordUseCase` (`changePassword`).
- **Attributes:** `FetchUserAttributesUseCase` (`getUserAttributes`), `UpdateUserAttributesUseCase` (`updateUserAttributes`), `ConfirmUserAttributeUseCase` (`verifyUserAttribute`), `ResendUserAttributeConfirmationCodeUseCase` (`getUserAttributeVerificationCode`).
- **MFA preferences:** `UpdateMFAPreferenceUseCase` (`setUserMFAPreference`), `FetchMFAPreferenceUseCase` (`getUserMFAPreference`).
- **TOTP:** `SetupTotpUseCase` (`associateSoftwareToken`), `VerifyTotpSetupUseCase` (`verifySoftwareToken`).
- **Passkey lifecycle:** `ListWebAuthnCredentialsUseCase` (`listWebAuthnCredentials`), `DeleteWebAuthnCredentialUseCase` (`deleteWebAuthnCredential`).

All require a valid access token (call `fetchAuthSession()` first).

---

## 10. Multi-user fork specifics

### `ThreadSafeLifoMap<K, V>`
File: `aws-auth-cognito/.../statemachine/util/ThreadSafeLifoMap.kt`

```kotlin
class LifoMap<K, V>(private val maxSize: Int? = null) {
    @Synchronized fun push(key: K, value: V)   // overflow drops oldest
    @Synchronized fun pop(): V?                // last-in
    @Synchronized fun pop(key: K): V?
    @Synchronized fun peek(): V?
    @Synchronized fun peekKey(): K?
    @Synchronized fun get(key: K): V?
    fun containsKey(key: K): Boolean
    fun isEmpty(): Boolean
    fun size(): Int
    fun clear()
}
```

LinkedHashMap-backed, synchronized. LIFO: most-recently-pushed user is on top. Used to track active user identity for credential-store routing.

### `AWSCognitoAuthCredentialStore`
- `retrieveCredential(userId: String?)` — userId-prefixed key (`"${userId}_${KEY_SESSION}"`); falls back to default key when `userId == null` (preserves data from pre-multi-user installs).
- `saveCredential(credential)` — extracts userId from credential, generates user-scoped key.

### `RealAWSCognitoAuthPlugin` userId overloads
- `fetchAuthSession(userId, onSuccess, onError)` — routes via `stateMachine.getCurrentState(userId) { … }` and userId-bearing `AuthorizationEvent`s.
- `signOut(options, userId, onComplete)`.
- `clearFederationToIdentityPool(userId)`.
- `deleteUser(userId, onSuccess, onError)`.

State-machine events that carry `userId`: `AuthorizationEvent.FetchUnAuthSession(userId)`, `RefreshSession(userId, credential)`, `StartFederationToIdentityPool(token, identityId, existingCredential, userId)`, `AuthenticationEvent.ClearFederationToIdentityPool(userId)`, `DeleteUserEvent.DeleteUser(accessToken, userId)`.

---

## 11. Landmines

Cross-reference when reviewing PRs or debugging:

1. **JWT serialization fragility** (v2.30.4 `d2674378`). Credential-store reads must tolerate parse failure and return `Empty`. Don't add a stricter parser.
2. **Auto sign-in transition gap** (v2.30.x `ef37f18c`). Any change to `SignUpState` → `AuthenticationState` linkage needs a `SignedUp → SignedOut` transition test.
3. **OTP retry session loss** (v2.30.x `71771e43`). `SignUpData.session` must survive `ConfirmingSignUp` errors.
4. **Auto-sign-in Hub event** (v2.30.x `23d6e6d8`). `SIGNED_IN` is emitted by `AutoSignInUseCase`, not the state machine. Don't move it.
5. **`getCurrentUser` must not hit network** (v2.30.0 `0dbb4aaa`, `f567b4be`). Cached only.
6. **Passkey device-support gates** (`fa4e4a78`, `bfcbf8d4`, `d3975dca`). Always check `LocalKeyStoreAvailable`, Android API level, CredentialManager availability before invoking passkey code paths.
7. **Hosted UI race after sign-out** (v2.31.0 `415a7676`). Re-launching hosted UI immediately after sign-out can race; the use case should observe `SignedOut` first.
8. **Refresh token rotation** (v2.30.0 `3f72bcbd`). Credential store must persist the rotated token from each refresh response. Old refresh tokens become invalid.
9. **Device-key MFA** (v2.36.0 `59a08797`). MFA challenge responses include `DEVICE_KEY`; older paths that omit it will fail under "remember this device" Cognito pools.
10. **Multi-user store regression** (fork). Pre-multi-user installs hold credentials under the default key. `retrieveCredential(null)` fallback exists; do not remove it without a migration plan.
11. **Hosted UI process death** (fork `2dce10de`). `CustomTabsManagerActivity` can NPE after process death; the fix is in the fork only — review on every upstream merge.

---

## 12. Merge hot spots (fork ↔ upstream)

When merging a new upstream tag, expect these to conflict:

- `SignOutUseCase` — upstream method signature vs. fork `(userId)` overload.
- `FetchAuthSessionUseCase` — same.
- `ClearFederationToIdentityPoolUseCase` — fork makes `userId` mandatory.
- `RealAWSCognitoAuthPlugin` — fork has `userId` overloads upstream lacks.
- `AuthCategoryBehavior` (Java) — fork adds methods; upstream may add unrelated methods.
- `AuthorizationEvent` — fork carries `userId` in several event subtypes.
- `AWSCognitoAuthCredentialStore` — userId-scoped keys.
- `ThreadSafeLifoMap.kt` — fork-only file; survives merges by default.

When upstream extracts a method from `RealAWSCognitoAuthPlugin` into a new use case (the dominant 2.26→2.36 trend): adopt upstream's use case verbatim, then re-apply the fork's `userId` plumbing on top. Don't keep the fork's hand-rolled implementation.

---

## How to use this doc

- **Implementing a flow:** find the section, read the path + actions, mirror the existing UseCase shape (`.claude/skills/amplify-auth-usecase`).
- **Debugging:** identify the flow from the stack trace (resolver class name, action class name, exception type), then walk the path to find where it diverged from the expected terminal state.
- **Reviewing a PR:** check that the path is unchanged, Hub event still emits on success, exception types still map via `CognitoAuthExceptionConverter`, and (for fork) `userId` is plumbed.
- **Merging upstream:** see §12 + the `amplify-merge-upstream` skill.
