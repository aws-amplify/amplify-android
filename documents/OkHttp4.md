# Using OkHttp4 in Amplify Android

Amplify Android v2 uses OkHttp5 by default, but starting with release `2.26.0` it will switch all clients to use OkHttp4 if the `OkHttp4Engine`
is available on the runtime classpath. Please use these steps to switch to using OkHttp4.

## 1. Upgrade Amplify if necessary

You must be using at least Amplify `2.26.0` to use OkHttp4.

## 2. Add the required dependency

Add the dependency on the `OkHttp4Engine` library to your application's `build.gradle.kts`

```kotlin
dependencies {
    implementation("aws.smithy.kotlin:http-client-engine-okhttp4:1.3.32") // Version must align with Smithy dependency in Amplify
}
```

To determine the correct version for the above dependency check in Amplify's [libs.versions.toml](../gradle/libs.versions.toml) file.
Ensure that you are viewing the file version for the Amplify version you are using, and then check the version entry for `aws-smithy`.
Remember to keep these versions in sync when you update Amplify.

## 3. Force the OkHttp version

Add the following snippet in your application's `build.gradle.kts` file:

```kotlin
configurations.configureEach {
    // Force replace OkHttp5 with OkHttp4
    resolutionStrategy {
        force("com.squareup.okhttp3:okhttp:4.12.0") // Or whicher OkHttp version you want
    }
    // Exclude other OkHttp5 dependencies
    exclude(group = "com.squareup.okhttp3", module = "okhttp-coroutines")
}
```

## 4. Add Proguard/R8 rules

If you are using obfuscation/minification you may encounter compilation errors in affected builds. Check
`build/outputs/mapping/<variant>/missing_rules.txt` for any rules that are needed. The following
rules may need to be added to `proguard-rules.pro`:

```
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn okhttp3.ConnectionListener$Companion
-dontwarn okhttp3.ConnectionListener
-dontwarn okhttp3.coroutines.ExecuteAsyncKt
```

## Troubleshooting

Please refer to the [AWS SDK for Kotlin document](https://github.com/smithy-lang/smithy-kotlin/tree/main/runtime/protocol/http-client-engines/http-client-engine-okhttp4) on this topic or [Open an Issue](https://github.com/aws-amplify/amplify-android/issues/new/choose) if you run into any problems.