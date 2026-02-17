# amplify-android

## What This Is

bFAN's fork of the AWS Amplify Library for Android. This provides the Android app with high-level interfaces for AWS services (Auth, API, Storage, DataStore, Push Notifications, Geo, Analytics, Predictions). The fork contains custom modifications specific to bFAN's integration needs that differ from the upstream AWS SDK.

<!-- Ask: What specific modifications does bFAN maintain in this fork vs upstream? -->

## Tech Stack

- **Language**: Kotlin 2.2.0, Java
- **Build System**: Gradle (Kotlin DSL)
- **Minimum Android SDK**: <!-- Ask: What's the minSdkVersion? Check a module's build.gradle.kts -->
- **Package Manager**: Maven Central
- **Key Dependencies**:
  - AWS SDK for Kotlin — underlying AWS service clients
  - Apollo GraphQL — for AppSync integration
  - SQLite (via Room or similar) — DataStore persistence
  - Google Play Services — for push notifications

## Quick Start

```bash
# Setup
# Clone is already done as a submodule in bfan-ai hub

# Build the project
./gradlew build

# Run tests
./gradlew test

# Run connected tests (requires emulator/device)
./gradlew connectedAndroidTest

# Build specific module
./gradlew :core:build
```

<!-- Ask: Does bFAN have CI/CD for this fork? How are updates merged from upstream AWS? -->

## Project Structure

```
core/                                    # Core Amplify interfaces and protocols
core-kotlin/                             # Kotlin-specific core APIs
common-core/                             # Shared utilities
aws-core/                                # AWS-specific core functionality
aws-auth-cognito/                        # Cognito authentication plugin
aws-api/                                 # REST API plugin
aws-api-appsync/                         # GraphQL/AppSync plugin
aws-datastore/                           # DataStore plugin (offline-first sync)
aws-storage-s3/                          # S3 storage plugin
aws-push-notifications-pinpoint/         # Push notifications plugin
aws-push-notifications-pinpoint-common/  # Push notification utilities
aws-analytics-pinpoint/                  # Analytics plugin
aws-geo-location/                        # Geo/mapping plugin
aws-predictions/                         # ML predictions plugin
aws-predictions-tensorflow/              # TensorFlow-based predictions
rxbindings/                              # RxJava bindings (legacy)
testutils/                               # Test utilities
testmodels/                              # Test data models
canaries/                                # Integration test apps
build-logic/                             # Custom Gradle build logic
configuration/                           # Build configuration
scripts/                                 # Build and release scripts
```

## Dependencies

**Upstream dependency**: This is a fork of AWS's official `amplify-android` SDK.

**Consumed by**:
- `SA-User-WhiteLabelApps-Android` — main white-label Android app
- `BFanSSO-Android` — single sign-on Android library
- Possibly other Android projects in the bFAN ecosystem

**External services**:
- AWS Cognito (Authentication)
- AWS AppSync (GraphQL API)
- AWS S3 (Storage)
- AWS DynamoDB (DataStore)
- AWS Pinpoint (Analytics, Push Notifications)
- AWS Location Service (Geo)
- AWS ML Services (Predictions: Comprehend, Polly, Rekognition, Textract, Translate)

## API / Interface

This library is consumed as a Gradle dependency via Maven Central (or via local build). The Android app includes specific Amplify modules:

```kotlin
// In app/build.gradle.kts
dependencies {
    implementation("com.amplifyframework:core:2.x.x")
    implementation("com.amplifyframework:aws-auth-cognito:2.x.x")
    implementation("com.amplifyframework:aws-api:2.x.x")
    implementation("com.amplifyframework:aws-storage-s3:2.x.x")
    implementation("com.amplifyframework:aws-datastore:2.x.x")
    implementation("com.amplifyframework:aws-push-notifications-pinpoint:2.x.x")
    // etc.
}
```

<!-- Ask: Which Amplify plugins does the bFAN Android app actually use? -->

## Key Patterns

- **Plugin Architecture**: Amplify uses a plugin-based system where each AWS service is a separate plugin registered at runtime
- **Category-based APIs**: Services are grouped into categories (Auth, API, Storage, DataStore, Predictions, Analytics, Geo)
- **Kotlin Coroutines + RxJava**: Modern Kotlin coroutine APIs alongside legacy RxJava bindings and callbacks
- **Offline-first DataStore**: Syncs with cloud when online, works offline when disconnected
- **Escape Hatch**: Direct access to underlying AWS SDK clients when needed via `plugin.escapeHatch`

<!-- Ask: Are there specific Amplify patterns or configurations that bFAN customizes? -->

## Environment

No environment variables required for the library itself. Configuration is provided by the consuming Android app via `amplifyconfiguration.json`.

## Deployment

This is a library dependency, not a deployed service. Updates to the fork require:

1. Make changes in a feature branch
2. Test with the Android app as a local build or via `mavenLocal()`
3. Merge to `main` via PR
4. Publish to internal Maven repo or update Android app to reference specific commit

<!-- Ask: Does bFAN publish artifacts to a private Maven repo? How does the Android app pin to specific versions? -->

## Testing

- **Framework**: JUnit 4/5, AndroidX Test
- **Unit tests**: `./gradlew test`
- **Instrumented tests**: `./gradlew connectedAndroidTest` (requires emulator/device)
- **Coverage**: Kover (see `kover.gradle`)
- **Code coverage reporting**: Codecov (see `codecov.yml`)

<!-- Ask: Does bFAN run the full test suite? Do we have AWS test infrastructure for functional tests? -->

## Gotchas

- **Forked dependency**: When merging upstream changes from AWS, conflict resolution can be complex. Document bFAN-specific changes clearly.
- **Multi-module Gradle project**: Changes in `core` affect all plugins. Build from root to catch cascading issues.
- **Kotlin version compatibility**: Amplify Android tracks recent Kotlin versions. Ensure bFAN's Android apps use compatible Kotlin compiler versions.
- **AndroidX dependencies**: Amplify depends on AndroidX libraries. Conflicts with legacy support libraries will break the build.
- **API-level DataStore conflicts**: DataStore schema changes require migration logic. Test thoroughly when modifying models.
- **Escape hatch type casting**: Direct AWS SDK access requires knowledge of underlying implementation types.

<!-- Ask: What's the process for syncing upstream changes from AWS into the bFAN fork? Who owns that? -->
