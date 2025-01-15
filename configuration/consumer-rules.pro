-keepclassmembers enum * { *; }

-keep class com.amazonaws.** { *; }
-keep class com.amplifyframework.** { *; }

# We check for specific engine classes on the classpath to determine whether Amplify should use OkHttp4 instead of OkHttp5
-keepnames class aws.smithy.kotlin.runtime.http.engine.okhttp4.*