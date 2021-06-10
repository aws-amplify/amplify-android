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

| Category                                                                                        | AWS Provider | Description                                |
|-------------------------------------------------------------------------------------------------|--------------|--------------------------------------------|
| **[Authentication](https://docs.amplify.aws/lib/auth/getting-started/q/platform/android)**      | Cognito      | Building blocks to create auth experiences |
| **[Storage](https://docs.amplify.aws/lib/storage/getting-started/q/platform/android)**          | S3           | Manages content in public, protected, private storage buckets |
| **[DataStore](https://docs.amplify.aws/lib/datastore/getting-started/q/platform/android)**      | AppSync      | Programming model for shared and distributed data, with simple online/offline synchronization |
| **[API (GraphQL)](https://docs.amplify.aws/lib/graphqlapi/getting-started/q/platform/android)** | AppSync      | Interact with your GraphQL or AppSync endpoint |
| **[API (REST)](https://docs.amplify.aws/lib/restapi/getting-started/q/platform/android)**       | API Gateway  | Sigv4 signing and AWS auth for API Gateway and other REST endpoints |
| **[Analytics](https://docs.amplify.aws/lib/analytics/getting-started/q/platform/android)**      | Pinpoint     | Collect Analytics data for your app including tracking user sessions |
| **[Predictions](https://docs.amplify.aws/lib/predictions/getting-started/q/platform/android)**  | Various*     | Connect your app with machine learning services like NLP, computer vision, TTS, and more. |

\* Predictions utilizes a range of Amazon's Machine Learning services,
including: Amazon Comprehend, Amazon Polly, Amazon Rekognition, Amazon
Textract, and Amazon Translate.

## Platform Support

The Amplify Framework supports Android API level 16 (Android 4.1) and above.

## Using Amplify from Your App

For step-by-step setup instructions, checkout our [Project Setup
guide](https://docs.amplify.aws/lib/project-setup/prereq/q/platform/android).

### Specifying Gradle Dependencies

To begin, include Amplify from your `app` module's `build.gradle`
dependencies section:

```groovy
dependencies {
    // Only specify modules that provide functionality your app will use
    implementation 'com.amplifyframework:aws-analytics-pinpoint:1.18.0'
    implementation 'com.amplifyframework:aws-api:1.18.0'
    implementation 'com.amplifyframework:aws-auth-cognito:1.18.0'
    implementation 'com.amplifyframework:aws-datastore:1.18.0'
    implementation 'com.amplifyframework:aws-predictions:1.18.0'
    implementation 'com.amplifyframework:aws-storage-s3:1.18.0'
}
```

### Java 8 Requirement

Amplify Android _requires_ Java 8 features. Please add a `compileOptions`
block inside your app's `build.gradle`, as below:

```gradle
android {
    compileOptions {
        coreLibraryDesugaringEnabled true
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
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

### Authentication

The default plugins for Amplify Android use the Authentication category to
provide authentication with AWS services. The default implementation uses Amazon
Cognito which allows you to add user sign-up, sign-in, and access control to
your mobile apps.

Please see [Getting Started with
Authentication](https://docs.amplify.aws/lib/auth/getting-started/q/platform/android)
for full details.

## License

This library is licensed under the [Apache 2.0 License](./LICENSE).

## Report a Bug

We appreciate your feedback -- comments, questions, and bug reports. Please
[submit a GitHub issue](https://github.com/aws-amplify/amplify-android/issues),
and we'll get back to you.

## Contribute to the Project

Please see the [Contributing Guidelines](./CONTRIBUTING.md).
