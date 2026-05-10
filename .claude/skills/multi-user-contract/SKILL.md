---
name: multi-user-contract
description: Canonical reference for the Harri multi-user login implementation, shipped at tag 2.26.14 and reapplied on top of upstream release_v2.36.0 — public API additions, AuthStateRepo + ThreadSafeLifoMap architecture, per-user state machine, credential store key scheme, event payload contracts, and the 12 invariants the upstream merge must preserve. Use when working on any multi-user auth path, auditing the current branch for migration drift, or merging an upstream release that touches auth.
---

# Harri multi-user login — canonical contract (tag 2.26.14)

The shipped reference implementation. Built on upstream `release_v2.26.0`. Delivered across 9 commits authored by Sami Khleaf and Rami Hussien between 2024-12-24 and 2025-02-10. Tag: `2.26.14`. Diff vs. upstream: 161 files, ~2000 net lines.

When working on any multi-user auth code path, this is the contract that must hold. When merging an upstream release into the fork, this is the list of invariants to preserve.

To read code at this tag: `git show 2.26.14:<path>` for individual files, `git diff release_v2.26.0..2.26.14 -- <file>` for the upstream→fork delta.

## Status on `feature/multi-user-on-2.36.0`

The contract is fully reapplied on top of upstream `release_v2.36.0` as of 2026-05-10. The port deviates from 2.26.14's literal diff because upstream's 2.26→2.36 use-case extraction moved every attachment point: `RealAWSCognitoAuthPlugin.kt` and `KotlinAuthFacadeInternal.kt` are gone, and the auth behaviour now lives in 32 internal use cases under `aws-auth-cognito/.../usecases/` plus a 604-line `AWSCognitoAuthPlugin` façade and an `AuthUseCaseFactory`.

**Engine layer.** Where 2.26.14 duplicated ~150 lines of state-machine engine code into a parallel `StateMachineForAuth`, the 2.36.0 port extracts a fork-only `StateMachineForAuth` open class that `AuthStateMachine` inherits from. It owns `AuthStateRepo` directly and exposes a non-conflated `MutableSharedFlow<AuthState>` (`replay = 1`, `extraBufferCapacity = 10`, `DROP_OLDEST`) — matching upstream's flow shape — plus `send(event, userId, ignoreUserId)`, `getStateForUser(userId)`, `getCurrentState()`, `activeStateKey()`, and `allUserIds()`. There is no `setCurrentState` override anymore; the routing happens via a private `setAuthState(userId, value)` invoked from `process()`. **Do not look for `setCurrentState` on the engine** — it does not exist on this branch.

**`AuthStateRepo` user index.** A reserved key `__amplify_auth_state_repo_user_index__` inside the encrypted store now holds a JSON-encoded set of every userId with a persisted session. Maintained transactionally on `put` (when persisting `SessionEstablished`) and `remove`. `allUserIds()` returns the union of in-memory keys and this index — read by `SignOutUseCase` to enumerate users for the all-users sign-out path. Without this index a successful sign-in clears the in-memory map (so the second user can stack), so a fresh process can't otherwise discover persisted users.

**Customizability of `signOut()` zero-arg.** `AWSCognitoAuthSignOutOptions.signOutAllUsers: Boolean = true` lets callers opt out of all-users semantics with `signOutAllUsers = false`. Default `true` preserves the 2.26.14 contract: a no-userId sign out iterates every user in `AuthStateRepo`. The flag has no effect on `signOut(userId, ...)` — that path always targets the supplied userId.

**Sign-in result enrichment.** `AuthSignInResult` gained nullable `userId` and `username` fields plus a 4-arg constructor. `UserPoolSignInHelper.signedInResult(userId, username)` and `WebUiSignInUseCase` populate them from `SignedInData` on the `SessionEstablished` branch — callers can chain `fetchAuthSession(userId, ...)` or `signOut(userId, ...)` directly off the result.

**Plugin lifecycle override.** `AWSCognitoAuthPlugin.configure(json, userId, ctx)` overrides the default no-op so the framework userId path actually reaches the plugin (the 2.36.0 base inherits an empty default from `Plugin.configure(JSONObject, String, Context)`). The override delegates to the no-userId configure since per-user routing happens at the use-case layer. **The 2.26.14 dead-code bug** — `Amplify.configure(config, userId, ctx)` calling the no-userId `Category.configure` — is fixed in this port; the userId-aware `Amplify.configure` calls `category.configure(categoryConfiguration, userId, context)`.

**`setAuthState` userId recovery.** `StateMachineForAuth.setAuthState(userId, value)` falls back to `signedInData.userId` when the dispatching userId is empty AND the new state is `SessionEstablished`. This handles the three cases where a sign-in is dispatched without an explicit userId — first sign-in (no active key), OAuth/WebUI redirect (userId not known until tokens arrive), and process-death recovery (the redirect callback rebuilds `SignedInData` from the ID token). Replaces the explicit `pendingSignInUserId` persistence that 2.26.14 used for the WebUI process-death case (planned as E6/E7 in the port plan, dropped because the recovery is unconditional via this fallback).

**`SignOutEvent` carries userId implicitly via `SignedInData`, not as an explicit field.** Intentional deviation from 2.26.14 §C.2. The 2.26.14 layout repeated `userId` on every `SignOutEvent.EventType` variant (`InvokeHostedUISignOut(userId, …)`, `SignOutGlobally(userId, …)`, etc.). The 2.36.0 port keeps the upstream variant shape (no explicit `userId`) because every variant already holds a `SignedInData` whose `userId` is the canonical identifier (invariant 9), and the state-machine `process(userId, event)` context routes per-user without needing the event to declare it again. Action-layer code that needs the userId reads `event.signedInData.userId`. **If you ever need to dispatch a downstream event from outside the state machine context** (i.e. without going through `process(userId, …)`), use `dispatcher.send(event, userId)` explicitly — the userId-routing overload on `EventDispatcher`/`StateMachineForAuth` is what makes the chain work.

**Path-mapped quick reference for 2.36.0:**

| 2.26.14 attachment point | 2.36.0 path |
|---|---|
| `RealAWSCognitoAuthPlugin.signOut(userId, ...)` | `AWSCognitoAuthPlugin.signOut(userId, ...)` (delegates to `useCaseFactory.signOut().execute(userId, options)`) |
| `RealAWSCognitoAuthPlugin._signOut`, `_fetchAuthSession`, etc. | `aws-auth-cognito/.../usecases/SignOutUseCase.kt`, `FetchAuthSessionUseCase.kt`, etc. |
| `KotlinAuthFacadeInternal.signOut()` | `KotlinAuthFacade.signOut(userId, options)` (suspend, default args) |
| `RealAWSCognitoAuthPlugin.configure(json, userId, ctx)` | `AWSCognitoAuthPlugin.configure(json, userId, ctx)` (override on the public plugin class) |
| `StateMachineForAuth` (2.26.14 parallel class) | `aws-auth-cognito/.../statemachine/StateMachineForAuth.kt` (open base class extended by `AuthStateMachine`) |
| 2.26.14 had no user index | `AuthStateRepo.allUserIds()` + `__amplify_auth_state_repo_user_index__` key |

## TL;DR — three things that are not obvious

1. **The fork extended the framework, not just auth.** `Plugin.configure()`, `Category.configure()`, and `Amplify.configure()` all gained a `userId` overload. The plugin lifecycle is multi-user-aware at the framework level.
2. **`AuthStateRepo` is the hidden architecture.** It holds per-user state via a `ThreadSafeLifoMap` (in-memory, intermediate states) plus an encrypted key-value store (persistent, only after `SessionEstablished`). The state machine's public `_state` flow is the *default* state, not the active user's state — per-user state is retrieved via `getCurrentState(userId)` / `listen(userId, …)`.
3. **`signOut()` with no userId = sign out ALL users by default, customizable.** Added in commit `2e3ea6cf`. The `SignOutData.signOutAllUsers: Boolean` flag drives action-layer iteration. **Customizability (added during the 2.36.0 port):** `AWSCognitoAuthSignOutOptions.signOutAllUsers: Boolean = true` lets callers opt out of all-users semantics with `signOutAllUsers = false`. Default `true` preserves the original contract. Sharp footgun if forgotten on an upstream merge.

## Commit narrative (release_v2.26.0 → 2.26.14)

| Commit | Author | Theme |
|---|---|---|
| `a6f5966c` | sami@harri.com (2024-12-24) | **Core multi-user** — events/states carry `userId`, per-user credential keys, `AuthStateRepo` + `LifoMap` |
| `9988bead` | sami@harri.com (2024-12-26) | **Migration support** — legacy credential store fallback; switch from username- to userId-keyed state |
| `9230bd65` | Rami Hussien (2024-12-27) | Test fixes / cleanup |
| `2e3ea6cf` | Rami Hussien (2025-01-08) | **All-users sign out** — zero-arg `signOut()` variant |
| `1c4c3685` | sami@harri.com (2025-01-15) | **Refresh-token fix** — userId plumbing through `RefreshSession` events |
| `ec34732c`–`891002ab` | Rami Hussien (2025-02) | SQLCipher + MapLibre 16KB page alignment (unrelated to multi-user) |

## A. Public API surface

### A.1 `AuthCategoryBehavior` (Java) — `core/.../auth/AuthCategoryBehavior.java`

New methods (existing single-user methods retained for backward compat):

```java
// Fetch
void fetchAuthSession(@NonNull String userId, @NonNull Consumer<AuthSession> onSuccess,
                      @NonNull Consumer<AuthException> onError);
void fetchAuthSession(@NonNull String userId, @NonNull AuthFetchSessionOptions options,
                      @NonNull Consumer<AuthSession> onSuccess, @NonNull Consumer<AuthException> onError);

// Sign out — three new shapes
void signOut(@NonNull String userId, @NonNull Consumer<AuthSignOutResult> onComplete);
void signOut(@NonNull String userId, @NonNull AuthSignOutOptions options,
             @NonNull Consumer<AuthSignOutResult> onComplete);
void signOut(@NonNull Consumer<AuthSignOutResult> onComplete);   // ALL users
```

`deleteUser` does NOT take a `userId` — it operates on the currently authenticated user (whichever that is in the active state).

### A.2 `AuthCategory` (delegation) — `core/.../auth/AuthCategory.java`

Forwards to plugin. Note the no-userId `signOut` delegates with empty string:
```java
public void signOut(@NonNull Consumer<AuthSignOutResult> onComplete) {
    getSelectedPlugin().signOut("", onComplete);   // "" = all-users sentinel
}
```

### A.3 `AuthSignInResult` — `core/.../auth/result/AuthSignInResult.java`

Two new nullable fields, accessor methods, and a constructor that carries them:
```java
private final String userId;     // @Nullable
private final String username;   // @Nullable
public AuthSignInResult(boolean isSignedIn, String username, String userId, AuthNextSignInStep nextStep);
@Nullable public String getUserId();
@Nullable public String getUsername();
```

These let the caller route subsequent `fetchAuthSession(userId)` / `signOut(userId)` calls without re-reading the credential store.

### A.4 Framework-level plumbing (the surprising bit)

**`Plugin.java`** — new default method:
```java
default void configure(JSONObject pluginConfiguration, String userId, @NonNull Context context) throws AmplifyException {}
```

**`Category.java`** — `configure` takes a `userId` and routes it specifically to `awsCognitoAuthPlugin`:
```java
public synchronized void configure(@NonNull CategoryConfiguration configuration, final String userId,
                                   @NonNull Context context) throws AmplifyException {
    if (pluginKey.equals("awsCognitoAuthPlugin")) {
        plugin.configure(pluginConfig, userId, context);
    } else {
        plugin.configure(pluginConfig, context);
    }
}
```

**`Amplify.java`** — overload accepting `userId`:
```java
public static void configure(@NonNull AmplifyConfiguration configuration, final String userId,
                             @NonNull Context context) throws AmplifyException;
```

**⚠️ Confirmed dead code at both 2.26.14 and `feature/upgrade-to-2.34.0-with-multi-user` HEAD.** `Amplify.configure(config, userId, ctx)` accepts the `userId` parameter at `core/.../Amplify.java:158` but the body at `core/.../Amplify.java:179` calls `category.configure(categoryConfiguration, context)` — the no-userId overload. The userId-routing variant on `Category.configure(...)` (at `core/.../Category.java:83`, which routes specifically to `awsCognitoAuthPlugin`) is never reached. Pre-existing bug; not a regression. **Fix it as part of the port to 2.36.0**: change `Amplify.java:179` to call `category.configure(categoryConfiguration, userId, context)`.

### A.5 Bindings

**`KotlinAuthFacade.kt`** — suspend overloads:
```kotlin
suspend fun fetchAuthSession(userId: String): AuthSession
suspend fun fetchAuthSession(userId: String, options: AuthFetchSessionOptions): AuthSession
suspend fun signOut(userId: String): AuthSignOutResult
suspend fun signOut(userId: String, options: AuthSignOutOptions): AuthSignOutResult
suspend fun clearFederationToIdentityPool(userId: String)
```

**`RxAuthBinding.java`** — `fetchAuthSession(userId, options)` overload added; `signOut()` (no args) delegates with `""` (all-users).

**`SynchronousAuth.java`** — test util mirrors the Java surface.

## B. Credential store

### B.1 `AWSCognitoAuthCredentialStore.kt` — userId-prefixed keys

```kotlin
override fun saveCredential(credential: AmplifyCredential) {
    val userId = when (credential) {
        is AmplifyCredential.UserPool      -> credential.signedInData.userId
        is AmplifyCredential.IdentityPool  -> credential.identityId
        else -> null
    }
    val sessionKey = userId?.let { generateKeyWithPrefix(it + "_", Key_Session) }
    keyValue.put(sessionKey ?: generateKey(Key_Session), serializeCredential(credential))
}

override fun retrieveCredential(userId: String?): AmplifyCredential {
    if (userId == null) return deserializeCredential(null, keyValue.get(generateKey(Key_Session)))
    return deserializeCredential(userId, keyValue.get(generateKeyWithPrefix(userId + "_", Key_Session)))
        .takeIf { it !is AmplifyCredential.Empty }
        ?: deserializeCredential(null, keyValue.get(generateKey(Key_Session)))   // fallback to default key
}

override fun deleteCredential(userId: String?) {
    userId?.let { keyValue.remove(generateKeyWithPrefix(it + "_", Key_Session)) }
        ?: keyValue.remove(generateKey(Key_Session))
}
```

**Key format:** `<userId>_amplify.session` (per-user) or `amplify.session` (default / pre-multi-user installs). The fallback in `retrieveCredential` is what makes upgrades safe — the first sign-in on the new build reads the legacy key, the next save writes the per-user key.

### B.2 `AWSCognitoLegacyCredentialStore.kt`

Reads the pre-multi-user key format. Used during migration; once a user re-authenticates on the new build, all subsequent reads/writes go through the per-user scheme.

### B.3 `AuthStateRepo.kt` (NEW)

The hidden architecture. Per-user state, in-memory + persistent.

```kotlin
class AuthStateRepo(context: Context) {
    private val authStateMap = LifoMap.empty<String, AuthState>()                  // in-memory
    private val encryptedStore = EncryptedKeyValueRepository(context, PREF_KEY)     // persistent

    fun put(key: String, value: AuthState) {
        if (value.isSignedOut) { remove(key); return }
        if (value.isSessionEstablished) {
            encryptedStore.put(key, serializeAuthNAndZState(...))
            authStateMap.clear()                  // clear in-memory; allows re-login
            return
        }
        authStateMap.push(key, value)             // intermediate states stay in-memory
    }

    fun get(key: String): AuthState? = authStateMap.get(key)
        ?: deserializeAuthNAndZState(encryptedStore.get(key))

    fun activeState(): AuthState?    = authStateMap.peek()
    fun activeStateKey(): String?    = authStateMap.peekKey()
}
```

Three rules:
- **`SignedOut`** → remove from both stores.
- **`SessionEstablished`** → persist to encrypted store + clear in-memory map (so a fresh login can stack on top).
- **Intermediate** (signing in, MFA, etc.) → in-memory only.

`activeState()` / `activeStateKey()` return the most-recently-active user — used by `RealAWSCognitoAuthPlugin` to default to "current user" when the caller omits `userId`.

### B.4 `LifoMap.kt` (NEW) — `LifoMap<K, V>`

```kotlin
class LifoMap<K, V>(private val maxSize: Int? = null) {
    @Synchronized fun push(key: K, value: V)        // overflow drops oldest (FIFO eviction)
    @Synchronized fun pop(): V?
    @Synchronized fun pop(key: K): V?
    @Synchronized fun peek(): V?                    // last-in
    @Synchronized fun peekKey(): K?
    @Synchronized fun get(key: K): V?
    @Synchronized fun containsKey(key: K): Boolean
    fun isEmpty(): Boolean
    fun size(): Int
    fun clear()
}
```

LinkedHashMap-backed. **LIFO** for `pop`/`peek`. **FIFO eviction** when `maxSize` is exceeded (oldest entry removed). All mutating ops `@Synchronized`.

### B.5 `AuthCredentialStore` interface — userId on every read/delete

```kotlin
interface AuthCredentialStore {
    fun saveCredential(credential: AmplifyCredential)
    fun saveDeviceMetadata(username: String, deviceMetadata: DeviceMetadata)
    fun saveASFDevice(device: AmplifyCredential.ASFDevice)

    fun retrieveCredential(userId: String?): AmplifyCredential
    fun retrieveDeviceMetadata(username: String): DeviceMetadata
    fun retrieveASFDevice(): AmplifyCredential.ASFDevice

    fun deleteCredential(userId: String?)
}
```

Note: device metadata is keyed by **username**, not userId. ASF device is global (not per-user).

### B.6 Deletions

| Removed | Replacement |
|---|---|
| `core/.../store/AmplifyKeyValueRepository.kt` | `EncryptedKeyValueRepository` + `KeyValueRepositoryFactory` |
| `core/.../store/InMemoryKeyValueRepositoryProvider.kt` | `KeyValueRepositoryFactory` (factory pattern handles both backings) |

## C. State machine extensions

### C.1 `StateMachineForAuth.kt` — per-user routing

```kotlin
class StateMachineForAuth(
    resolver: StateMachineResolver<AuthState>,
    val environment: AuthEnvironment,
    private val dispatcherQueue: CoroutineDispatcher = Dispatchers.Default,
    private val executor: EffectExecutor = ConcurrentEffectExecutor(dispatcherQueue),
    private val initialState: AuthState? = null
) : EventDispatcher {
    private val authStateRepo: AuthStateRepo = AuthStateRepo.getInstance(environment.context)
    private val _state = MutableStateFlow(initialState ?: resolver.defaultState)

    private fun getAuthStateForUser(userId: String?, ignoreUserId: Boolean = false): AuthState {
        if (userId.isNullOrEmpty() || ignoreUserId) return _state.value
        return authStateRepo.get(userId) ?: authStateRepo.getDefaultConfiguredState()
    }

    private fun setAuthState(userId: String, value: AuthState) {
        if (userId.isNotEmpty()) authStateRepo.put(userId, value)
        _state.value = if (value.isSessionEstablished) authStateRepo.getDefaultConfiguredState() else value
    }

    fun listen(userId: String, token: StateChangeListenerToken,
               listener: (AuthState) -> Unit, onSubscribe: OnSubscribedCallback?)
    fun getCurrentState(userId: String, completion: (AuthState) -> Unit)
    override fun send(event: StateMachineEvent, userId: String, ignoreUserId: Boolean)
}
```

Behaviour to know cold:
- `_state.value` = **default** state (not active user's state). When a user reaches `SessionEstablished`, `_state` is reset to default — that's intentional, so a second login can stack.
- `listen(userId, …)` filters events to that userId. Multiple users can have concurrent listeners.
- `send(event, userId)` dispatches with userId-scoped state from `AuthStateRepo`.
- `ignoreUserId = true` falls back to the public `_state` flow (used for global, user-agnostic operations).

### C.2 Event types carrying userId

**`AuthEvent.EventType`:** `ConfigureAuth(configuration, userId)`.

**`SignOutEvent.EventType`:**
```kotlin
data class InvokeHostedUISignOut(userId, signOutData, signedInData)
data class SignOutLocally(userId, signedInData?, hostedUIErrorData?, globalSignOutErrorData?,
                          revokeTokenErrorData?, signOutAllUsers: Boolean = false)
data class SignOutGlobally(userId, signedInData, hostedUIErrorData?)
data class RevokeToken(userId, signedInData, hostedUIErrorData?, globalSignOutErrorData?)
```

**`DeleteUserEvent.EventType`:**
```kotlin
data class DeleteUser(accessToken, userId = "", username = "")
data class UserDeleted(id = "", userId)
data class ThrowError(exception, userId, signOutUser: Boolean)
```

**Pattern:** every event that affects credential state or sign-out carries `userId` explicitly. Actions extract userId from the event payload — never from environment / globals.

### C.3 `SignOutData` — multi-user fields

```kotlin
data class SignOutData(
    val userId: String,
    val globalSignOut: Boolean = false,
    val browserPackage: String? = null,
    val bypassCancel: Boolean = false,
    val signOutAllUsers: Boolean = false   // <-- the all-users flag
)
```

### C.4 `SignedInData` — userId is canonical

```kotlin
@Serializable
data class SignedInData(
    val userId: String,         // PRIMARY identifier; never use username for routing
    val username: String,
    val signedInDate: Date,
    val signInMethod: SignInMethod,
    val cognitoUserPoolTokens: CognitoUserPoolTokens,
    val email: String? = null
)
```

## D. Plugin entry point — `RealAWSCognitoAuthPlugin`

Plugin instance is constructed with a `userId: String?` (per-instance default). Public methods come in pairs:

```kotlin
fun fetchAuthSession(onSuccess, onError) {
    val activeUserId = authStateMachine.authStateRepo.activeStateKey().orEmpty()
    _fetchAuthSession(activeUserId, onSuccess)
}
fun fetchAuthSession(userId: String, onSuccess, onError) { _fetchAuthSession(userId, onSuccess) }
fun fetchAuthSession(options: AuthFetchSessionOptions, onSuccess, onError) { /* uses activeStateKey */ }
fun fetchAuthSession(userId: String, options: AuthFetchSessionOptions, onSuccess, onError) { /* uses userId */ }

fun signOut(userId: String, onComplete) = signOut(userId, AuthSignOutOptions.builder().build(), false, onComplete)
fun signOut(userId: String, options: AuthSignOutOptions, signOutAllUsers: Boolean = false, onComplete)
fun signOut(onComplete) { /* iterate ALL users in authStateRepo */ }
```

The default behaviour when caller omits `userId` is to pull `authStateRepo.activeStateKey()` (most recently active user). The zero-arg `signOut(onComplete)` is the all-users variant.

## E. Action layer — userId via event payload

Actions are stateless w.r.t. user. They receive `userId` as a constructor parameter on the event, propagate it through every dispatched downstream event, and pass it to the credential store.

Example from `SignOutCognitoActions.kt`:
```kotlin
override fun globalSignOutAction(userId: String, event: SignOutEvent.EventType.SignOutGlobally) =
    Action<AuthEnvironment>("GlobalSignOut") { id, dispatcher ->
        val accessToken = event.signedInData.cognitoUserPoolTokens.accessToken
        val evt = try {
            cognitoAuthService.cognitoIdentityProviderClient?.globalSignOut(...)
            SignOutEvent(SignOutEvent.EventType.RevokeToken(userId, event.signedInData, ...))
        } catch (e: Exception) {
            SignOutEvent(SignOutEvent.EventType.SignOutGloballyError(
                userId = userId,                          // preserve through error path
                signedInData = event.signedInData,
                ...
            ))
        }
        dispatcher.send(evt)
    }
```

Hard rule: **never** fetch userId from a global / environment / `activeStateKey()` inside an action. Always from the event.

## F. Twelve invariants the upstream merge must preserve

1. **Credential store fallback** — `retrieveCredential(userId)` tries `<userId>_amplify.session` first, falls back to `amplify.session`. Keeps pre-multi-user installs working.
2. **Active user via LIFO peek** — `AuthStateRepo.activeStateKey()` returns the canonical "default" user for no-userId calls. Don't replace with a `currentUserId` field.
3. **Persistence rule** — only `SessionEstablished` states reach the encrypted store. Intermediate states (signing in, MFA) stay in `LifoMap`.
4. **Per-user listeners** — `listen(userId, token, listener)` MUST filter to that userId only. Listener cross-talk between users is a bug.
5. **userId via event payload** — every credential-affecting / sign-out event carries `userId` as a field. Actions read it from there. No environment-level userId.
6. **Three signOut shapes** — `signOut(userId)` (one user), `signOut(userId, opts)` (with options), `signOut()` (ALL users). The empty-string `""` userId on the inside is the all-users sentinel.
7. **Plugin lifecycle is multi-user-aware** — `Plugin.configure(json, userId, ctx)` default no-op; `Category.configure(config, userId, ctx)` routes to `awsCognitoAuthPlugin`; `Amplify.configure(config, userId, ctx)` is the entry point.
8. **All bindings carry userId overloads** — `KotlinAuthFacade`, `RxAuthBinding`, `SynchronousAuth`. Non-userId forms delegate with `""` for all-users semantics.
9. **`SignedInData.userId` is canonical** — actions and state machine route by `userId`, never `username` or `email`.
10. **`_state` flow is the default state** — `StateMachineForAuth._state.value` resets to default on `SessionEstablished`. Per-user state lives in `AuthStateRepo`. Don't change the public flow to track the active user.
11. **Legacy migration is one-shot** — `AWSCognitoLegacyCredentialStore` reads happen only until the first re-auth on the new build. After that, all paths use the per-user scheme.
12. **All-users sign-out iterates** — when `signOutAllUsers = true` in `SignOutData`, the action layer iterates every user in `AuthStateRepo` and dispatches per-user sign-out events. Not a single global event.

## G. Deleted/superseded

| File | Why | Replacement |
|---|---|---|
| `core/.../store/AmplifyKeyValueRepository.kt` | Generic abstraction unused | `EncryptedKeyValueRepository` + `KeyValueRepositoryFactory` |
| `core/.../store/InMemoryKeyValueRepositoryProvider.kt` | Static provider replaced | `KeyValueRepositoryFactory` (handles persistent and in-memory) |

## How to use this doc

- **Implementing a multi-user feature**: read §A (which API to extend), §C (which event/state to add userId to), §E (where the action receives it).
- **Reviewing a PR**: walk §F. Any invariant violated is a code-review block.
- **Merging an upstream release**: cross-reference §F against the diff. Anything in §A/C/D/E that upstream changed without preserving §F is a conflict. The `amplify-merge-upstream` skill is the playbook; this skill is the contract.
- **Auditing the current branch for migration drift**: diff the file lists in §A–E from this doc against `feature/upgrade-to-2.34.0-with-multi-user`. Files modified in 2.26.14 but not on the current branch are likely places where the migration dropped multi-user wiring.
