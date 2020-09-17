<img src="https://s3.amazonaws.com/aws-mobile-hub-images/aws-amplify-logo.png" alt="AWS Amplify" width="550">

 <a href="https://discord.gg/jWVbPfC" target="_blank">
   <img src="https://img.shields.io/discord/308323056592486420?logo=discord" alt="Discord Chat" />  
 </a>
 <a href="https://codecov.io/gh/aws-amplify/amplify-android">
   <img src="https://codecov.io/gh/aws-amplify/amplify-android/branch/main/graph/badge.svg" />
 </a>


AWS Amplify provides a high-level interface to perform different
**categories** of cloud operations. Each category is fulfilled by a
**plugin**. You specify which plugins to use during setup.

The default plugins that we provide are designed to facilitate interaction with
Amazon Web Services (AWS). But, the Amplify Framework is designed to be
extensible to any other backend or service.

To familiarize yourself with Amplify, checkout our [Getting Started
Guide](https://docs.amplify.aws/start/q/integration/android).

## Categories

- **[Authentication](https://docs.amplify.aws/lib/auth/getting-started/q/platform/android)**
  APIs and building blocks for developers who want to create user authentication
  experiences.
- **[Storage](https://docs.amplify.aws/lib/storage/getting-started/q/platform/android)**
  Provides a simple mechanism for managing user content for your app in public,
  protected or private storage buckets
- **[DataStore](https://docs.amplify.aws/lib/datastore/getting-started/q/platform/android)**
  Provides a programming model for leveraging shared and distributed data
  without writing additional code for offline and online scenarios.
- **[API
  (GraphQL)](https://docs.amplify.aws/lib/graphqlapi/getting-started/q/platform/android)**
  Interact with your GraphQL server or AWS AppSync API with an easy-to-use &
  configured GraphQL client.
- **[API
  (REST)](https://docs.amplify.aws/lib/restapi/getting-started/q/platform/android)**
  Provides a simple solution when making HTTP requests. It provides an
  automatic, lightweight signing process which complies with AWS Signature
  Version 4.
- **[Analytics](https://docs.amplify.aws/lib/analytics/getting-started/q/platform/android)**
  Easily collect analytics data for your app. Analytics data includes user
  sessions and other custom events.
- **[Predictions](https://docs.amplify.aws/lib/predictions/getting-started/q/platform/android)**
  Connect your application with machine learning cloud services to enhance your
  application with natural language processing, computer vision, text to speech,
  and more.

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
    implementation 'com.amplifyframework:aws-analytics-pinpoint:1.3.2'
    implementation 'com.amplifyframework:aws-api:1.3.2'
    implementation 'com.amplifyframework:aws-auth-cognito:1.3.2'
    implementation 'com.amplifyframework:aws-datastore:1.3.2'
    implementation 'com.amplifyframework:aws-predictions:1.3.2'
    implementation 'com.amplifyframework:aws-storage-s3:1.3.2'
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

### Rx Support

By default, Amplify's interfaces render results through async callbacks.
However, we also offer an Rx-compatible front-end to Amplify.

See [Using RxJava with Amplify](https://docs.amplify.aws/lib/project-setup/rxjava/q/platform/android)
for more information.

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

