# Multi-User Auth — Hand-off

Drop-in for the next person picking up `feature/multi-user-on-2.36.0`. Sized for a 30-minute read, then start working.

---

## TL;DR

**Status:** complete. Tag **`2.36.0-harri`** at `dc9d4973`, 33 commits since `release_v2.36.0`. Branch `feature/multi-user-on-2.36.0`, **not pushed to origin yet**. All five touched modules (`:core`, `:core-kotlin`, `:aws-auth-cognito`, `:rxbindings`, `:testutils`) green for `test` + `ktlintCheck` + `checkstyle` + `apiCheck`. Tag annotated with the full release note; see `git show 2.36.0-harri`.

**One outstanding pre-ship task:** I4 — four canonical multi-user flows on a real device. Listed in §3.2 below.

**Where the canonical contract lives:** `.claude/skills/multi-user-contract/SKILL.md`.

**Where the architectural reference lives:** `documents/MultiUserAuth_Architecture.md`.

---

## 1. What shipped

### 1.1 Public API (consumer-visible)

- **`Amplify.Auth.fetchAuthSession(userId, …)`** ×2 (with / without `AuthFetchSessionOptions`) on `AuthCategoryBehavior`.
- **`Amplify.Auth.signOut(userId, …)`** ×2 on `AuthCategoryBehavior`.
- **`Amplify.Auth.signOut(onComplete)`** zero-arg now means "sign out all tracked users" — customizable via `AWSCognitoAuthSignOutOptions.signOutAllUsers` (default `true`).
- **`AuthSignInResult.getUserId()` / `.getUsername()`** — new nullable accessors populated from `SignedInData` on success.
- **`Amplify.configure(config, userId, ctx)`** — framework-level userId entry point. Routes through `Category.configure → Plugin.configure`.
- **`AWSCognitoAuthPlugin.federateToIdentityPool(token, provider, userId, …)`** ×2 and **`clearFederationToIdentityPool(userId, …)`** — Cognito-only multi-user federate methods.
- **Kotlin facade** (`KotlinAuthFacade`): `suspend fun fetchAuthSession(userId, options=defaults)` and `suspend fun signOut(userId, options=defaults)`.
- **Rx bindings** (`RxAuthBinding`): `fetchAuthSession(userId, …)` ×2 and `signOut(userId, …)` ×2.
- **Test util** (`SynchronousAuth`): same userId methods as sync helpers.

### 1.2 Internal architecture

- `AuthStateRepo` — per-user state with `ThreadSafeLifoMap` (in-memory) + `EncryptedKeyValueRepository` (persisted) + a reserved persisted-user-index key. Falls back to `InMemoryFallbackStore` when the keystore can't initialize.
- `StateMachineForAuth` — open base class extended by `AuthStateMachine`. Adds `send(event, userId)`, `getStateForUser`, `allUserIds`. `setAuthState` recovers userId from `SignedInData` on `SessionEstablished` and gates the reset-to-default emission on the caller passing an explicit userId (preserves single-user upstream semantics).
- `AWSCognitoAuthCredentialStore` — dual-writes to default key + userId-prefixed key on save; `retrieveCredential(userId)` falls back to the default key when the prefixed key is empty.
- 32 use cases under `aws-auth-cognito/.../usecases/` — five (SignOut, FetchAuthSession, ClearFederationToIdentityPool, DeleteUser, FederateToIdentityPool) gained `execute(userId, …)` overloads.
- 5 `SignOutEvent.EventType` variants carry explicit `userId` fields, threaded through `AuthenticationCognitoActions` and `SignOutCognitoActions`.

### 1.3 Tests added

- `AuthStateRepoTest` — 8 cases covering the persistence rule, allUserIds, persisted-index round-trip simulating process death, clearInMemory.
- `AWSCognitoAuthCredentialStoreTest` — 5 new cases for per-user keys, fallback, delete, two-user no-collision.
- `SignOutUseCaseTest` — 5 new multi-user cases on top of the existing 9.
- `FetchAuthSessionUseCaseTest`, `ClearFederationToIdentityPoolUseCaseTest`, `DeleteUserUseCaseTest`, `FederateToIdentityPoolUseCaseTest` — one assertion-rich multi-user test each.

### 1.4 Documentation

- `.claude/skills/multi-user-contract/SKILL.md` — refreshed with the 2.36.0 status section, path-mapped quick reference, and decision log.
- `.claude/plans/multi-user-port-plan-2.36.0.md` — the per-stage plan that drove the work; now historical reference.
- `documents/MultiUserAuth_Architecture.md` — long-form architecture (this doc's sibling).
- `CLAUDE.md` — refreshed with current branch / target version / migration-complete wording.
- `.claude/rules.md` — rule 19 picked up the `runCatching` ban and the `InMemoryFallbackStore` pattern.

---

## 2. Verify before pushing

### 2.1 Reproduce the green state

```sh
./gradlew :core:test :core:ktlintCheck :core:checkstyle :core:apiCheck \
          :core-kotlin:test :core-kotlin:ktlintCheck :core-kotlin:apiCheck \
          :aws-auth-cognito:test :aws-auth-cognito:ktlintCheck :aws-auth-cognito:checkstyle :aws-auth-cognito:apiCheck \
          :rxbindings:test :rxbindings:ktlintCheck :rxbindings:checkstyle :rxbindings:apiCheck
```

Expected: `BUILD SUCCESSFUL` in ~1–2 minutes (incremental) or ~5 minutes (clean).

```sh
./gradlew compileDebugKotlin --continue
./gradlew apiCheck --continue
```

Expected: every module compiles and `apiCheck` passes (the `.api` files in each module's `api/` directory match what `apiBuild` produces).

### 2.2 Inspect the tag

```sh
git show 2.36.0-harri
git log --oneline release_v2.36.0..2.36.0-harri
```

Expected: 33 commits, an annotated tag with the full release note in its message body.

### 2.3 Push to origin (when ready)

```sh
git push origin feature/multi-user-on-2.36.0
git push origin 2.36.0-harri
```

The tag is currently local-only. Pushing it makes it visible to anyone with read access to the repo.

---

## 3. Outstanding work

### 3.1 Required before declaring ship-ready

**I4 — manual smoke test of the four canonical multi-user flows on a device.** Cannot be automated — needs a device with the consuming app integrated. Steps:

1. **Sign-in user A → fetch session → sign-out user A.** Assert: SignedInData persisted under `userA_amplify.<pool>.session` key; after signOut, that key is gone, `signedIn` is `false`.
2. **Sign-in user A, sign-in user B (without signing out A) → fetch session for A, fetch session for B.** Assert: `getCurrentState()` returns the default state immediately after B's `SessionEstablished` (the multi-user reset fired); `fetchAuthSession("userA")` returns A's session, `fetchAuthSession("userB")` returns B's session.
3. **Federate to identity pool → clear federation.** Assert: federated credential persisted; clearFederation transitions cleanly.
4. **Sign-out all users (zero-arg `Amplify.Auth.signOut(onComplete)`)**. Assert: both A and B credential keys removed; `allUserIds()` returns empty after.

### 3.2 Deferred — explicitly out of scope for this port

- **Removing deprecated Pinpoint plugins** — upstream's deprecation stays. Don't touch.
- **The 16KB page alignment work** from `2.26.14` (`ec34732c`–`891002ab`). Carry separately on a different branch if/when needed.
- **Per-user `state(userId): Flow<AuthState>` filtered observation API** — the multi-user contract §C.1 lists `listen(userId, …)` but the practical effect (per-user routing via dispatcher) holds in 2.36.0 without exposing a Flow filter. Add only if a real consumer needs it.

### 3.3 Possible enhancements

- **Eager loading of a configured userId at boot.** `AWSCognitoAuthPlugin.configure(json, userId, ctx)` currently delegates to the no-userId configure (userId is informational). A future iteration could load the supplied userId's persisted state eagerly so `getCurrentState()` returns that user's state immediately after `Amplify.configure(config, userId, ctx)` returns. Would require threading userId into `AuthEnvironment` and the configure flow's resolver path.
- **Robolectric coverage of `AuthStateRepo`'s encrypted-store path.** Current tests use `createForTest` with an in-memory fake. A Robolectric-based `AndroidTest` would exercise `EncryptedKeyValueRepository` end-to-end.

---

## 4. Common operations — recipes

### 4.1 Add a new userId-aware use case method

Pattern for a hypothetical `BarUseCase.execute(userId, options)`:

```kotlin
internal class BarUseCase(
    private val stateMachine: AuthStateMachine,
    private val emitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(options: BarOptions = BarOptions.defaults()): BarResult =
        execute(userId = null, options = options)

    suspend fun execute(userId: String?, options: BarOptions = BarOptions.defaults()): BarResult {
        val authState = if (userId.isNullOrEmpty()) {
            stateMachine.getCurrentState()
        } else {
            stateMachine.getStateForUser(userId)
        }
        // … validate state …
        val event = BarEvent(BarEvent.EventType.BarRequested(userId = userId, …))
        return stateMachine.state
            .onSubscription {
                if (userId.isNullOrEmpty()) stateMachine.send(event)
                else stateMachine.send(event, userId)
            }
            .drop(1)
            .mapNotNull { /* … */ }
            .first()
    }
}
```

Then on `AWSCognitoAuthPlugin`:

```kotlin
override fun bar(onComplete: …) = enqueue(onComplete, ::throwIt) { useCaseFactory.bar().execute() }
override fun bar(userId: String, onComplete: …) = enqueue(onComplete, ::throwIt) {
    useCaseFactory.bar().execute(userId)
}
```

Then on `AuthCategoryBehavior` (for Java surface), `KotlinAuthFacade` (Kotlin), `RxAuthBinding` (Rx), `SynchronousAuth` (sync). Run `./gradlew :<module>:apiDump` after.

### 4.2 Merge a new upstream release

The next anticipated work. Walk the playbook:

1. `git fetch upstream --tags` → see new tag (e.g. `release_v2.37.0`).
2. Read the `amplify-merge-upstream` skill (`.claude/skills/amplify-merge-upstream/SKILL.md`).
3. `git checkout -b feature/multi-user-on-2.37.0 release_v2.37.0`.
4. Cherry-pick or merge the 33 fork commits between `release_v2.36.0` and `2.36.0-harri`.
5. Resolve conflicts using **the contract** (`multi-user-contract/SKILL.md` §F) — every invariant must hold post-merge.
6. Rerun the full verification matrix from §2.1.
7. Tag `2.37.0-harri` once green.

Specific risks during merge:

- **`StateMachineForAuth` engine internals.** Upstream may reshape `StateMachine.kt`. Reapply the open-base-class structure on top.
- **Use case extraction.** Upstream is still moving behaviour from the plugin into use cases. The userId overloads must be reattached to whichever use case absorbed the old code.
- **`AuthCategoryBehavior` shape.** Any new upstream method gets default `UnsupportedOperationException` impls in `AuthPlugin`.
- **`SignOutEvent` variant evolution.** Each variant added by upstream needs a userId field if it's credential-affecting.

### 4.3 Investigate a multi-user bug

Order to check, fastest payoff first:

1. `git log --oneline release_v2.36.0..HEAD -- <file>` — is the bug in fork code or upstream code?
2. Run the use case's unit test suite with `--info` to see the exact event order.
3. Check `AuthStateRepo`'s state at the time of the bug: `allInMemoryKeys()` (intermediate users) and `allUserIds()` (intermediate + persisted). If both empty during a flow that should have a tracked user, look at `setAuthState` — the userId recovery from `SignedInData` may not be firing.
4. For sign-out chain bugs, audit the `SignOutEvent.EventType` variants — does the userId thread through `AuthenticationCognitoActions.initiateSignOutAction` → `SignOutCognitoActions.<x>Action` → the next event dispatched via `dispatcher.send(evt)`?
5. For credential-store bugs, run `AWSCognitoAuthCredentialStoreTest` — the per-user-key + fallback paths are well covered.

---

## 5. Pitfalls and gotchas

### 5.1 `runCatching` is forbidden in production code

It catches `Throwable` (rule 19 violation). Use `try { … } catch (e: Exception) { … }` matching the existing pattern in `AWSCognitoAuthCredentialStore.deserializeCredential`. When the catch needs to handle infrastructure failures (Android keystore unavailable on JVM unit tests), add an explicit `catch (e: LinkageError) { … }` clause and provide a graceful fallback (see `AuthStateRepo.InMemoryFallbackStore`).

### 5.2 `setAuthState` reset emission is multi-user-only

The emission of `getDefaultConfiguredState()` after `SessionEstablished` only fires when the caller passed a non-empty `userId` arg to the dispatch. Single-user observers (callers that don't pass userId) keep `_state` at `SessionEstablished` so `getCurrentState()` returns `SignedIn`. **Do not unconditionally fire the reset** — that regresses every single-user upstream test (`AuthValidationTest` and friends).

### 5.3 `signOut()` no-arg is ALL users by default

Existing single-user upstream callers expecting `signOut()` to sign out the current user only get the same effect because `allUserIds()` returns one user, but in multi-user apps the same call wipes everyone. Document this in your consuming app's release notes.

### 5.4 `AuthHubEventEmitter.sendHubEvent(eventName: String)` takes a String

`sendHubEvent(SomeEnum.toString())` is correct API usage, not a "hub-event-as-string-literal" violation. Don't refactor those calls just because rule 12 says "use enums".

### 5.5 The `userId` field on a `SignOutEvent.EventType` variant is non-redundant

The variant also carries `SignedInData` which contains a userId. The explicit field exists because `SignedInData` is nullable on `SignOutLocally` and because invariant 5 requires actions to read userId from the event payload, not from environment.

### 5.6 Test environment caveats

JVM unit tests cannot construct `EncryptedKeyValueRepository` — `androidx.security.crypto.MasterKeys` throws `LinkageError`. `AuthStateRepo`'s lazy delegate catches this and substitutes `InMemoryFallbackStore`. Tests that exercise `AuthStateRepo` directly should use `AuthStateRepo.createForTest(InMemoryKeyValueRepository())` to avoid touching the lazy at all.

`AuthStateRepo` is a process-wide singleton via `getInstance(context)`. Across-test contamination is a real risk; use `resetInstanceForTest()` in `@Before` if a test creates and inspects state.

### 5.7 The `apiCheck` gate

Any change to a public method signature in `:core`, `:core-kotlin`, `:aws-auth-cognito`, `:rxbindings`, or other api-validated modules will fail `apiCheck`. Run `./gradlew :<module>:apiDump` to refresh the `.api` file, inspect the diff carefully, then commit the dump alongside the source change.

If `apiCheck` and `apiDump` run in the same Gradle invocation, you'll get a "task uses output of task without declaring an explicit dependency" warning — run them in two separate invocations.

---

## 6. Where to find things

| Need | Path |
|---|---|
| The contract (12 invariants) | `.claude/skills/multi-user-contract/SKILL.md` |
| Architecture reference | `documents/MultiUserAuth_Architecture.md` (this doc's sibling) |
| Per-stage port plan | `.claude/plans/multi-user-port-plan-2.36.0.md` |
| Coding rules | `.claude/rules.md` |
| Project guide | `CLAUDE.md` |
| Skills (task-triggered) | `.claude/skills/` |
| Use cases | `aws-auth-cognito/src/main/java/com/amplifyframework/auth/cognito/usecases/` |
| State machine extension | `aws-auth-cognito/src/main/java/com/amplifyframework/statemachine/StateMachineForAuth.kt` |
| Per-user state | `aws-auth-cognito/src/main/java/com/amplifyframework/statemachine/codegen/data/AuthStateRepo.kt` |
| Credential store | `aws-auth-cognito/src/main/java/com/amplifyframework/auth/cognito/data/AWSCognitoAuthCredentialStore.kt` |
| Plugin façade | `aws-auth-cognito/src/main/java/com/amplifyframework/auth/cognito/AWSCognitoAuthPlugin.kt` |
| Public Java API | `core/src/main/java/com/amplifyframework/auth/AuthCategoryBehavior.java` |
| Kotlin facade | `core-kotlin/src/main/java/com/amplifyframework/kotlin/auth/KotlinAuthFacade.kt` |
| Rx | `rxbindings/src/main/java/com/amplifyframework/rx/RxAuthBinding.java` |
| Sync test util | `testutils/src/main/java/com/amplifyframework/testutils/sync/SynchronousAuth.java` |
| Result type | `core/src/main/java/com/amplifyframework/auth/result/AuthSignInResult.java` |
| Multi-user options | `aws-auth-cognito/src/main/java/com/amplifyframework/auth/cognito/options/AWSCognitoAuthSignOutOptions.java` |
| Tag | `2.36.0-harri` (`git show 2.36.0-harri`) |

---

## 7. Roadmap for the next iteration

1. **I4 manual smoke** on a device. Block on this before pushing the tag to origin.
2. **`git push origin feature/multi-user-on-2.36.0` + `git push origin 2.36.0-harri`.** The tag is local-only right now.
3. **Open a PR** if the team's flow needs a review before merging into the release branch.
4. **Integrate into the consuming app** (Harri Android). Update its dependency on the fork to `2.36.0-harri`, run the integration test suite, ship.
5. **Track upstream** (`git fetch upstream --tags`). When upstream cuts `release_v2.37.0+`, run the merge per §4.2.

When the next merge starts, reread the SKILL doc's "Status on `feature/multi-user-on-2.36.0`" section first — it captures the port-specific decisions a literal contract walk would miss.
