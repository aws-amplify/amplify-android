# amplify-android coding rules

Authoritative coding rules for this fork. Distilled from upstream `AGENTS.md` (release_v2.36.0) plus fork-specific multi-user conventions. CLAUDE.md links here; skills assume these rules hold.

When upstream changes a rule, upstream wins — re-pull `AGENTS.md` from the latest release tag and reconcile.

---

## 1. File & module naming

- Source files: PascalCase, file name = primary class. `AWSCognitoAuthPlugin.kt`, `AmplifyException.java`.
- Module directories: kebab-case. `aws-auth-cognito`, `aws-api-appsync`, `core-kotlin`.
- Packages: `com.amplifyframework.<category>` (`com.amplifyframework.auth.cognito`, `com.amplifyframework.kinesis`).
- Test files: `<ClassName>Test.kt` in the matching package under the test source tree.
- KMP source sets: `commonMain/kotlin/`, `androidMain/kotlin/`, `androidHostTest/kotlin/`.

## 2. Languages

- **New code is Kotlin.** Period.
- **Existing Java stays Java.** Don't rewrite Java for style — only when changing behaviour and the Kotlin equivalent is materially better.
- Java source is allowed to be less idiomatic than Kotlin source; hold it to "internally consistent" rather than "modern Kotlin style".

## 3. Lint, style, public-API surface

```bash
./gradlew ktlintCheck checkstyle apiCheck      # CI gate
./gradlew ktlintFormat apiDump                 # local fix
```

- KtLint 1.5.0 with the standard ruleset. Don't disable rules locally; if a rule must yield, justify in `.editorconfig` at module scope.
- `apiCheck` (binary-compatibility-validator) is a hard gate. Public API changes require an `apiDump`.
- `checkstyle` for Java only.
- Line length: 120.

## 4. Concurrency — non-negotiable

- Kotlin Coroutines exclusively. Forbidden in new code: `Executors`, `Runnable` for async work, `Future`, `RxJava`, `CountDownLatch`, `runBlocking`, `GlobalScope`.
- Suspending functions are main-safe — they never block the calling thread.
- Pick the dispatcher explicitly: `withContext(Dispatchers.IO)` for network/disk/DB; `Dispatchers.Default` for CPU.
- Every `CoroutineScope` has a defined lifetime and is cancelled when its owner dies. No fire-and-forget on a leaked scope.
- StateMachine implementation detail: a single-threaded context (`newSingleThreadContext("StateMachineContext")`) serialises transitions; actions run on `Dispatchers.Default` via `ConcurrentEffectExecutor`. Don't bypass.

## 5. Errors

- All Amplify exceptions extend `AmplifyException` with `message` + `recoverySuggestion` (mandatory, non-empty) + optional `cause`. Empty recovery suggestion is a code-review block.
- Translate AWS SDK exceptions into typed Amplify exceptions via a `*ExceptionConverter` (e.g. `CognitoExceptionConverter.toAuthException`). Never let an `AwsServiceException` leak past a use case.
- New code prefers `Result<T, E>` (sealed `Success`/`Failure` from `foundation`) over throwing. Throw only on contract violations / logic bugs.
- Sealed exception hierarchies for typed handling:
  ```kotlin
  sealed class AmplifyKinesisException(...) : AmplifyException(...) {
      companion object {
          internal fun from(error: Throwable): AmplifyKinesisException = when (error) { ... }
      }
  }
  class AmplifyKinesisStorageException(...) : AmplifyKinesisException(...)
  ```
- Functions that return null in the absence of a value, where missing-is-not-an-error, are suffixed `OrNull`.

## 6. Builders — dual Kotlin/Java

Every options/config class follows this exact shape:

```kotlin
data class Options internal constructor(val foo: String) {
    companion object {
        @JvmStatic fun builder() = Builder()
        @JvmSynthetic operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()
        @JvmStatic fun defaults() = builder().build()
    }
    class Builder internal constructor() {
        var foo: String = "defaultValue"
            @JvmSynthetic set
        fun foo(value: String) = apply { foo = value }
        fun build() = Options(foo = foo)
    }
}
```

Yields:
- Kotlin DSL: `Options { foo = "x" }`
- Java: `Options.builder().foo("x").build();`

## 7. Java interop on Kotlin code

- Companion-object public APIs annotated `@JvmStatic`.
- Hide Kotlin-only setters with `@JvmSynthetic` so Java sees only the fluent setter.
- Don't expose default-arg overloads to Java without `@JvmOverloads` if Java consumers need them.

## 8. Annotations & visibility

- `@InternalAmplifyApi` — opt-in ERROR. Internal API; binary-compatibility-validator excludes from public surface.
- `@InternalApiWarning` — opt-in WARN. Same intent, softer enforcement.
- `@ExperimentalAmplifyApi` — public-but-unstable.
- `@AmplifyFlutterApi` — opt-in ERROR. Visible only for the Flutter bridge.
- `internal` Kotlin classes are the default for use cases, actions, resolvers. Public class is the surface layer (the `*Plugin`, the category facade).

## 9. Plugin / category architecture

- Each category lives in `core/` as a Java interface + abstract `Category<P extends Plugin<?>>`.
- Plugin lifecycle: `getPluginKey()`, `getVersion()`, `configure(JSONObject, Context)`, `configure(AmplifyOutputsData, Context)` (Gen2, default throws), `initialize(Context)` (called async on a worker), `getEscapeHatch()` (nullable — return the underlying SDK client or `null`).
- Configuration sources: Gen1 `amplifyconfiguration.json` (per-category JSON object) or Gen2 `amplify_outputs.json` parsed into `AmplifyOutputsData`. New plugins MUST support both.
- Plugin status flows: `NOT_CONFIGURED → CONFIGURING → CONFIGURED → INITIALIZING → INITIALIZED`. Failures publish Hub events on `HubChannel.<CATEGORY>` with `InitializationStatus`.

## 10. Auth state machine

Mandatory pattern for stateful auth flows.

- `State` is immutable, value-semantics, sealed. Never mutate; always replace.
- `StateMachineResolver.resolve(oldState, event) → StateResolution(newState, actions)`. Pure function.
- `Action` is a coroutine-based side effect (network, store, Hub). Runs on the executor's dispatcher, not the state-machine thread.
- `Event` is a sealed type per machine (`AuthenticationEvent.EventType.SignInRequested(...)`, etc.).
- `Environment` holds non-state context (logger, store, SDK clients).
- Read state via `stateMachine.state` (a `SharedFlow`, replay=1, not a `StateFlow` — values are not conflated).
- Read current state from outside the machine via `getCurrentState()` (it dispatches onto the single-thread context).

## 11. Use case pattern (auth)

The dominant pattern in v2.26 → v2.36. Every public auth method is an `internal class *UseCase`:

```kotlin
internal class SignInUseCase(
    private val stateMachine: AuthStateMachine,
    private val configuration: AuthConfiguration,
    private val hubEmitter: AuthHubEventEmitter = AuthHubEventEmitter()
) {
    suspend fun execute(/* args */): AuthSignInResult {
        waitForStateThatAllowsSignIn()                         // 1. precondition
        val event = AuthenticationEvent(EventType.SignInRequested(...))
        val result = stateMachine.sendEventAndGetSignInResult(event)   // 2. dispatch + collect
        if (result.isSignedIn) hubEmitter.sendHubEvent(SIGNED_IN.toString())   // 3. hub
        return result                                          // 4. typed result
    }
    private suspend fun waitForStateThatAllowsSignIn(): AuthState =
        stateMachine.state.mapNotNull { /* per-state branching, throw on Error */ }.first()
}
```

Rules:
- `internal class`, constructor-injected dependencies, `suspend fun execute(...)` as the entry point.
- Validate preconditions by collecting state — never assume the machine is idle.
- Emit Hub events only on success.
- Return a typed result (`AuthSignInResult`, `AuthSignOutResult`, `AuthSession`). Never `Unit` if there's information to convey.
- Translate any AWS SDK exception via `toAuthException(...)` before throwing.
- **Fork-specific:** thread `userId: String?` through the use case constructor and pass it to credential-store / fetch operations. The multi-user `ThreadSafeLifoMap` keys by user id.

## 12. Hub events

- One `HubChannel` per category. Event name is an enum (`AuthChannelEventName.SIGNED_IN`), never a string literal.
- Emit `Amplify.Hub.publish(HubChannel.AUTH, HubEvent.create(enum, optionalData))`.
- Emit on success; never on intermediate or error state.
- Only the use case (or category facade) publishes — actions and the state machine itself do not.

## 13. Logging

- `Amplify.Logging.logger(<CategoryType>, "<tag>")` to obtain a logger.
- Lazy supplier form for non-trivial messages: `logger.debug { "value=$expensiveCall" }` — the lambda is skipped when below threshold.
- No PII in logs (token bodies, passwords, refresh tokens). The redaction is your responsibility.

## 14. Builders for new clients (foundation pattern)

New clients (e.g. `aws-kinesis`) are NOT category plugins. They:
- Live in their own module that depends on `foundation` and `foundation-bridge` (and AWS SDK Kotlin).
- Expose a `suspend fun` API that returns `Result<T, E>`.
- Use `WorkManager` for persistent background work (not raw coroutines).
- Implement the dual builder pattern (rule 6).
- Mark the public API `@ExperimentalAmplifyApi` until stable.

## 15. Build conventions

- Kotlin DSL only (`build.gradle.kts`).
- Apply convention plugins from `build-logic/plugins/`. Never use `subprojects {}` / `allprojects {}`.
  - `amplify.android.library` — Android lib (compileSdk 36, minSdk 24, ktlint, checkstyle, kotlin convention)
  - `amplify.kotlin` — JVM toolchain 17, coroutine opt-ins
  - `amplify.kmp` — Kotlin Multiplatform
  - `amplify.publishing` — Maven publish + signing
  - `amplify.kover` — coverage
  - `amplify.api-validator` — binary-compatibility-validator
- All deps come from `gradle/libs.versions.toml`. No inline `"1.2.3"` versions. No new repositories.
- `includeBuild("build-logic")` in root `settings.gradle.kts` — convention plugins are a composite build.

## 16. Tests

- Kotlin only for new tests.
- Unit-test names use backticks: `` `flush should drop record on permanent error` ``.
- Connected (`androidTest`) names DON'T use backticks (unsupported pre-API 30).
- Arrange-Act-Assert / Given-When-Then.
- Assertions: Kotest matchers (`shouldBe`, `shouldThrow`, `shouldNotBeNull`). No JUnit `assertEquals` in new code.
- Mocking: MockK (`mockk`, `coEvery`, `coVerify`, `justRun`). Mockito only when modifying legacy Mockito tests.
- Static / object mocks: scoped (`mockkStatic { … }`) or cleaned up explicitly.
- Coroutine tests: `runTest { }` from `kotlinx-coroutines-test`. Use `runCurrent()` to drain pending continuations.
- Flow tests: Turbine. `awaitItem()`, `awaitComplete()`, `expectNoEvents()`. Cancel collection before assertion to avoid flake.
- Connected tests: extend `DeviceFarmTestBase` for automatic retry.
- Custom assertions live in `:testutils` (e.g. `shouldBeSuccess`, `shouldBeFailure`).
- Robolectric over connected tests where possible.

## 17. Deprecation policy

- Pinpoint analytics + Pinpoint push notifications are deprecated (AWS sunsetting Pinpoint 2026-10-30). Don't add features to those modules; bug-fix only.
- New deprecations: annotate with `@Deprecated(message, replaceWith)` and document the sunset date in the message.

## 18. Fork-specific (multi-user) rules

These do NOT come from upstream; they are Harri's:

- A use case that touches the credential store accepts `userId: String?` and forwards it.
- The credential store's per-user instances are tracked by `ThreadSafeLifoMap` (`statemachine/util/ThreadSafeLifoMap.kt`). LIFO matters — most recently activated user is on top.
- `RealAWSCognitoAuthPlugin` exposes overloads that accept `userId`. The legacy single-user overload delegates to the multi-user one with `userId = null` (treated as the active user).
- When merging upstream, expect conflicts in `RealAWSCognitoAuthPlugin`, `AuthCategoryBehavior`, and the use cases that take `userId`. See the `amplify-merge-upstream` skill.
- Fork branches follow `feature/upgrade-to-<upstream-version>-<descriptor>` (e.g. `feature/upgrade-to-2.34.0-with-multi-user`).

## 19. Things to never do

- Add `runBlocking` in production code.
- Inline a dependency version in a `build.gradle.kts`.
- Throw a raw `RuntimeException` from a public API.
- Catch `Throwable` (catch `Exception`, or specific types).
- Use Kotlin's `runCatching` in production code — it catches `Throwable` internally. Use `try { ... } catch (e: Exception) { ... }` (matches the existing pattern in `AWSCognitoAuthCredentialStore.deserializeCredential`). When a class genuinely depends on environment infrastructure that may be missing (e.g. `EncryptedKeyValueRepository` requires `androidx.security.crypto.MasterKeys`, which JVM unit tests lack), wrap the construction in `try { ... } catch (e: Exception) { ... } catch (e: LinkageError) { ... }` and fall back to a no-op / in-memory implementation so the dependent class degrades gracefully — see `AuthStateRepo`'s `InMemoryFallbackStore`.
- Log a JWT, refresh token, password, or PII.
- Re-introduce a callback-based public API in a new module.
- Edit a generated file (anything under `build/`, `generated/`).
- Ship a public API change without `./gradlew apiDump`.
