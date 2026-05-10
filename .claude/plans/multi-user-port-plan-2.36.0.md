# Multi-user port plan — fresh from `release_v2.36.0`

**Status**: planned, awaiting approval. Not yet executed.
**Branch**: `feature/multi-user-on-2.36.0` (off `release_v2.36.0`).
**Source of truth**: `.claude/skills/multi-user-contract/SKILL.md` (the 12 invariants from tag 2.26.14).
**Target**: upstream `release_v2.36.0` baseline.

This plan is the precision blueprint for re-applying the Harri multi-user contract on top of upstream Amplify 2.36.0, after upstream's 2.26→2.36 use-case extraction. **One commit per numbered item.** Build + tests verified between commits. Plan deviates from 2.26.14's literal diff because the attachment points moved (RealAWSCognitoAuthPlugin no longer exists; logic lives in 32 use cases now).

---

## Executive summary

Upstream 2.36.0 deleted `RealAWSCognitoAuthPlugin.kt` and `KotlinAuthFacadeInternal.kt` entirely. All auth behaviour now lives in 32 internal use cases under `aws-auth-cognito/.../usecases/` plus an `AuthUseCaseFactory(189 lines)` and the public `AWSCognitoAuthPlugin(604 lines)`. Each public plugin method is a one-liner: `enqueue(...) { useCaseFactory.<x>().execute(...) }`. State machine engine is unchanged conceptually but lost ~107 lines (cleaner `state` SharedFlow exposure, removed listener tokens).

The fork's multi-user invariants must reattach to this new shape. ~7 use cases need a userId-bearing `execute` overload (the rest read active state). The state-machine engine needs **one upstream-line change** (`setCurrentState` from `private` to `protected open`) to enable a clean override in `AuthStateMachine` for per-user routing.

Total scope: ~38 commits across 7 stages. ETA: deliberate, multi-day. No broken intermediates — every commit compiles + passes its own tests + keeps `apiCheck` green (with explicit `apiDump` updates where the public API legitimately gains methods).

---

## Architectural decisions

### D1. Engine modification: one upstream line, not a fork

Change `StateMachine.kt:74`:
```kotlin
// upstream:
private fun setCurrentState(newState: StateType) { _state.tryEmit(newState) }
// fork:
protected open fun setCurrentState(newState: StateType) { _state.tryEmit(newState) }
```

Rationale: 2.26.14's parallel `StateMachineForAuth` class duplicated ~150 lines of engine logic. With a `protected open` hook, `AuthStateMachine` can override state-set in 20 lines and reuse the upstream engine entirely. **Far easier to maintain across upstream merges.** Single line of upstream divergence.

### D2. Multi-user routing in `AuthStateMachine`

`AuthStateMachine` becomes the multi-user-aware specialization. It:
- Owns `AuthStateRepo` (private field).
- Overrides `setCurrentState(newState)` to: (a) call super (normal emission), (b) write to `authStateRepo[activeUserId]`, (c) if `newState.isSessionEstablished`, emit default state again to allow next user's login.
- Adds `getStateForUser(userId): AuthState` (suspend) and `state(userId): Flow<AuthState>` (filtered).
- Adds `send(event: StateMachineEvent, userId: String?)` overload — userId routing via the active key when null.
- Adds `activeStateKey(): String?` exposing the LIFO peek.

The base `StateMachine.process(event)` flow is untouched. Resolvers continue to operate on a single `AuthState` per call. The multi-user logic is in HOW the state is persisted/exposed, not in HOW it's resolved.

### D3. `userId` on use case `execute(...)`, not constructor

For each use case that needs userId plumbing (~7 of them), add an `execute(userId: String?, ...)` overload. The `userId == null` default reads from `authStateRepo.activeStateKey()` so existing callers continue to work. The factory stays unchanged (fresh use case per call).

This is cleaner than the 2.26.14 design where userId leaked into the plugin constructor; it keeps each call self-describing.

### D4. `userId` on event payloads — minimal extension

Only events whose action layer needs userId routing get the field. Order:
- `SignOutData` — add `userId: String? = null`, `signOutAllUsers: Boolean = false`.
- `AuthenticationEvent.EventType.ClearFederationToIdentityPool` — add `userId: String? = null`.
- `AuthorizationEvent` — add `userId: String? = null` to `RefreshSession`, `FetchUnAuthSession`, `StartFederationToIdentityPool`.
- `DeleteUserEvent.EventType.DeleteUser` — add `userId: String? = null`.

Default-nullable means resolvers and actions that don't care can ignore it.

### D5. `AuthCredentialStore` interface gets userId

Change interface signatures:
```kotlin
fun saveCredential(credential: AmplifyCredential)                // unchanged — userId from credential.signedInData
fun retrieveCredential(userId: String? = null): AmplifyCredential   // ADD userId, default null = legacy key
fun deleteCredential(userId: String? = null)                        // ADD userId, default null = legacy key
```

`AWSCognitoAuthCredentialStore` implementation: prefix-and-fallback per the 2.26.14 contract — try `<userId>_amplify.session` first, fall back to `amplify.session` for pre-multi-user installs.

### D6. Public API extension matches 2.26.14

`AuthCategoryBehavior` (Java) gains the same five methods documented in `multi-user-contract/SKILL.md §A.1`. Defaults match 2.26.14 (no-userId variants delegate with `""` for all-users semantics; userId variants route via the use case).

### D7. Fix the dead-code `Amplify.configure` bug

`Amplify.java:179` currently calls `category.configure(categoryConfiguration, context)` (no userId) inside the `Amplify.configure(config, userId, ctx)` overload. Fix it to call the userId variant. **This is a bug present in both 2.26.14 and HEAD; we fix it as part of the port.**

---

## Stage breakdown — ordered work list

Each numbered item is one commit. After every commit:
```
./gradlew :aws-auth-cognito:test :core:test :core-kotlin:test ktlintCheck apiCheck
```
If `apiCheck` fails legitimately (because we added a public method), `apiDump` and review the diff before committing. Stop on first regression.

### Stage A — Foundation (new files, no upstream conflict, no behaviour change)

**A1. Add `ThreadSafeLifoMap.kt`** — copy verbatim from 2.26.14: `aws-auth-cognito/src/main/java/com/amplifyframework/statemachine/util/ThreadSafeLifoMap.kt`. New file. No tests touched.

**A2. Add `AuthStateRepo.kt`** — port from 2.26.14: `aws-auth-cognito/src/main/java/com/amplifyframework/statemachine/codegen/data/AuthStateRepo.kt`. Three rules: SignedOut→remove, SessionEstablished→persist+clear, intermediate→in-memory. Singleton instance via `getInstance(context)`. Imports `LifoMap` from A1. New file.

**A3. Add `isSessionEstablished` extension** — small helper used by `AuthStateRepo.put`. Co-locate next to `AuthState` extensions if upstream has a similar file, else add to `AuthStateRepo.kt`.

**A4. ~~Add `AWSCognitoLegacyCredentialStore.kt`~~ — RESOLVED: present at 2.36.0 (384 lines, `aws-auth-cognito/.../auth/cognito/data/AWSCognitoLegacyCredentialStore.kt`).** No action needed in Stage A; will reuse for fallback in Stage C2.

**Verification at end of Stage A:** new files compile in isolation, no other code touched. Tests pass unchanged.

### Stage B — Engine layer (single-line upstream change, multi-user routing)

**B1. `StateMachine.setCurrentState` → `protected open`** — `aws-auth-cognito/.../statemachine/StateMachine.kt:74`. The only upstream-line change in the engine. Justify in commit message: "Required hook for multi-user state routing in AuthStateMachine."

**B2. `AuthStateMachine` overrides `setCurrentState`** — wire `AuthStateRepo` integration. Add fields and override:
```kotlin
private val authStateRepo by lazy { AuthStateRepo.getInstance((environment as AuthEnvironment).context) }

override fun setCurrentState(newState: AuthState) {
    val activeUserId = authStateRepo.activeStateKey()
    if (activeUserId != null) authStateRepo.put(activeUserId, newState)
    super.setCurrentState(newState)
    if (newState.isSessionEstablished) {
        authStateRepo.put(activeUserId ?: extractUserId(newState), newState)
        super.setCurrentState(authStateRepo.getDefaultConfiguredState())
    }
}
```
Also add `getStateForUser(userId): AuthState` (suspend), `state(userId): Flow<AuthState>` (filtered), `send(event, userId)` overload, `activeStateKey()` exposed.

**B3. `EventDispatcher` interface gains overload** — `EventDispatcher.kt`. Add `fun send(event: StateMachineEvent, userId: String?)` with default delegating to `send(event)`. This avoids breaking implementers.

**B4. State machine unit tests** — extend `StateMachineTests.kt` and add a new `AuthStateMachineMultiUserTest.kt` exercising: per-user state persistence, default reset on SessionEstablished, second-user login while first is signed in, `getStateForUser(userId)` retrieval.

**Verification at end of Stage B:** state machine passes new multi-user tests; existing tests unchanged.

### Stage C — Credential store (per-user keys)

**C1. `AuthCredentialStore` interface signatures** — `aws-auth-cognito/.../statemachine/codegen/data/AuthCredentialStore.kt`. Add `userId: String? = null` to `retrieveCredential` and `deleteCredential`. Internal interface — no public API impact.

**C2. `AWSCognitoAuthCredentialStore` implementation** — `aws-auth-cognito/.../auth/cognito/data/AWSCognitoAuthCredentialStore.kt`. Implement prefix-and-fallback exactly per 2.26.14 contract §B.1. Extract userId from credential on save. Add SESSION_KEY_REGEX for clear-all.

**C3. `AWSCognitoLegacyCredentialStore` integration** — wire fallback path in `retrieveCredential(null)`.

**C4. Credential-store unit tests** — `AWSCognitoAuthCredentialStoreTest.kt`. Add: per-user save/retrieve, fallback to default key on userId miss, delete by userId, two-user concurrent storage.

**Verification at end of Stage C:** credential store tests green; `AWSCognitoAuthCredentialStoreInstrumentationTest` (connected) is not run in this phase.

### Stage D — Event payloads carrying userId

**D1. `SignOutData` extension** — `aws-auth-cognito/.../statemachine/codegen/data/SignOutData.kt`. Add `userId: String? = null` and `signOutAllUsers: Boolean = false`. Existing callers continue to work.

**D2. `AuthenticationEvent.EventType.ClearFederationToIdentityPool` userId** — `aws-auth-cognito/.../statemachine/codegen/events/AuthenticationEvent.kt`. Add `userId: String? = null` to the data class.

**D3. `AuthorizationEvent` userId fields** — same file or `AuthorizationEvent.kt`. Add `userId: String? = null` to: `RefreshSession`, `FetchUnAuthSession`, `StartFederationToIdentityPool`.

**D4. `DeleteUserEvent.EventType.DeleteUser` userId** — `DeleteUserEvent.kt`. Add `userId: String? = null` to the data class.

**Verification at end of Stage D:** all events compile, ktlint clean, no resolver behaviour change yet.

### Stage E — Use case layer (the userId-bearing overloads)

**E1. `SignOutUseCase.execute(userId, options)` overload** — accept optional userId, build `SignOutData(globalSignOut, browserPackage, bypassCancel, userId, signOutAllUsers)`, route `completeSignOut` to filter on `state(userId)` if userId provided. Existing `execute(options)` delegates to the new method with `userId = null`.

**E2. `SignOutUseCase.execute()` no-arg → ALL users by default, customizable via options** — per the user's "intentional but customizable" call. Default behaviour: iterate `authStateRepo.allKeys()` and sign out each. Customizable via the `AWSCognitoAuthSignOutOptions.signOutAllUsers` field (added in F0 below) — if `signOutAllUsers = false`, sign out only the active user. The bool default is `true` to match the 2.26.14 contract.

**E3. `FetchAuthSessionUseCase.execute(userId, options)` overload** — accept optional userId, route state collection through `state(userId)` if provided.

**E4. `ClearFederationToIdentityPoolUseCase.execute(userId)` overload** — pass userId in `AuthenticationEvent.EventType.ClearFederationToIdentityPool(userId)`.

**E5. `DeleteUserUseCase.execute(userId)` overload** — internal-only routing; public API at 2.36.0 doesn't expose userId for delete (per 2.26.14 contract §A.1: delete operates on currently authenticated user).

**E6. `WebUiSignInUseCase` userId + pending-userId persistence** — when sign-in succeeds, the resulting state must be persisted under the userId extracted from `SignedInData.userId`. The `setCurrentState` override (B2) handles state persistence; **additionally, persist a `pendingSignInUserId` in encrypted storage when the OAuth intent is launched** (per user-confirmed Q2). On process death, the redirect callback (E7) reads `pendingSignInUserId` to know which user's flow to complete. Stored under a dedicated key `<userId>_amplify.pending_signin` (or generic `amplify.pending_signin` if userId not yet known — multi-user sign-in via web is rare for the unauthenticated case).

**E7. `WebUiSignInResponseUseCase` userId** — on redirect arrival, read `pendingSignInUserId` from encrypted storage. Use it to route the resulting `SignedInData` to the correct credential bucket via `setCurrentState`. Clear the pending key on success or final failure. Add a unit test for the process-death path.

**E8. `FederateToIdentityPoolUseCase.execute(token, provider, userId, options)` overload** — accept userId for federated session storage key. Routes credential to `<userId>_federated_amplify.session` or default.

**E9. Use case unit tests** — extend each `*UseCaseTest.kt` with multi-user scenarios. Use MockK + `runTest` + Turbine + a `MutableSharedFlow<AuthState>` driving `stateMachine.state`.

**Verification at end of Stage E:** every modified use case has a green test exercising the new overload.

### Stage F — Plugin layer (public Java API surface)

**F0. `AWSCognitoAuthSignOutOptions.signOutAllUsers: Boolean = true` field + builder** — `aws-auth-cognito/.../options/AWSCognitoAuthSignOutOptions.kt`. Add the field with default `true` to preserve 2.26.14 default behaviour. Builder gets `signOutAllUsers(value: Boolean)` setter. Companion `defaults()` returns `signOutAllUsers = true`. This is the customizability surface — callers wanting single-user signout: `signOut(opts.builder().signOutAllUsers(false).build(), onComplete)`.

**F1. `AWSCognitoAuthPlugin.signOut(userId, onComplete)` overload** — new public method. Delegates: `enqueue(onComplete, ::throwIt) { useCaseFactory.signOut().execute(userId) }`.

**F2. `AWSCognitoAuthPlugin.signOut(userId, options, onComplete)` overload** — same shape with options.

**F3. `AWSCognitoAuthPlugin.signOut()` no-arg = ALL users** — already in upstream as `signOut(onComplete)` (line 433); ensure semantics shift to "all users" by routing to the new `SignOutUseCase.execute()` no-arg variant.

**F4. `AWSCognitoAuthPlugin.fetchAuthSession(userId, ...)` overloads** (×2 for options/no-options) — new public methods.

**F5. `AWSCognitoAuthPlugin.clearFederationToIdentityPool(userId, ...)` overload** — new method.

**F6. Public API surface review** — `./gradlew apiDump` and inspect the diff. Every new method should be expected; nothing else should change.

**Verification at end of Stage F:** plugin compiles, tests for plugin-level method routing pass, `apiCheck` baseline updated by `apiDump`.

### Stage G — Framework-level (`Plugin`, `Category`, `Amplify`)

**G1. `Plugin.java` default `configure(json, userId, ctx)`** — add default no-op method. Internal, default = no behaviour change for non-auth plugins.

**G2. `Category.java` userId routing** — implement `configure(config, userId, ctx)` that routes specifically to `awsCognitoAuthPlugin`. Per `multi-user-contract` §A.5 — exact pattern from 2.26.14.

**G3. `Amplify.java` `configure(config, userId, ctx)` overload** — add the overload AND fix the dead-code bug (call `category.configure(config, userId, ctx)`, not the no-userId variant). This is decision D7.

**G4. `AuthCategoryBehavior.java` interface methods** — five new methods per `multi-user-contract` §A.1. Documented `@NonNull String userId` parameters.

**G5. `AuthCategory.java` delegation** — forward to plugin per the 2.26.14 contract.

**G6. `AuthSignInResult.java` userId/username fields** — add nullable fields, accessors, and constructor that carries them. Backward-compatible (existing constructor stays).

**Verification at end of Stage G:** core compiles, framework tests pass, `:core:apiCheck` updated by `apiDump`.

### Stage H — Bindings

**H1. `KotlinAuthFacade.kt` suspend overloads** — `fetchAuthSession(userId)`, `fetchAuthSession(userId, options)`, `signOut(userId)`, `signOut(userId, options)`, `clearFederationToIdentityPool(userId)`. All wrap callbacks via `suspendCoroutine`.

**H2. `RxAuthBinding.java` userId-aware methods** — mirror Java surface in Rx style.

**H3. `RxAuthCategoryBehavior.java` interface** — declare new methods.

**H4. `SynchronousAuth.java` test util** — mirror Java surface for tests that use the sync helper.

**Verification at end of Stage H:** bindings compile, their tests pass.

### Stage I — Verify ship-readiness

**I1. Full build** — `./gradlew clean build`. No errors, no warnings beyond upstream baseline.

**I2. Full test suite** — `./gradlew test`. All modules.

**I3. Public-API gate** — `./gradlew apiCheck`. The accumulated `apiDump` from Stage F + G should match.

**I4. Smoke check four canonical flows** (manual, with notes):
- (a) Sign-in user A → fetch session → sign-out user A
- (b) Sign-in user A, sign-in user B (without signing out A) → fetch session A and B independently
- (c) Federate to identity pool → clear federation
- (d) Sign-out all users (zero-arg `signOut()`) — verify both A and B credential keys removed

**I5. Lint clean** — `./gradlew ktlintCheck checkstyle`.

**I6. Update `multi-user-contract/SKILL.md`** — note that the port shifted to `setCurrentState` override (§ engine modification), and update file:line citations to 2.36.0 paths.

---

## Per-file change list (reference table)

| Stage | File | Action | Source |
|---|---|---|---|
| A1 | `aws-auth-cognito/.../statemachine/util/ThreadSafeLifoMap.kt` | NEW | `git show 2.26.14:<path>` |
| A2 | `aws-auth-cognito/.../statemachine/codegen/data/AuthStateRepo.kt` | NEW | `git show 2.26.14:<path>` |
| A4 | `aws-auth-cognito/.../auth/cognito/data/AWSCognitoLegacyCredentialStore.kt` | VERIFY existence at 2.36.0 | upstream may already have |
| B1 | `aws-auth-cognito/.../statemachine/StateMachine.kt:74` | MODIFY (private→protected open) | one-line |
| B2 | `aws-auth-cognito/.../auth/cognito/AuthStateMachine.kt` | EXTEND (override, new methods) | new logic |
| B3 | `aws-auth-cognito/.../statemachine/EventDispatcher.kt` | EXTEND (overload) | one method |
| C1 | `aws-auth-cognito/.../statemachine/codegen/data/AuthCredentialStore.kt` | EXTEND interface signatures | per `multi-user-contract` §B.5 |
| C2 | `aws-auth-cognito/.../auth/cognito/data/AWSCognitoAuthCredentialStore.kt` | REWRITE save/retrieve/delete | per `multi-user-contract` §B.1 |
| D1 | `aws-auth-cognito/.../statemachine/codegen/data/SignOutData.kt` | EXTEND (userId, signOutAllUsers) | additive |
| D2 | `aws-auth-cognito/.../statemachine/codegen/events/AuthenticationEvent.kt` | EXTEND (ClearFederationToIdentityPool userId) | additive |
| D3 | `aws-auth-cognito/.../statemachine/codegen/events/AuthorizationEvent.kt` | EXTEND (Refresh/FetchUnAuth/StartFederation userId) | additive |
| D4 | `aws-auth-cognito/.../statemachine/codegen/events/DeleteUserEvent.kt` | EXTEND (DeleteUser userId) | additive |
| E1-E2 | `aws-auth-cognito/.../auth/cognito/usecases/SignOutUseCase.kt` | ADD execute(userId), execute() all-users | additive |
| E3 | `aws-auth-cognito/.../auth/cognito/usecases/FetchAuthSessionUseCase.kt` | ADD execute(userId, options) | additive |
| E4 | `aws-auth-cognito/.../auth/cognito/usecases/ClearFederationToIdentityPoolUseCase.kt` | ADD execute(userId) | additive |
| E5 | `aws-auth-cognito/.../auth/cognito/usecases/DeleteUserUseCase.kt` | ADD execute(userId) | additive |
| E6 | `aws-auth-cognito/.../auth/cognito/usecases/WebUiSignInUseCase.kt` | VERIFY userId routes through setCurrentState | confirm only |
| E7 | `aws-auth-cognito/.../auth/cognito/usecases/WebUiSignInResponseUseCase.kt` | VERIFY same | confirm only |
| E8 | `aws-auth-cognito/.../auth/cognito/usecases/FederateToIdentityPoolUseCase.kt` | ADD execute(token, provider, userId, options) | additive |
| F1-F5 | `aws-auth-cognito/.../auth/cognito/AWSCognitoAuthPlugin.kt` | ADD userId overloads | additive (new public methods) |
| G1 | `core/src/main/java/com/amplifyframework/core/plugin/Plugin.java` | ADD default configure(json, userId, ctx) | additive |
| G2 | `core/src/main/java/com/amplifyframework/core/category/Category.java` | ADD configure(config, userId, ctx) | additive + routing |
| G3 | `core/src/main/java/com/amplifyframework/core/Amplify.java` | ADD configure(config, userId, ctx) AND fix dead-code | additive + bugfix |
| G4 | `core/src/main/java/com/amplifyframework/auth/AuthCategoryBehavior.java` | ADD 5 methods | additive |
| G5 | `core/src/main/java/com/amplifyframework/auth/AuthCategory.java` | ADD 5 delegating methods | additive |
| G6 | `core/src/main/java/com/amplifyframework/auth/result/AuthSignInResult.java` | ADD userId/username fields, ctor, accessors | additive |
| H1 | `core-kotlin/.../kotlin/auth/KotlinAuthFacade.kt` | ADD 5 suspend overloads | additive |
| H2 | `rxbindings/src/main/java/com/amplifyframework/rx/RxAuthBinding.java` | ADD methods | additive |
| H3 | `rxbindings/src/main/java/com/amplifyframework/rx/RxAuthCategoryBehavior.java` | ADD declarations | additive |
| H4 | `testutils/src/main/java/com/amplifyframework/testutils/sync/SynchronousAuth.java` | ADD methods | additive |

**~30 production files modified, ~8 new files added, ~10 test files extended.**

---

## Risk register

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Upstream changed `state` flow conflation semantics in a way that breaks `setAuthState` reset-to-default. | Low (already verified unchanged) | High (would break second-user login) | B2 commit includes a unit test for second-user login while first signed-in. |
| R2 | `AuthStateMachine` constructor wiring (the resolver tree) hides state-write paths the override misses. | Medium | High | B2 commit reads every place `setCurrentState` could fire — actions, resolvers, error paths. Audit before committing. |
| R3 | `apiCheck` rejects new public methods even though they're additive. | Medium | Low | Run `apiDump` after Stage F and Stage G; commit the dumps as part of those commits. |
| R4 | `AuthCredentialStore` interface change breaks third-party plugin implementations (if any). | Low (interface is internal) | Low | Default-null params keep existing call sites working. |
| R5 | Per-user `state(userId): Flow<AuthState>` filter logic introduces subtle event ordering bug. | Medium | Medium | E1+E3 commits pin behaviour with Turbine tests asserting per-user emission ordering. |
| R6 | `AuthStateRepo.getInstance(context)` singleton races with `AuthStateMachine` init. | Low | Medium | A2 commit makes the singleton thread-safe (lazy + double-checked) and uses `applicationContext`. |
| R7 | `WebUiSignInUseCase` redirect handling assumes single-user state — process-death recovery breaks for multi-user. | Medium | High | E6/E7 verification step explicitly covers process-death scenario. May warrant a follow-up commit if the redirect path needs userId persistence. |
| R8 | `signOut()` zero-arg = all-users semantic surprises legacy callers expecting single-user behaviour. | High (intentional) | Low | Document the semantic change in `AWSCognitoAuthPlugin` JavaDoc; cover in changelog. |
| R9 | Refresh-token rotation (upstream 2.30 feature) writes new refresh tokens — multi-user store must persist correctly per user. | Medium | High | C2 implementation must extract userId from `signedInData` on save; covered by C4 unit tests. |
| R10 | `Amplify.configure(config, userId, ctx)` fix changes behaviour for any caller that was relying on the bug being a no-op. | Very low | Low | Document in commit message; the userId param was always *intended* to flow per `multi-user-contract` §A.4. |

---

## Verification plan (exit criteria for each stage)

**Stage A:** new files compile in isolation. No other code touched. `./gradlew :aws-auth-cognito:compileDebugKotlin` green.

**Stage B:** new state-machine multi-user tests pass; upstream `StateMachineTests.kt` unchanged and still green.

**Stage C:** `AWSCognitoAuthCredentialStoreTest.kt` covers per-user save/retrieve, fallback, two-user storage. `apiCheck` clean (interface change is internal).

**Stage D:** all modules compile; ktlint clean.

**Stage E:** each modified use case test covers (i) the no-userId path (still works), (ii) the userId path (correct routing), (iii) the all-users path where applicable.

**Stage F:** `./gradlew :aws-auth-cognito:apiDump` produces a diff with exactly the expected new methods. `RealAWSCognitoAuthPluginTest.kt` (if it still exists at 2.36.0 — verify) and `AWSCognitoAuthPluginTest.kt` extended.

**Stage G:** `./gradlew :core:apiDump` produces a diff with exactly the expected new methods. The dead-code Amplify.configure fix is verified by a unit test.

**Stage H:** binding tests exist and pass.

**Stage I:** `./gradlew clean build test ktlintCheck checkstyle apiCheck` all green. Manual smoke (4 flows) passes.

---

## Open questions — RESOLVED

1. ✅ **`AWSCognitoLegacyCredentialStore` at 2.36.0** — present (384 lines). Reuse for fallback in C2. No new file needed in Stage A.

2. ✅ **Process-death recovery for multi-user hosted UI** — confirmed by user. Plan E6/E7 updated to persist `pendingSignInUserId` in encrypted storage when launching OAuth intent; read it back on redirect arrival to route the result.

3. ✅ **`SignedInData.userId` always populated at 2.36.0** — confirmed by user. Multi-user routing may safely key on `signedInData.userId`.

4. ✅ **`signOut()` zero-arg = ALL users** — confirmed intentional, with the refinement that it must be **customizable**. Plan F0 adds `AWSCognitoAuthSignOutOptions.signOutAllUsers: Boolean = true` with builder support; setting it to `false` reverts to single-active-user behaviour. Default `true` preserves the 2.26.14 contract.

---

## Skill cross-references

- `.claude/skills/multi-user-contract/SKILL.md` — the 12 invariants this plan implements.
- `.claude/skills/amplify-auth-usecase/SKILL.md` — pattern each use-case overload follows.
- `.claude/skills/amplify-state-machine/SKILL.md` — per-user state-machine extension principles.
- `.claude/skills/amplify-merge-upstream/SKILL.md` — playbook for the next upstream merge after this lands.
- `.claude/rules.md` — the project-wide coding rules every commit must respect.

---

## Out of scope for this plan

- The 16KB page alignment work (SQLCipher / MapLibre) from `2.26.14` commits `ec34732c`–`891002ab`. Unrelated to multi-user; carry separately on a different branch, then merge.
- Any new feature work. This is a port, not an enhancement.
- Removing deprecated Pinpoint plugins. Upstream's deprecation is in place; we leave it.
