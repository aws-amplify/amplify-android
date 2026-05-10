---
name: amplify-new-module
description: Scaffold a new Gradle module in this repo, or correctly modify an existing module's build.gradle.kts. Use when adding a new client/library module, splitting code out of an existing module, or pinning a new dependency in libs.versions.toml.
---

# Adding a Gradle module

The repo uses convention plugins from `build-logic/plugins/`, a version catalog at `gradle/libs.versions.toml`, and a single root `settings.gradle.kts` that lists every module. There is exactly one right way to wire each of these in.

Read `.claude/rules.md §15` first.

## Decision: category plugin or standalone client?

| Need | Pattern | Examples |
|---|---|---|
| New behaviour for an Amplify category (Auth, Storage, …) | Plugin module — implements `Plugin<E>` and `*Category` interface | `aws-auth-cognito`, `aws-storage-s3` |
| Standalone AWS client (not a category) | Foundation-pattern client — depends on `foundation` + `foundation-bridge`, returns `Result<T,E>` | `aws-kinesis` |
| Shared utility code | Plain Android library or KMP module | `core-kotlin`, `foundation`, `testutils` |
| Customer-facing API only, no Android deps | KMP module with `commonMain` | `foundation` |

Standalone clients are the **preferred shape for new code** — they don't carry the Java callback baggage of `core`.

## Convention plugins available

Under `build-logic/plugins/src/main/kotlin/`:

| Plugin id | When to apply |
|---|---|
| `amplify.android.library` | Every Android library module (also applies `amplify.kotlin`) |
| `amplify.kotlin` | JVM-only Kotlin (rare on its own) |
| `amplify.kmp` | Kotlin Multiplatform module |
| `amplify.publishing` | Module is published to Maven Central |
| `amplify.api-validator` | Public API surface enforcement (binary-compatibility-validator) |
| `amplify.kover` | Code coverage |
| `amplify.ktlint` | Implicit via `amplify.kotlin` — don't apply directly |
| `amplify.licenses` | Aggregates license metadata |

Apply the highest-level convention you can; they cascade.

## Steps to scaffold a new Android library module

1. **Create the directory**: `mkdir -p <module>/src/main/java/com/amplifyframework/<feature>` and `<module>/src/test/java/com/amplifyframework/<feature>` (kebab-case for the directory, e.g. `aws-foo`; package in normal dotted form).

2. **Create `<module>/build.gradle.kts`**:
   ```kotlin
   plugins {
       alias(libs.plugins.amplify.android.library)
       alias(libs.plugins.amplify.publishing)        // only if it's published
       alias(libs.plugins.amplify.api.validator)     // only if there's a public API
   }

   android {
       namespace = "com.amplifyframework.<feature>"
   }

   dependencies {
       implementation(project(":core"))
       implementation(libs.kotlin.coroutines)
       implementation(libs.aws.cognitoidentityprovider)   // example
       testImplementation(libs.test.mockk)
       testImplementation(libs.test.kotest.assertions)
       testImplementation(libs.test.coroutines)
   }
   ```
   No inline versions. No custom repositories. No `apply plugin:` (Groovy). No `subprojects {}`.

3. **Register in `settings.gradle.kts`** at the project root: add `include(":<module>")` in the alphabetical block.

4. **Pin new dependencies in `gradle/libs.versions.toml`**:
   ```toml
   [versions]
   foo = "1.2.3"

   [libraries]
   foo-bar = { group = "com.example", name = "bar", version.ref = "foo" }

   [plugins]
   foo-plugin = { id = "com.example.plugin", version.ref = "foo" }
   ```
   If a version line already pins what you want, reuse it — don't add a duplicate.

5. **Refresh consumer modules.** If you intend other modules to depend on this one, add `implementation(project(":<module>"))` to their `build.gradle.kts`. Don't create cyclic deps; the build will detect them.

6. **Add a public-API baseline** (only if `amplify.api-validator` was applied): `./gradlew :<module>:apiDump`. Commit the resulting `<module>/api/<module>.api` file.

7. **Verify the wiring**: `./gradlew :<module>:assembleDebug :<module>:test ktlintCheck apiCheck`.

## KMP module variant

For a KMP module (like `foundation`):

```kotlin
plugins {
    alias(libs.plugins.amplify.kmp)
    alias(libs.plugins.amplify.publishing)
}

kotlin {
    androidTarget()
    jvm()                                      // optional, if needed
    sourceSets {
        commonMain.dependencies { /* shared deps */ }
        androidMain.dependencies { /* android-only deps */ }
        commonTest.dependencies { /* shared tests */ }
    }
}
```

Source layout: `commonMain/kotlin/`, `androidMain/kotlin/`, `androidHostTest/kotlin/`, `commonTest/kotlin/`.

## Standalone client (foundation pattern)

Mirror `aws-kinesis/`:

```kotlin
plugins {
    alias(libs.plugins.amplify.android.library)
    alias(libs.plugins.amplify.publishing)
    alias(libs.plugins.amplify.api.validator)
}

dependencies {
    implementation(project(":foundation"))
    implementation(project(":foundation-bridge"))
    implementation(libs.aws.<service>)
    implementation(libs.androidx.workmanager)
}
```

Public API marked `@ExperimentalAmplifyApi` until stable. API returns `Result<T, E>` from `foundation`. No `Consumer<T>` callbacks — `suspend fun` only.

## Things to never do

- Add `repositories { ... }` to a module. The root `settings.gradle.kts` declares them with `FAIL_ON_PROJECT_REPOS`. Adding one will fail the build — and you should let it.
- Apply a Gradle plugin by string id when there's an `alias(libs.plugins.…)` available.
- Inline a version string. Catalog only.
- Use `subprojects {}` or `allprojects {}` to share build logic. Convention plugins exist precisely so we don't.
- Forget to add `include(":<module>")` to root `settings.gradle.kts`. The build will silently work for the existing modules but never compile yours.
- Skip `./gradlew apiDump` after exposing a new public symbol when `amplify.api-validator` is applied. CI will reject the PR.

## Quick verification checklist

```bash
./gradlew :<module>:assembleDebug                 # compiles
./gradlew :<module>:test                          # unit tests
./gradlew ktlintCheck checkstyle apiCheck         # gates
./gradlew :<module>:dependencies | head -50       # sanity-check resolved deps
```
