1. `./gradlew build`
2. `./gradlew publishToMavenLocal`
3. add local maven repository to `build.gradle` file

```groovy
repositories {
    mavenLocal()
    ...
}
```

4. add utils dependency to app’s `build.gradle`

```groovy
dependencies {
   implementation 'com.amplifyframework:aws-push-notifications-pinpoint-utils:2.0.0'
}
```
