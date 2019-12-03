## Amplify for Android (Beta)
<img src="https://s3.amazonaws.com/aws-mobile-hub-images/aws-amplify-logo.png" alt="AWS Amplify" width="550" >
AWS Amplify provides a declarative and easy-to-use interface across different categories of cloud operations. AWS Amplify goes well with any JavaScript based frontend workflow, and React Native for mobile developers.

Our default implementation works with Amazon Web Services (AWS), but AWS Amplify is designed to be open and pluggable for any custom backend or service.

- **API Documentation**
  https://aws-amplify.github.io/amplify-amplify/docs

## Features/APIs
*Note: Amplify docs are still being updated and will go live by EOW. The below links will take you to the SDK documentation currently.

- [**Analytics**](https://aws-amplify.github.io/docs/android/analytics): Easily collect analytics data for your app. Analytics data includes user sessions and other custom events that you want to track in your app.
- [**API**](https://aws-amplify.github.io/docs/android/api): Provides a simple solution when making HTTP requests. It provides an automatic, lightweight signing process which complies with AWS Signature Version 4.
- [**GraphQL Client**](https://aws.github.io/aws-amplify/media/api_guide#configuration-for-graphql-server): Interact with your GraphQL server or AWS AppSync API with an easy-to-use & configured GraphQL client.
- [**Storage**](https://aws-amplify.github.io/docs/android/storage): Provides a simple mechanism for managing user content for your app in public, protected or private storage buckets.

All services and features not listed above are supported via the Escape Hatch with the [Android SDK](https://github.com/aws-amplify/aws-sdk-android) like below:

``` java
AmazonS3Client s3Client = Amplify.Storage.getEscapeHatch();
List<Bucket> buckets = s3Client.listBuckets();
```

## Platform Support

Amplify SDK supports Android API level 15 (Android 4.0.3) and above.

## License

This library is licensed under the Apache 2.0 License. 

## Installation

### Local Publishing of Artifacts

You can manually install the library by cloning this repo and publishing the Android modules to the local maven repository.

Execute following commands from the project root:

``` console
./gradlew publishToMavenLocal
```

Locally published artifacts can be accessed by specifying `mavenLocal()` inside the app's `build.gradle` file:

``` gradle
buildscript {
    repositories {
        mavenLocal() // This should ideally appear before other repositories
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.5.1'

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        mavenLocal() // This should ideally appear before other repositories
    }
}
```
