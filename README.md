## Amplify for Android

A declarative library for application development using cloud services.

## Local Publishing of artifacts

In order to publish the Android modules to the local maven repository,  execute following commands from the project root:

```
./gradlew publishToMavenLocal
```

## Remote Publishing of artifacts

In order to publish the Android modules to the maven central repository through Sonatype,  execute following commands from the project root:

```
./gradlew :<name-of-Android-module>:uploadArchives
```

For example, to publish `amplify-core`, execute the following command:

```
./gradlew :amplify-core:uploadArchives
```

## License

This library is licensed under the Apache 2.0 License. 
