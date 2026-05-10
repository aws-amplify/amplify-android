# Multi-User Auth — Architecture Reference

Technical reference for the Harri multi-user login implementation as it ships at tag **`2.36.0-harri`** on branch `feature/multi-user-on-2.36.0`. Reapplies the contract that was originally delivered at fork tag `2.26.14` on top of upstream `release_v2.36.0`.

This document is the long-form companion to `.claude/skills/multi-user-contract/SKILL.md` (the canonical contract with the 12 invariants). Read this when you want the architecture; read the SKILL doc when you want the rule the next merge must preserve.

---

## 1. Purpose

Allow an Amplify-android-based app to keep **multiple Cognito users signed in simultaneously**, route Auth API calls (`fetchAuthSession`, `signOut`, `clearFederationToIdentityPool`, `federateToIdentityPool`, `deleteUser`) to a specific user by `userId`, and degrade gracefully back to single-user upstream behaviour for callers that don't pass a userId.

The fork's product use case is the Harri Android app, where one device may have a manager and an employee signed in at once and switching between them must not require re-authenticating.

## 2. Architecture overview

Five layers, each with one responsibility:

```
┌──────────────────────────────────────────────────────────────────┐
│  Public API surface                                              │
│  AuthCategoryBehavior (Java) · KotlinAuthFacade (Kotlin) ·       │
│  RxAuthBinding (Rx) · SynchronousAuth (test util)                │
│  Each carries fetchAuthSession(userId,…) / signOut(userId,…)     │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  Plugin façade — AWSCognitoAuthPlugin                            │
│  Public methods are one-liners that call useCaseFactory.<x>()    │
│  .execute(userId?, options?)                                     │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  Use case layer — internal class *UseCase                        │
│  SignOutUseCase, FetchAuthSessionUseCase, …                      │
│  • execute(options) — single-user / upstream-compat              │
│  • execute(userId, options) — per-user routing                   │
│  • execute() no-arg — iterates AuthStateRepo for sign-out        │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  State machine — AuthStateMachine extends StateMachineForAuth    │
│  Routes events with an explicit userId via send(event, userId)   │
│  setAuthState persists per-user state and gates the reset-to-    │
│  default emission on the caller passing an explicit userId       │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  Persistence — AuthStateRepo                                     │
│  ThreadSafeLifoMap (in-memory intermediate states)               │
│  EncryptedKeyValueRepository (persisted SessionEstablished)      │
│    + persisted user-index for allUserIds()                       │
│    + InMemoryFallbackStore when keystore unavailable             │
│  AWSCognitoAuthCredentialStore (per-user-prefixed credential     │
│    keys with fallback to the default key for pre-multi-user      │
│    installs)                                                     │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  Framework userId path — Plugin.configure(json, userId, ctx) →   │
│  Category.configure(config, userId, ctx) routes to               │
│  awsCognitoAuthPlugin → Amplify.configure(config, userId, ctx)   │
└──────────────────────────────────────────────────────────────────┘
```

## 3. Component responsibilities

### `core/` (Java framework)

| File | Responsibility |
|---|---|
| `AuthCategoryBehavior.java` | Public Java API. Adds 4 methods: `fetchAuthSession(userId, …)` ×2 and `signOut(userId, …)` ×2. Existing single-user methods retained. |
| `AuthCategory.java` | Forwards the new methods to `getSelectedPlugin()`. |
| `AuthPlugin.java` | Default impls of the 4 new methods throw `UnsupportedOperationException("Multi-user … is not implemented in this plugin")` — matches the existing pattern for `setUpTOTP` / `verifyTOTPSetup` so non-multi-user plugins fail loudly. |
| `Plugin.java` | New default `configure(JSONObject, String userId, Context)` — empty body so non-auth plugins ignore userId. |
| `Category.java` | New `configure(CategoryConfiguration, String userId, Context)` routes only the auth plugin (`awsCognitoAuthPlugin`) to the userId-aware variant. |
| `Amplify.java` | New `configure(AmplifyConfiguration, String userId, Context)` calls `category.configure(…, userId, …)` — fixes the 2.26.14 dead-code bug where the equivalent overload was passing through the no-userId Category variant. |
| `AuthSignInResult.java` | Two new nullable fields: `userId`, `username`. New 4-arg constructor; existing 2-arg constructor delegates with nulls. |

### `aws-auth-cognito/` (Cognito plugin)

| File | Responsibility |
|---|---|
| `AWSCognitoAuthPlugin.kt` | Public façade. Adds userId overloads of `signOut`, `fetchAuthSession`, `clearFederationToIdentityPool`, `federateToIdentityPool`. Overrides `configure(json, userId, ctx)` to delegate to the no-userId `configure` (per-user routing happens at the use-case layer). |
| `usecases/SignOutUseCase.kt` | When userId is null/empty AND `signOutAllUsers=true` (default), iterates `stateMachine.allUserIds()` and signs each user out sequentially. Result aggregation: `CompleteSignOut` on full success, otherwise the last non-complete per-user result. |
| `usecases/FetchAuthSessionUseCase.kt` | `execute(userId, options)` reads `getStateForUser(userId)` and dispatches refresh / federation events with userId on the event payload. |
| `usecases/ClearFederationToIdentityPoolUseCase.kt` | `execute(userId)` builds a userId-bearing `AuthenticationEvent.EventType.ClearFederationToIdentityPool` and routes through `SignOutUseCase.completeSignOut`. |
| `usecases/DeleteUserUseCase.kt` | `execute(userId)` fetches the access token via the userId-aware `FetchAuthSessionUseCase.execute(userId)` and dispatches `DeleteUserEvent.EventType.DeleteUser` with userId. |
| `usecases/FederateToIdentityPoolUseCase.kt` | `execute(token, provider, userId, options)` reads `getStateForUser(userId)`, dispatches `AuthorizationEvent.EventType.StartFederationToIdentityPool` with userId, routes via `send(event, userId)`. |
| `usecases/WebUiSignInUseCase.kt` | Reads `signedInData.userId` / `.username` on `SessionEstablished` and populates them on the returned `AuthSignInResult`. |
| `helpers/UserPoolSignInHelper.kt` | `signedInResult(userId, username)` helper used by every sign-in flow. |
| `util/StateMachineExtensions.kt` | `sendEventAndGetSignInResult` extracts `signedInData.userId/.username` on success and forwards to `signedInResult`. |
| `actions/AuthenticationCognitoActions.kt` | `initiateSignOutAction` forwards `event.signOutData.userId` to every constructed `SignOutEvent` variant (with fallback to `signedInData.userId` for upstream-compat). |
| `actions/SignOutCognitoActions.kt` | Every chained event in the sign-out action graph (`SignOutGlobally → RevokeToken → SignOutLocally`, etc.) forwards `event.userId`. |
| `options/AWSCognitoAuthSignOutOptions.java` | Adds `signOutAllUsers: Boolean = true` field + `CognitoBuilder.signOutAllUsers(value)` setter + `isSignOutAllUsers()` getter. Default `true` preserves the contract; opt out with `false`. |
| `data/AWSCognitoAuthCredentialStore.kt` | `saveCredential` dual-writes the default key AND a userId-prefixed key derived from `credential.signedInData.userId`. `retrieveCredential(userId)` reads the prefixed key with fallback to the default key (keeps pre-multi-user installs working). `deleteCredential(userId)` removes only the prefixed entry. |

### `aws-auth-cognito/` (state machine)

| File | Responsibility |
|---|---|
| `statemachine/StateMachineForAuth.kt` | Open base class extended by `AuthStateMachine`. Owns `AuthStateRepo`, exposes `send(event, userId, ignoreUserId)`, `getStateForUser(userId)`, `getCurrentState()`, `activeStateKey()`, `allUserIds()`. `setAuthState` writes to `_state` and (when an effective userId is available) to `AuthStateRepo`; the reset-to-default emission only fires when the caller passed a non-empty `userId`. |
| `codegen/data/AuthStateRepo.kt` | `ThreadSafeLifoMap<String, AuthState>` for in-memory intermediate states + `EncryptedKeyValueRepository` (with `InMemoryFallbackStore`) for persisted `SessionEstablished` states. Maintains a `__amplify_auth_state_repo_user_index__` JSON list in the encrypted store so `allUserIds()` returns the union of in-memory keys and persisted keys. |
| `util/LifoMap.kt` | Thread-safe LIFO map keyed by userId. Most-recently-pushed user is the active key. |
| `codegen/data/SignOutData.kt` | Carries `userId` and `signOutAllUsers`. |
| `codegen/events/SignOutEvent.kt` | Each `EventType` variant (`InvokeHostedUISignOut`, `SignOutLocally`, `SignOutGlobally`, `RevokeToken`, `SignOutGloballyError`) carries a nullable `userId`. |
| `codegen/events/AuthenticationEvent.kt` | `ClearFederationToIdentityPool` carries `userId`. |
| `codegen/events/AuthorizationEvent.kt` | `RefreshSession`, `FetchUnAuthSession`, `StartFederationToIdentityPool` carry `userId`. |
| `codegen/events/DeleteUserEvent.kt` | `DeleteUser` carries `userId`. |

### `core-kotlin/` (Kotlin facade)

| File | Responsibility |
|---|---|
| `kotlin/auth/Auth.kt` | Adds `suspend fun fetchAuthSession(userId, options=defaults)` and `suspend fun signOut(userId, options=defaults)`. |
| `kotlin/auth/KotlinAuthFacade.kt` | Wraps the Java callbacks via `suspendCoroutine`. |

### `rxbindings/` (Rx)

| File | Responsibility |
|---|---|
| `RxAuthCategoryBehavior.java` | Adds `fetchAuthSession(userId, …)` ×2 and `signOut(userId, …)` ×2. |
| `RxAuthBinding.java` | Forwards via `toSingle` to the Java delegate. |

### `testutils/` (test helper)

| File | Responsibility |
|---|---|
| `sync/SynchronousAuth.java` | Adds `fetchAuthSession(userId, …)` ×2 and `signOut(userId, …)` ×2 sync helpers. |

## 4. Sequence diagrams

### 4.1 Sign-in (single-user / first user)

```
caller             AuthStateMachine            AuthStateRepo            _state flow
  │                       │                           │                       │
  │ signIn(...)           │                           │                       │
  │──────────────────────►│                           │                       │
  │                       │ send(SignInRequested)     │                       │
  │                       │ process("", SignIn…)      │                       │
  │                       │   resolver → SigningIn    │                       │
  │                       │   setAuthState("",        │                       │
  │                       │                SigningIn) │                       │
  │                       │     _state.tryEmit ───────┼──────────────────────►│
  │                       │     (userId="" so no      │                       │
  │                       │      AuthStateRepo write) │                       │
  │                       │                           │                       │
  │                       │ … Cognito challenge …     │                       │
  │                       │                           │                       │
  │                       │ resolver → SignedIn +     │                       │
  │                       │            SessionEst.    │                       │
  │                       │   setAuthState("",        │                       │
  │                       │       SessionEstablished) │                       │
  │                       │     _state.tryEmit ───────┼──────────────────────►│
  │                       │     userId="" → recover   │                       │
  │                       │     "userA" from          │                       │
  │                       │     SignedInData          │                       │
  │                       │     authStateRepo.put("userA", SE) ──────────────►│ (encrypted store +
  │                       │                           │                          persisted index)
  │                       │     userId arg was empty  │                       │
  │                       │     so NO reset fires.    │                       │
  │                       │     _state stays at       │                       │
  │                       │     SessionEstablished.   │                       │
  │                       │                           │                       │
  │ AuthSignInResult       │                           │                       │
  │ (isSignedIn=true,     │                           │                       │
  │  userId="userA",      │                           │                       │
  │  username="…")        │                           │                       │
  │◄──────────────────────│                           │                       │
```

Caller sees `SignedIn`. `getCurrentState()` returns `SignedIn` because `_state` was not reset. AuthStateRepo has userA persisted, so subsequent `signOut("userA")` / `fetchAuthSession("userA")` finds the entry.

### 4.2 Multi-user sign-in (second user on top of first)

```
state: userA SessionEstablished. _state = SessionEstablished. authStateRepo[userA] = SE.

caller             AuthStateMachine            AuthStateRepo
  │ signIn(userId="userB", …)
  │──────────────────────►│
  │                       │ send(SignInRequested, "userB")
  │                       │ process("userB", SignIn…)
  │                       │   getStateForUser("userB") → default Configured
  │                       │   resolver → SigningIn
  │                       │   setAuthState("userB", SigningIn)
  │                       │     _state.tryEmit(SigningIn)
  │                       │     authStateRepo.put("userB", SigningIn)
  │                       │       (in-memory only, intermediate state)
  │                       │
  │                       │ … challenge …
  │                       │
  │                       │ resolver → SignedIn + SessionEstablished (userB)
  │                       │   setAuthState("userB", SE)
  │                       │     _state.tryEmit(SE for userB)
  │                       │     authStateRepo.put("userB", SE) → persisted
  │                       │     userId arg "userB" non-empty → RESET fires
  │                       │     _state.tryEmit(getDefaultConfiguredState())
  │                       │
  │ AuthSignInResult(true, userId="userB", …)
  │◄──────────────────────│
```

Now `authStateRepo` has both userA and userB persisted. `_state` is back at default (SignedOut+Configured) so a third user could sign in next. `allUserIds()` returns `{userA, userB}`.

### 4.3 Sign out — all users

```
caller             AuthStateMachine             SignOutUseCase            authStateRepo
  │ signOut() // no userId, default options
  │
  │ AWSCognitoAuthPlugin.signOut(onComplete)
  │ → useCaseFactory.signOut().execute()
  │ → execute(userId = null, options = defaults)
  │
  │ options.signOutAllUsers ?: true                  ─► true
  │ stateMachine.allUserIds()                        ─►─►─► {userA, userB}
  │
  │ for uid in {userA, userB}:
  │   signOutOne(uid, options)
  │     getStateForUser(uid) ─►─►─►─►─►─►─►─►─►─►─►─►─►─►─►   SignedIn+SE
  │     completeSignOut(SignOutRequested(userId=uid),
  │                     sendHubEvent=true,
  │                     userId=uid)
  │       state.onSubscription { send(event, uid) }
  │       drop(1).first { authNState is SignedOut }
  │
  │ aggregated result: CompleteSignOut if all users complete cleanly,
  │ else the last non-complete per-user result.
```

Each user's sign-out chain is independent. State-machine's single-thread context serialises them. `allUserIds()` reads from in-memory keys + persisted index, so users that signed in then had the app process killed are still discoverable.

### 4.4 Sign out — one user (multi-user-aware)

```
plugin.signOut("userA", onComplete)
→ useCase.execute("userA", options)

userId is non-null/non-empty → skip iteration, call signOutOne("userA", options)
  getStateForUser("userA") → SignedIn+SE
  completeSignOut(event(userId="userA"), sendHubEvent=true, userId="userA")
    state.onSubscription { stateMachine.send(event, "userA") }
    drop(1).first { authNState is SignedOut + authZState is Configured }
```

`signOutAllUsers` flag has no effect here — explicit userId always targets one user.

### 4.5 Process death recovery (WebUI sign-in mid-OAuth)

```
1. User taps "Sign in with Google" → WebUiSignInUseCase.execute(activity)
2. App launches OAuth intent. AuthStateRepo has authStateMap[? = SigningIn].
3. OS kills the app process.
4. User completes OAuth in Chrome. Chrome delivers the redirect to the app.
5. App is restarted. AuthStateMachine constructed; AuthStateRepo singleton fresh.
6. AuthStateMachine begins processing the redirect. Eventually it builds a
   SignedInData from the ID token claims; resolver emits SessionEstablished.
7. setAuthState("", SessionEstablished):
     _state.tryEmit(SE)
     userId arg was "" so recover from signedInData.userId → "userA"
     authStateRepo.put("userA", SE) → persisted
     userId arg was empty (single-user observer semantics) so NO reset fires.
8. WebUiSignInUseCase reads signedInData.userId/.username and returns
   AuthSignInResult(true, username, userId, DONE).
```

Because the recovery is unconditional on `SessionEstablished`, no separate `pendingSignInUserId` persistence is needed (replaces the planned E6/E7 from the port plan).

## 5. Persistence model

Three storage tiers:

1. **In-memory `LifoMap<String, AuthState>`** — every state mutation that's not `SignedOut` or `SessionEstablished`. The most-recently-pushed key is the "active" user (`activeStateKey()`). Cleared on `SessionEstablished`.

2. **`EncryptedKeyValueRepository`** — only `SessionEstablished` states reach here, keyed by userId. Survives process death.

3. **Persisted user-index** — a single reserved key `__amplify_auth_state_repo_user_index__` in the encrypted store, holding a JSON list of userIds with persisted sessions. Maintained transactionally on `put` and `remove`. Read by `allUserIds()`.

**Fallback:** when `EncryptedKeyValueRepository` cannot construct (corrupted keystore in production, no `androidx.security.crypto.MasterKeys` class in JVM unit tests), the lazy delegate catches `Exception` + `LinkageError` and substitutes `InMemoryFallbackStore`. Multi-user routing keeps working in-memory; persistence is silently lost; the app stays alive.

## 6. State machine extension

The fork extends upstream's `StateMachine` via an open base class **`StateMachineForAuth`** that `AuthStateMachine` inherits from. The base provides:

- `private val _state: MutableSharedFlow<AuthState>(replay=1, …, DROP_OLDEST)` — non-conflated, every transition delivered.
- `val state` — public read-only view.
- `private val authStateRepo: AuthStateRepo` (via singleton).
- `override fun send(event: StateMachineEvent)` — routes to `process(activeStateKey().orEmpty(), event)`.
- `fun send(event: StateMachineEvent, userId: String, ignoreUserId: Boolean = false)` — explicit per-user dispatch.
- `suspend fun getStateForUser(userId: String?): AuthState` — `authStateRepo.get(userId)` or default.
- `suspend fun getCurrentState(): AuthState` — preserves `authStateRepo.activeState() ?: _state.first()`.
- `fun activeStateKey(): String?` — LIFO peek.
- `fun allUserIds(): Set<String>` — union of in-memory keys and persisted index.
- `private fun setAuthState(userId, value)` — writes to `_state`, recovers userId from `SignedInData` on `SessionEstablished`, persists when an effective userId exists, **gates the reset-to-default emission on the caller passing a non-empty `userId` arg**.

The reset gate is the difference between single-user and multi-user behaviour. Single-user callers (no explicit userId) keep `_state` at `SessionEstablished` so `getCurrentState()` returns `SignedIn`, matching upstream `AuthStateMachine`. Multi-user callers (explicit userId) get the reset so a second user can sign in on top.

## 7. Public API matrix

| Layer | Single-user | Multi-user |
|---|---|---|
| Java | `Amplify.Auth.signOut(onComplete)` | `Amplify.Auth.signOut(userId, onComplete)` |
| Java | `Amplify.Auth.signOut(options, onComplete)` | `Amplify.Auth.signOut(userId, options, onComplete)` |
| Java | `Amplify.Auth.fetchAuthSession(onSuccess, onError)` | `Amplify.Auth.fetchAuthSession(userId, onSuccess, onError)` |
| Java | `Amplify.Auth.fetchAuthSession(options, onSuccess, onError)` | `Amplify.Auth.fetchAuthSession(userId, options, onSuccess, onError)` |
| Kotlin | `auth.signOut(options = defaults)` | `auth.signOut(userId, options = defaults)` |
| Kotlin | `auth.fetchAuthSession(options = defaults)` | `auth.fetchAuthSession(userId, options = defaults)` |
| Rx | `rxAuth.signOut() / signOut(options)` | `rxAuth.signOut(userId) / signOut(userId, options)` |
| Rx | `rxAuth.fetchAuthSession() / (options)` | `rxAuth.fetchAuthSession(userId) / (userId, options)` |
| Sync | `syncAuth.signOut() / signOut(options)` | `syncAuth.signOut(userId) / signOut(userId, options)` |
| Plugin (Cognito-only) | `plugin.federateToIdentityPool(token, provider, …)` | `plugin.federateToIdentityPool(token, provider, userId, …)` |
| Plugin (Cognito-only) | `plugin.clearFederationToIdentityPool(…)` | `plugin.clearFederationToIdentityPool(userId, …)` |
| Framework | `Amplify.configure(config, ctx)` | `Amplify.configure(config, userId, ctx)` |
| Result type | `AuthSignInResult(isSignedIn, nextStep)` | `AuthSignInResult(isSignedIn, username, userId, nextStep)` |

`signOut(onComplete)` no-arg semantically means **"sign out all tracked users"** when `AWSCognitoAuthSignOutOptions.signOutAllUsers = true` (default). Set the flag to `false` to revert to single-active-user semantics.

## 8. The 12 invariants

Verbatim from `.claude/skills/multi-user-contract/SKILL.md` §F. Production code must preserve every one:

1. **Credential store fallback** — `retrieveCredential(userId)` tries `<userId>_amplify.session` first, falls back to `amplify.session`.
2. **Active user via LIFO peek** — `activeStateKey()` returns the canonical default user.
3. **Persistence rule** — only `SessionEstablished` reaches the encrypted store.
4. **Per-user listeners** — per-user routing via `send(event, userId)` and `getStateForUser(userId)`.
5. **userId via event payload** — every credential-affecting / sign-out event carries `userId` as a field.
6. **Three signOut shapes** — `signOut(userId)`, `signOut(userId, opts)`, `signOut()` for ALL users.
7. **Plugin lifecycle is multi-user-aware** — `Plugin/Category/Amplify.configure(json, userId, ctx)`.
8. **All bindings carry userId overloads** — Java, Kotlin, Rx, Sync.
9. **`SignedInData.userId` is canonical** — route by userId, never username or email.
10. **`_state` flow is the default state** — resets to default on `SessionEstablished` (only when caller opted into multi-user).
11. **Legacy migration is one-shot** — `AWSCognitoLegacyCredentialStore` reads only until first re-auth on the new build.
12. **All-users sign-out iterates** — when `signOutAllUsers = true`, iterate every user.

## 9. Decision log — port-specific deviations from 2.26.14

The 2.26.14 contract is the spec; the 2.36.0 port deliberately deviates in a few places where the upstream architecture forced or invited a cleaner shape.

| # | Decision | Rationale |
|---|---|---|
| D1 | `StateMachineForAuth` as open base class extended by `AuthStateMachine` | 2.26.14 had a parallel `StateMachineForAuth` class duplicating ~150 lines of engine logic. Extending the upstream engine via a fork-only open base is far easier to maintain across upstream merges. |
| D2 | userId on use case `execute(...)` overloads, not on plugin constructor | 2.26.14 leaked userId into `RealAWSCognitoAuthPlugin` constructor. The 2.36.0 port keeps each call self-describing; the factory creates fresh use cases per call. |
| D3 | All-users iteration in the use case layer, not the action layer | 2.26.14 did `AmplifyCredential.Empty(userId, clearAllSessions=true)` and let the credential-store action bulk-clear. The 2.36.0 port iterates `authStateRepo.allUserIds()` in `SignOutUseCase` and dispatches per-user sign-out events. Cleaner separation, easier to reason about. |
| D4 | `signOutAllUsers` is a field on `AWSCognitoAuthSignOutOptions`, default `true`, customizable | 2.26.14 hardcoded the all-users semantic to the zero-arg shape. The port adds the flag so callers wanting single-active-user can opt out without losing the customization point. |
| D5 | `AWSCognitoAuthPlugin.configure(json, userId, ctx)` delegates to no-userId | userId is informational at boot; per-user routing happens at the use-case layer. Future work may load the supplied userId's persisted state eagerly. |
| D6 | Encrypted store is wrapped in a try/catch that falls back to in-memory | Corrupted keystore in production OR no Android keystore in JVM unit tests would crash AuthStateRepo. Graceful degradation is the production-correct behaviour AND makes JVM tests work. |
| D7 | `setAuthState` reset-to-default gated on caller passing explicit userId | The reset-to-default emission is multi-user-only behaviour. Firing it for single-user observers (callers that don't pass userId) regresses upstream semantics where `getCurrentState()` returns `SignedIn` after `SessionEstablished`. |
| D8 | `setAuthState` recovers userId from `SignedInData` even when caller didn't pass one | Without recovery, a sign-in dispatched without an explicit userId (first sign-in, OAuth redirect, process-death restore) would never reach `AuthStateRepo`. Recovery is what makes subsequent multi-user-aware calls find the user. Persistence happens via recovery; the reset-emission stays gated on caller intent. |
| D9 | `Amplify.configure(config, userId, ctx)` correctly delegates to `Category.configure(config, userId, ctx)` | The 2.26.14 implementation had a dead-code bug where it called the no-userId `Category.configure`, dropping the userId before it reached the auth plugin. The 2.36.0 port fixes it from the start. |

## 10. Testing approach

### Unit tests
- **AuthStateRepoTest** — persistence rules, allUserIds, persisted-index round-trip across fresh repo instances (process-death simulation), in-memory clear behaviour. Uses `AuthStateRepo.createForTest(InMemoryKeyValueRepository)` to inject a fake store.
- **AWSCognitoAuthCredentialStoreTest** — per-user dual-write, retrieve(userId) reads the prefixed key, fallback to default key for pre-multi-user installs, delete(userId), two-user no-collision.
- **SignOutUseCaseTest** — five new multi-user cases on top of the existing single-user suite: iterates each user when no userId + signOutAllUsers=true, signs out only the active user when signOutAllUsers=false, explicit userId ignores signOutAllUsers, falls back to active user when allUserIds is empty, aggregated result surfaces partial when one of several users partially signs out.
- **FetchAuthSessionUseCaseTest, ClearFederationToIdentityPoolUseCaseTest, DeleteUserUseCaseTest, FederateToIdentityPoolUseCaseTest** — one assertion-rich multi-user test per use case.
- **AuthValidationTest** — existing single-user end-to-end suite; the reset-gate decision was made to keep this suite green.

### Feature tests
- **AWSCognitoAuthPluginFeatureTest** — JSON-fixture-based integration suite. 11 sign-in / confirmSignIn / autoSignIn success fixtures were updated to include `userId="userId"` and `username="username"` matching the populated `AuthSignInResult` fields.

### Tests deferred (manual)
- **I4 — four canonical multi-user flows on a device**:
  - (a) Sign-in user A → fetch session → sign-out user A.
  - (b) Sign-in user A, sign-in user B (without signing out A) → fetch session A and B independently.
  - (c) Federate to identity pool → clear federation.
  - (d) Sign-out all users (zero-arg `signOut()`) — verify both A and B credential keys removed.

## 11. Build & verification commands

```sh
# Per-module verification (runs in ~1 minute each)
./gradlew :core:test :core:ktlintCheck :core:checkstyle :core:apiCheck
./gradlew :core-kotlin:test :core-kotlin:ktlintCheck :core-kotlin:apiCheck
./gradlew :aws-auth-cognito:test :aws-auth-cognito:ktlintCheck :aws-auth-cognito:checkstyle :aws-auth-cognito:apiCheck
./gradlew :rxbindings:test :rxbindings:ktlintCheck :rxbindings:checkstyle :rxbindings:apiCheck

# Cross-module sanity (compiles all, runs apiCheck across all)
./gradlew compileDebugKotlin --continue
./gradlew apiCheck --continue

# Full clean build (10–15 min)
./gradlew clean build
```

After any public-API change, run `./gradlew :<module>:apiDump` then commit the resulting `.api` file diff.

## 12. References

- **Contract**: `.claude/skills/multi-user-contract/SKILL.md` (canonical, with the 12 invariants).
- **Port plan**: `.claude/plans/multi-user-port-plan-2.36.0.md` (historical reference for the 2.26.14→2.36.0 reapplication).
- **Coding rules**: `.claude/rules.md`.
- **Project guide**: `CLAUDE.md`.
- **Handoff**: `documents/MultiUserAuth_Handoff.md`.
- **Tag**: `2.36.0-harri` at `dc9d4973`.
- **Branch**: `feature/multi-user-on-2.36.0`.
- **Upstream baseline**: `release_v2.36.0`.
