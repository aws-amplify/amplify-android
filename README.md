## Amplify for Android
<img src="https://s3.amazonaws.com/aws-mobile-hub-images/aws-amplify-logo.png" alt="AWS Amplify" width="550">

[![DiscordChat](https://img.shields.io/discord/308323056592486420?logo=discord)](https://discord.gg/jWVbPfC)
[![GitHub release](https://img.shields.io/github/release/aws-amplify/amplify-android.svg)](https://github.com/aws-amplify/amplify-android/releases)
[![Maven Central](https://img.shields.io/maven-central/v/com.amplifyframework/core.svg)](https://search.maven.org/search?q=g:com.amplifyframework%20a:core)
-------------------------------------------------------

The Amplify Android library is AWS' preferred mechanism for interacting
with AWS services from an Android device.

The library provides a high-level interface to perform different
**categories** of cloud operations. Each category may be fulfilled by a
**plugin**, which you configure during setup.

The default plugins that we provide are designed to facilitate
interaction with Amazon Web Services (AWS). But, the Amplify Framework
is designed to be extensible to any other backend or service.

To familiarize yourself with Amplify, checkout our [Getting Started
Guide](https://docs.amplify.aws/start/q/integration/android).

## Categories

| Category                                                                                                     | AWS Provider | Description                                                                                   |
|--------------------------------------------------------------------------------------------------------------|--------------|-----------------------------------------------------------------------------------------------|
| **[Authentication](https://docs.amplify.aws/lib/devpreview/getting-started/q/platform/android)**             | Cognito      | Building blocks to create auth experiences                                                    |
| **[Storage](https://docs.amplify.aws/lib/storage/getting-started/q/platform/android)**                       | S3           | Manages content in public, protected, private storage buckets                                 |
| **[DataStore](https://docs.amplify.aws/lib/datastore/getting-started/q/platform/android)**                   | AppSync      | Programming model for shared and distributed data, with simple online/offline synchronization |
| **[API (GraphQL)](https://docs.amplify.aws/lib/graphqlapi/getting-started/q/platform/android)**              | AppSync      | Interact with your GraphQL or AppSync endpoint                                                |
| **[API (REST)](https://docs.amplify.aws/lib/restapi/getting-started/q/platform/android)**                    | API Gateway  | Sigv4 signing and AWS auth for API Gateway and other REST endpoints                           |
| **[Analytics](https://docs.amplify.aws/lib/analytics/getting-started/q/platform/android)**                   | Pinpoint     | Collect Analytics data for your app including tracking user sessions                          |
| **[Geo](https://docs.amplify.aws/lib/geo/getting-started/q/platform/android)**                               | Location     | Add maps to your app with APIs and map UI components                                          |
| **[Predictions](https://docs.amplify.aws/lib/predictions/getting-started/q/platform/android)**               | Various*     | Connect your app with machine learning services like NLP, computer vision, TTS, and more.     |
| **[Push Notifications](https://docs.amplify.aws/lib/push-notifications/getting-started/q/platform/android)** | Pinpoint     | Segment users, trigger push notifications, and record metrics                                 |

\* Predictions utilizes a range of Amazon's Machine Learning services,
including: Amazon Comprehend, Amazon Polly, Amazon Rekognition, Amazon
Textract, and Amazon Translate.

All services and features not listed above are supported via the [Kotlin SDK](https://github.com/awslabs/aws-sdk-kotlin) or if supported by a category can be accessed via the Escape Hatch like below:

### Kotlin

```kotlin
val s3StoragePlugin = Amplify.Storage.getPlugin("awsS3StoragePlugin")
val s3Client = s3StoragePlugin.escapeHatch as S3Client
```

### Java

```java
AWSS3StoragePlugin plugin = (AWSS3StoragePlugin) Amplify.Storage.getPlugin("awsS3StoragePlugin");
S3Client s3Client = plugin.getEscapeHatch();
```

## Platform Support

The Amplify Framework supports Android API level 24 (Android 7.0) and above.

## Using Amplify from Your App

For step-by-step setup instructions, checkout our [Project Setup
guide](https://docs.amplify.aws/lib/project-setup/prereq/q/platform/android).

### Specifying Gradle Dependencies

To begin, include Amplify from your `app` module's `build.gradle`
dependencies section:

```groovy
dependencies {
    // Only specify modules that provide functionality your app will use
    implementation 'com.amplifyframework:aws-analytics-pinpoint:2.29.2'
    implementation 'com.amplifyframework:aws-api:2.29.2'
    implementation 'com.amplifyframework:aws-auth-cognito:2.29.2'
    implementation 'com.amplifyframework:aws-datastore:2.29.2'
    implementation 'com.amplifyframework:aws-predictions:2.29.2'
    implementation 'com.amplifyframework:aws-storage-s3:2.29.2'
    implementation 'com.amplifyframework:aws-geo-location:2.29.2'
    implementation 'com.amplifyframework:aws-push-notifications-pinpoint:2.29.2'
}
```

### Java 8 Requirement

Amplify Android _requires_ Java 8 features. Please add a `compileOptions`
block inside your app's `build.gradle`, as below:

```gradle
android {
    compileOptions {
        coreLibraryDesugaringEnabled true
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
}
```
In the same file, add core library desugaring in your `dependencies`
block:
```gradle
dependencies {
    // Add this line
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.0.10'
}
```

### Kotlin & Rx Support

Amplify's default interface renders results through async callbacks. We also provide optional, adapter APIs which better integrate with RxJava and Kotlin:

 - [Using RxJava with Amplify](https://docs.amplify.aws/lib/project-setup/rxjava/q/platform/android)
 - [Kotlin Coroutines Support](https://docs.amplify.aws/lib/project-setup/coroutines/q/platform/android)

### Semantic versioning

We follow [semantic versioning](https://semver.org/) for our releases.

#### Semantic versioning and enumeration cases

When Amplify adds a new a new enumeration class entry or sealed class subtype, we
will publish a new **minor** version of the library.

Applications that use a `when` expression to evaluate all members of an enumerated
type can add an `else` branch to prevent new cases from causing compile warnings
 or errors.

#### Semantic versioning and dependencies update

We follow [semantic versioning for updating our dependencies](https://semver.org/#what-should-i-do-if-i-update-my-own-dependencies-without-changing-the-public-api). This includes updating the Kotlin language version.

## License

This library is licensed under the [Apache 2.0 License](./LICENSE).

## Report a Bug

[![Open Bugs](https://img.shields.io/github/issues/aws-amplify/amplify-android/bug?color=d73a4a&label=bugs)](https://github.com/aws-amplify/amplify-android/issues?q=is%3Aissue+is%3Aopen+label%3Abug)
[![Open Questions](https://img.shields.io/github/issues/aws-amplify/amplify-android/question?color=558dfd&label=questions)](https://github.com/aws-amplify/amplify-android/issues?q=is%3Aissue+label%3A%22question%22+is%3Aopen)
[![Feature Requests](https://img.shields.io/github/issues/aws-amplify/amplify-android/feature-request?color=ff9001&label=feature%20requests)](https://github.com/aws-amplify/amplify-android/issues?q=is%3Aissue+label%3A%22feature-request%22+is%3Aopen+)
[![Closed Issues](https://img.shields.io/github/issues-closed/aws-amplify/amplify-android?color=%2325CC00)](https://github.com/aws-amplify/amplify-android/issues?q=is%3Aissue+is%3Aclosed+)

We appreciate your feedback – comments, questions, and bug reports. Please
[submit a GitHub issue](https://github.com/aws-amplify/amplify-android/issues),
and we'll get back to you.

## Contribute to the Project

We welcome any and all contributions from the community! Make sure you read through our [Contribution Guidelines](./CONTRIBUTING.md) before submitting any PR's. Thanks! ♥️
