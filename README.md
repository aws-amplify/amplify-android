<img src="https://s3.amazonaws.com/aws-mobile-hub-images/aws-amplify-logo.png" alt="AWS Amplify" width="400">
 <a href="https://discord.gg/jWVbPfC" target="_blank">
   <img src="https://img.shields.io/discord/308323056592486420?logo=discord"" alt="Discord Chat" />  
 </a>

## Amplify for Android (Preview)

AWS Amplify provides a high-level interface to perform different
categories of cloud operations. Each category is fulfilled by a
_plugin_. You specify which plugins to use during setup.

The default plugins that we provide are designed to facilitate
interaction with Amazon Web Services (AWS). But, the Amplify framework
is designed to be extensible to any other backend or service.

If you're new to the project, checkout the
[Getting Started Guide](https://docs.amplify.aws/lib/getting-started/setup/q/platform/android).

## The Categories

### [DataStore](https://docs.amplify.aws/lib/datastore/getting-started/q/platform/android):

Model your app's data. Save, query, and observe changes to your data
from a local repository. Let DataStore synchronize your local data with
the Cloud. Our default implemenation syncs local data to/from an Amazon
DynamoDB database, via an Amazon AppSync front-end.

### [REST APIs](https://docs.amplify.aws/lib/restapi/getting-started/q/platform/android):

Easy auth and request signing against multiple REST endpoints. Our
default plugin works great with Amazon API Gateway.

### [GraphQL APIs](https://docs.amplify.aws/lib/graphqlapi/getting-started/q/platform/android)

Data modeling and simple auth against GraphQL endpoints. Our default
plugin targets AppSync.

### [Analytics](https://docs.amplify.aws/lib/analytics/getting-started/q/platform/android):

Collect and report usage data for your app. Our default plugin
communicates with Amazon Pinpoint.

### [Storage](https://docs.amplify.aws/lib/storage/getting-started/q/platform/android):

Store and retrieve files in the Cloud. We use Amazon Simple Storage
Service (S3) by default.

## Platform Support

The Amplify Framework supports Android API level 16 (Android 4.1) and above.

## Using Amplify from Your App

### Specifying Gradle Dependencies

To begin, include Amplify from your `app` module's `build.gradle`
dependencies section:

```gradle
dependencies {
    // Only specify modules that provide functionality your app will use
    implementation 'com.amplifyframework:aws-datastore:0.10.0'
    implementation 'com.amplifyframework:aws-api:0.10.0'
    implementation 'com.amplifyframework:aws-storage-s3:0.10.0'
    implementation 'com.amplifyframework:aws-analytics-pinpoint:0.10.0'
}
```

### Java 8 Compatibility

Amplify Android uses Java 8 features. Please add a `compileOptions`
block inside your app's `build.gradle`, as below:

```gradle
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

### AWS Mobile Client

The default plugins for Amplify Android rely on the AWS Mobile Client to
provide authentication with AWS services.

Please see [Getting Started with Authentication](https://docs.amplify.aws/lib/auth/getting-started?platform=android)
for full details.

In summary, you need to ensure `AWSMobileClient` is [initialized](https://docs.amplify.aws/lib/auth/getting-started/q/platform/android#initialization)
before interfacting with `Amplify`:

```
AWSMobileClient awsAuth = AWSMobileClient.getInstance();
Context context = getApplicationContext();
awsAuth.initialize(context, new Callback<UserStateDetails>() {
    @Override
    public void onResult(UserStateDetails userStateDetails) {
        Amplify.addPlugin(new AWSApiPlugin()); // For example
        Amplify.configuration(context);
        Toast.makeText(context, "OK!", Toast.LENGTH_SHORT);
    }

    @Override
    public void onError(Exception error) {
        Toast.makeText(context, "Uh oh...", Toast.LENGTH_SHORT);
    }
});
```

Please note that `AWSMobileClient` initialization is ___not___ required
when using the `AWSApiPlugin` or `AWSDataStorePlugin` with _api key_ as the
authorization mode.

## License

This library is licensed under the [Apache 2.0 License](./LICENSE).

## Report a Bug

We appreciate your feedback -- comments, questions, and bug reports. Please
[submit a GitHub issue](https://github.com/aws-amplify/amplify-android/issues),
and we'll get back to you.

## Contribute to the Project

Please see the [Contributor's Guide](./CONTRIBUTING.md).

