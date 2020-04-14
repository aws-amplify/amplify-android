## Amplify for Android (Preview)
<img src="https://s3.amazonaws.com/aws-mobile-hub-images/aws-amplify-logo.png" alt="AWS Amplify" width="550">

AWS Amplify provides a high-level interface to perform different categories of cloud operations. Each category can be implemented by a _plugin_. Our default plugins support interaction with Amazon Web Services (AWS), but AWS Amplify is designed to be extensible to any other backend or service.

If you're new to the project, checkout the [Getting Started Guide](https://aws-amplify.github.io/docs/android/start).

## Features/APIs

- [**Analytics**](https://docs.amplify.aws/lib/analytics/getting-started?platform=android): Easily collect and report usage data for your app.
- [**API**](https://docs.amplify.aws/lib/graphqlapi/getting-started?platform=android): An interface for interacting with REST and GraphQL endpoints. Provides AWS Signature Version 4 signing.
- [**DataStore**](https://docs.amplify.aws/lib/datastore/getting-started?platform=android): Provides a persistent on-device storage repository for you to write, read, and observe changes to data. Data is sycnhronized to the cloud as well as across devices.
- [**Storage**](https://docs.amplify.aws/lib/storage/getting-started?platform=android): Manage user content for your app, storing it in public, protected or private buckets.

## Platform Support

Amplify SDK supports Android API level 16 (Android 4.1) and above.

## Installation

### Using Gradle

To use Amplify, specify which modules you want to use inside of your app's `build.gradle` dependencies section:

```gradle
dependencies {
    // Core SDK is required for configuring Amplify
    implementation 'com.amplifyframework:core:0.10.0'
    
    // Specify only the modules that the app will use
    implementation 'com.amplifyframework:aws-datastore:0.10.0'
    implementation 'com.amplifyframework:aws-api:0.10.0'
    implementation 'com.amplifyframework:aws-storage-s3:0.10.0'
    implementation 'com.amplifyframework:aws-analytics-pinpoint:0.10.0'
}
```

### Local Publishing of Artifacts

You can manually install the library by cloning this repo and publishing the Android modules to the local maven repository.

Execute following commands from the project root:

```
./gradlew publishToMavenLocal
```

Locally published artifacts can be accessed by specifying `mavenLocal()` inside the app's `build.gradle` file:

``` gradle
buildscript {
    repositories {
        mavenLocal() // This should ideally appear before other repositories
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.6.1'
    }
}

allprojects {
    repositories {
        mavenLocal() // This should ideally appear before other repositories
    }
}
```

## Using Amplify

### Java 8 Compatibility

Amplify Android uses Java 8 features. Please add compile options inside app's `build.gradle` like following:

``` gradle
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

### Using AWS Mobile Client

Amplify relies on the AWS Mobile Client for authentication with AWS services. Please refer to the [this documentation](https://aws-amplify.github.io/docs/android/authentication) for details.

You need to ensure `AWSMobileClient` is [initialized](https://aws-amplify.github.io/docs/android/authentication#initialization) before you initalize and configure `Amplify`. Please note that AWSMobileClient initialization is not required when using API or Datastore category with _API Key_ as the authorization mode.

## License

This library is licensed under the Apache 2.0 License. 

