# amplify-android — Harri fork

Fork of [aws-amplify/amplify-android](https://github.com/aws-amplify/amplify-android) (`upstream` remote) maintained by HarriLLC. Tracks upstream releases; current target is **2.34.0+** with Harri's multi-user auth additions on `feature/upgrade-to-2.34.0-with-multi-user`.

The fork's primary divergence is **multi-user auth**: a `userId` parameter is threaded through `SignOutUseCase`, `FetchAuthSessionUseCase`, `ClearFederationToIdentityPoolUseCase`, etc., and a `ThreadSafeLifoMap` manages per-user credential stores. When touching auth code, assume multi-user is in scope unless told otherwise.

## Module map (what lives where)

```
core/                       Java category interfaces, Plugin<E> contract, AmplifyException, Hub
core-kotlin/                suspend-fun facades over Java callback APIs
foundation/  (KMP)          Result<T,E>, Logger, AwsCredentialsProvider, new AmplifyException
foundation-bridge/          Bridges foundation types to v2 Java types
annotations/                @InternalAmplifyApi, @InternalApiWarning, @AmplifyFlutterApi
aws-auth-cognito/           Cognito auth — state machine + use cases (largest module)
aws-auth-plugins-core/      Shared auth utilities (AuthHubEventEmitter, etc.)
aws-api/, aws-datastore/    Category plugins
aws-storage-s3/, aws-geo-location/
aws-analytics-pinpoint/     DEPRECATED (Pinpoint EOL 2026-10-30)
aws-push-notifications-pinpoint/  DEPRECATED
aws-kinesis/                Standalone client (new pattern, NOT a category plugin)
apollo/, appsync/           AppSync GraphQL extensions / AppSync Events client
build-logic/plugins/        Convention plugins (amplify.android.library, amplify.kotlin, …)
gradle/libs.versions.toml   Version catalog — single source of truth for deps
testutils/, testmodels/     Shared test infra (DeviceFarmTestBase, ResultAssertions, MockedModels)
rxbindings/                 RxJava bindings (legacy)
```

Compile SDK 36, min SDK 24, JVM 17, Kotlin 2.2.0, Coroutines 1.10.2.

## Architecture in 60 seconds

1. **Categories + Plugins** — Each capability (Auth, Storage, API…) is a `Category<P>` in `core/` exposing a stable Java API. Concrete behaviour lives in a `Plugin<E>` (e.g. `AWSCognitoAuthPlugin`). Lifecycle: `configure(json|AmplifyOutputs, ctx)` → `initialize(ctx)`. Underlying SDK exposed via `getEscapeHatch()`.
2. **State machine (auth)** — `aws-auth-cognito` models auth as `StateMachine<State, Environment>` with a `StateMachineResolver` that maps `(state, event) → (new state, [actions])`. State emission goes through `MutableSharedFlow` (no conflation); transitions serialised on a single-thread context (`newSingleThreadContext("StateMachineContext")`).
3. **Use cases** — Every public auth call (`signIn`, `signOut`, `fetchAuthSession`, `deleteUser`, `confirmSignIn`, …) is an `internal class *UseCase` that: validates preconditions by collecting state, dispatches an event, awaits the terminal state via `state.mapNotNull { … }.first()`, emits a Hub event on success, returns a typed result. **This is the dominant pattern in 2.26→2.36 — `RealAWSCognitoAuthPlugin` is being progressively gutted into use cases.** New auth work goes in a use case.
4. **Foundation (new code path)** — KMP module. New clients (Kinesis, Firehose, AppSync Events) depend on `foundation` instead of `core`'s callback APIs and return `Result<T, E>` rather than throwing.
5. **Hub events** — Cross-category pub/sub (`Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(AuthChannelEventName.SIGNED_IN))`). Emit only on success, always with an enum, never a string literal.
6. **core-kotlin facades** — Wrap Java `Consumer<T>` / `Action` callbacks in `suspendCoroutine { }`. New public APIs should be `suspend fun` on the Kotlin facade.

## Coding rules (full list in `.claude/rules.md`)

- **Kotlin first.** New code is Kotlin. Existing Java code stays Java; don't rewrite for style.
- **Coroutines only.** No `Executors`, `Future`, `RxJava`, `CountDownLatch`, `runBlocking`, `GlobalScope`. Every scope has a defined lifetime.
- **`Result<T, E>` over exceptions** in new code. Exceptions are reserved for contract violations / logic bugs.
- **All exceptions extend `AmplifyException`** with `message`, `recoverySuggestion`, optional `cause`. Use a `*ExceptionConverter` to translate AWS SDK exceptions.
- **Builders are dual-language.** Options classes expose Kotlin DSL (`Options { foo = "x" }`) AND Java builder (`Options.builder().foo("x").build()`) via `@JvmStatic` / `@JvmSynthetic`. See `.claude/rules.md`.
- **Dependencies live in `gradle/libs.versions.toml`.** Never inline a version. Never add a new repository. New modules use convention plugins (`amplify.android.library`, `amplify.publishing`, `amplify.kmp`).
- **Tests in Kotlin, backtick names** (`` `should ... when ...` ``), Kotest assertions (not JUnit `assertEquals`), MockK (not Mockito), Turbine for Flow, `runTest` for coroutines. Connected tests extend `DeviceFarmTestBase`.
- **Internal API surface** marked `@InternalAmplifyApi` (error opt-in) or `@InternalApiWarning` (warning opt-in). Experimental public APIs use `@ExperimentalAmplifyApi`. Public surface is enforced by binary-compatibility-validator (`./gradlew apiCheck`).
- **Logging:** `Amplify.Logging.logger(category, tag)` for category loggers, lazy supplier form `logger.debug { "..." }` — never concatenate at the call site.
- **Multi-user (fork-specific):** when adding/modifying a use case, plumb `userId: String?` through the constructor and credential-store calls. See existing `SignOutUseCase`, `FetchAuthSessionUseCase`. Use `ThreadSafeLifoMap` for per-user state.

## Common commands

```bash
./gradlew build                                  # full build (slow)
./gradlew :aws-auth-cognito:assembleDebug        # build one module
./gradlew :aws-auth-cognito:test                 # unit tests for one module
./gradlew ktlintCheck checkstyle apiCheck        # lint + style + public-api gate
./gradlew ktlintFormat apiDump                   # auto-fix and refresh API surface
./gradlew :foundation:koverHtmlReport            # coverage HTML for one module

git fetch upstream --tags                        # see new upstream releases
git log release_v2.34.0..release_v2.36.0 --oneline  # what's new upstream
```

`scripts/build_local.sh` is a local-only convenience (untracked).

## Pointers

- **Skills** (task-triggered, in `.claude/skills/`):
  - `amplify-auth-usecase` — adding or modifying a Cognito auth use case
  - `amplify-state-machine` — adding states, events, actions, or resolvers
  - `amplify-new-module` — scaffolding a new Gradle module
  - `amplify-merge-upstream` — syncing upstream Amplify into the fork
- **Full rules:** `.claude/rules.md` (read this before non-trivial changes)
- **Upstream guidance:** `git show release_v2.36.0:AGENTS.md` (canonical conventions doc)
- **SDK migration guide:** `documents/MobileSDK_To_AmplifyAndroid.md`

## When in doubt

If a public Auth API in `RealAWSCognitoAuthPlugin` does anything more than delegate to a use case, that's a refactor opportunity, not a bug. The upstream direction is clear: use cases own behaviour, the plugin is a thin façade. Match that direction unless asked otherwise.
