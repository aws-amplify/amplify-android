# Project Info

## Setup

```bash
./gradlew build
```

## Project Coding Conventions & Patterns Summary

### Project Overview
- **Languages**: Kotlin (primary for all new code) and Java (legacy — existing Java code is not subject to Kotlin standards, but should be internally consistent)
- **Architecture**: Gradle multi-module monorepo 
- **Build system**: Gradle with Kotlin DSL (`build.gradle.kts`), convention plugins in `build-logic/plugins/`
- **Min SDK**: 24, **Compile SDK**: 36, **JVM toolchain**: 17
- **Dependency management**: Version catalog at `gradle/libs.versions.toml` — all dependencies, plugins, and versions MUST be declared there
- Root `gradle.properties` — version (`VERSION_NAME`), group (`POM_GROUP=com.amplifyframework`)

---

### 1. File & Directory Naming
- **PascalCase** for Kotlin/Java source files matching class name: `AmplifyKinesisClient.kt`, `AmplifyException.java`, etc. 
- Module directories use kebab-case: `aws-auth-cognito`, `aws-api-appsync`, `core-kotlin`
- Package structure: `com.amplifyframework.<category>` (e.g., `com.amplifyframework.kinesis`, `com.amplifyframework.auth.cognito`)
- Test files: `<ClassName>Test.kt` — placed in the same package under the test source tree
- KMP modules use `commonMain/kotlin/`, `androidMain/kotlin/`, `androidHostTest/kotlin/` source sets

### 2. Code Style & Lint Rules
```bash
# Check
./gradlew ktlintCheck checkstyle apiCheck

# Fix
./gradlew ktlintFormat apiDump
```

### 3. Architecture Patterns

#### 3a. Category/Plugin Architecture (V2 — existing modules)
- **Categories**: Abstract interfaces (`AuthCategory`, `StorageCategory`, etc.) define the public API in `core`
- **Plugins**: Concrete implementations (`AWSCognitoAuthPlugin`) implement category interfaces
- **Escape hatch**: Plugins expose the underlying SDK client via `getEscapeHatch()`
- Java callback pattern: `Consumer<T>` for success, `Consumer<Exception>` for error, `Action` for void callbacks

#### 3b. Foundation Module (new code)
- KMP module (`foundation`) with `commonMain` source set — shared across platforms
- `Result<T, E>` sealed interface with `Success` and `Failure` subtypes — used instead of throwing exceptions
- `AmplifyException` base class with `message`, `recoverySuggestion`, and `cause`
- `Logger` interface with lazy message suppliers: `logger.debug { "message" }`
- `AwsCredentialsProvider` for credential management

#### 3c. Client Pattern (new modules like `aws-kinesis`)
- Standalone clients (not category plugins): `AmplifyKinesisClient`, `AmplifyFirehoseClient`
- Suspend-first API returning `Result<T, E>` — no exceptions thrown in normal usage
- Options via builder pattern with Kotlin DSL + Java builder support (see Builders section)

### 4. Builders
Builders MUST work in both Kotlin and Java:
```kotlin
data class Options internal constructor(val foo: String) {
    companion object {
       @JvmStatic
       fun builder() = Builder()

       @JvmSynthetic
       operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()

       @JvmStatic
       fun defaults() = builder().build()
    }

    class Builder internal constructor() {
        var foo: String = "defaultValue"
            @JvmSynthetic set

        fun foo(value: String) = apply { foo = value }

        fun build() = Options(foo = foo)
    }
}
```
This enables idiomatic usage in both languages:
```kotlin
// Kotlin DSL
val options = Options { foo = "something" }

// Java builder
Options options = Options.builder().foo("something").build();
```

### 5. Java Interop
- Amplify is Kotlin-first but supports Java consumers
- Companion object public APIs MUST be annotated `@JvmStatic`

### 6. Error Handling
- Kotlin code SHOULD NOT throw exceptions as part of their API — use `Result<T, E>` instead
- Exceptions are reserved for unexpected errors (logic bugs, contract violations)
- Functions returning null in exceptional cases SHOULD be suffixed with `OrNull`
- Sealed exception hierarchies for typed error handling:
  ```kotlin
  sealed class AmplifyKinesisException(...) : AmplifyException(...) {
      companion object {
          internal fun from(error: Throwable): AmplifyKinesisException = when (error) { ... }
      }
  }
  class AmplifyKinesisStorageException(...) : AmplifyKinesisException(...)
  class AmplifyKinesisValidationException(...) : AmplifyKinesisException(...)
  ```
- All exceptions include `message`, `recoverySuggestion`, and optional `cause`

### 7. Concurrency
- New code MUST use Kotlin Coroutines — avoid Executors, Runnables, Futures, RxJava, CountdownLatch
- Suspending functions MUST be main-safe (never block the calling thread)
- Use `withContext(Dispatchers.IO)` for IO-bound work, `withContext(Dispatchers.Default)` for CPU-bound
- SHOULD NOT use `GlobalScope` or `runBlocking` (except bridging legacy blocking APIs on background threads)
- Every `CoroutineScope` MUST have a defined lifetime and be canceled when done

### 8. Custom Annotations
- `@InternalAmplifyApi` — ERROR-level opt-in; internal API not for external use
- `@InternalApiWarning` — WARNING-level opt-in; same intent, softer enforcement
- `@AmplifyFlutterApi` — ERROR-level opt-in; visible only for Amplify Flutter bridge
- All three are excluded from the public API surface by the binary compatibility validator

### 9. Testing Patterns
- New tests MUST be written in Kotlin
- Unit tests use backtick names: `` `flush should handle mixed record states correctly` ``
- Connected Android tests MUST NOT use backticks (unsupported pre-API 30)
- Arrange-Act-Assert structure (Given-When-Then)
- Assertions: Use Kotest assertions (`shouldBe`, `shouldNotBeNull`) — not JUnit `assertEquals`
- Mocking: MockK preferred (`mockk`, `coEvery`, `coVerify`). Use `justRuns` for Unit-returning mocks. Prefer `verify` with `withArg` over slots.
- Static mocks (`mockkStatic`, `mockkObject`) MUST be cleaned up — prefer scoped versions
- Prefer unit tests wherever reasonable, Robolectric over connected Android tests
- Coroutine tests use `runTest` from `kotlinx-coroutines-test`
- Custom test assertions in `testutils` module (e.g., `shouldBeSuccess`, `shouldBeFailure`)
- Connected Android tests MUST extend `DeviceFarmTestBase` to get automatic retries on network errors

### 10. Gradle Build System
- New Gradle files MUST use Kotlin DSL (`.gradle.kts`)
- Build logic shared via convention plugins in `build-logic/plugins/` — NOT via `subprojects`/`allprojects`
- Convention plugins are hierarchical: `amplify.android.library` applies `amplify.kotlin` which applies `amplify.ktlint`
- All dependencies declared in `gradle/libs.versions.toml` using type-safe accessors
- Convention plugins included via `includeBuild("build-logic")` in `settings.gradle.kts`

### 11. Module Structure
Key modules:
- `core` — Framework categories, plugin interfaces, `AmplifyException` (Java)
- `core-kotlin` — Kotlin coroutine facades (`suspend fun` wrappers around callback APIs)
- `foundation` — KMP module with `Result`, `Logger`, `AwsCredentialsProvider`, new `AmplifyException`
- `foundation-bridge` — Bridges foundation types to V2 types
- `annotations` — `@InternalAmplifyApi`, `@InternalApiWarning`, `@AmplifyFlutterApi`
- `aws-auth-cognito` — Cognito auth plugin (state machine-based, largest module)
- `aws-auth-plugins-core` — Shared auth plugin utilities
- `aws-api`, `aws-datastore`, `aws-storage-s3`, `aws-analytics-pinpoint`, etc. — Category plugin implementations
- `aws-kinesis` — Standalone client (new pattern, not a category plugin)
- `testutils`, `testmodels` — Shared test infrastructure
- `rxbindings` — RxJava bindings (legacy)
