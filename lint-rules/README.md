# Lint Rules

Custom Android Lint checks enforcing this repository's own conventions. Internal only — this module
is not published, and Lint loads its jar from the build rather than from a consumer's classpath.

The `amplify.lint` convention plugin attaches these checks to every module of the main build,
including the non-Android ones. (`build-logic` is a separate included build and is not covered.)

## Rules

| Issue id | Severity | Reports |
|---|---|---|
| `AmplifyPrintln` | Error | `print`/`println` resolving to `kotlin.io.ConsoleKt` or a `java.io.PrintStream` subclass. Use a `Logger` instead. |

## Running

```bash
./gradlew :lint-rules:test    # this module's own tests
./gradlew lint                # run the checks against the whole repo
./gradlew :core:lint          # or against one module
```

Lint warnings are errors in every module, so a new built-in check arriving with an AGP bump can break
the build. The escape hatch is a per-issue severity override in the root `lint.xml`, or a Lint
baseline — not disabling `warningsAsErrors`.

## Adding a rule

1. Write a `Detector` in `src/main/java/com/amplifyframework/lint/`, exposing its `Issue` as `ISSUE`
   in a companion object.
2. Add that `ISSUE` to the `issues` list in `AmplifyIssueRegistry`. A rule missing from that list
   does nothing.
3. Test it in `src/test/java/com/amplifyframework/lint/` with `TestLintTask.lint()`, and assert the
   new `ISSUE` is present in `AmplifyIssueRegistry().issues` so step 2 cannot be silently skipped.
   Include a case that must *not* be flagged — otherwise the test passes even if the detector only
   matches method names instead of resolving types.

Run `./gradlew :lint-rules:lint` as well as the tests. `TestLintTask` never lints detector source, so
Lint's checks-on-checks only run through Gradle, and two of them catch easy mistakes: a multi-line
`explanation` needs a trailing `\` on every non-final line (`LintImplTextFormat`), and it must not call
`.trimIndent()`, since Lint trims lazily itself (`LintImplTrimIndent`).

## Suppressing

Suppress a genuine exception at the call site, with a comment explaining why:

- Kotlin: `@Suppress("<IssueId>")`
- Java: `@SuppressLint("<IssueId>")`, or `//noinspection <IssueId>` in modules where
  `android.annotation` is not on the classpath.

## Dependencies

Two declarations in `build.gradle.kts` look redundant but are load-bearing:

- `lint-api` is both `compileOnly` and `testImplementation`. Lint supplies it at runtime, and
  `lint-tests` exposes its own dependencies at runtime scope only, so tests need it declared again.
- `test-junit` is declared individually rather than via the `test-unit` bundle, whose other members
  (MockK, coroutines-test, Kotest, Turbine) are unused here.

The `lint` version tracks `agp` with a major version 23 higher; a `check` in `build.gradle.kts`
enforces the pairing, so bump both together in `gradle/libs.versions.toml`.
