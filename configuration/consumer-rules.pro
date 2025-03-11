-keepclassmembers enum * { *; }

-keep class com.amazonaws.** { *; }
-keep class com.amplifyframework.** { *; }

# We check for specific engine classes on the classpath to determine whether Amplify should use OkHttp4 instead of OkHttp5
-keepnames class aws.smithy.kotlin.runtime.http.engine.okhttp4.*

# OkHttp4 will not be present if not explicitly added by the customer, don't warn if it's missing
-dontwarn aws.smithy.kotlin.runtime.http.engine.okhttp4.OkHttp4Engine

# This Tink annotation is missing from an upstream dependency
-dontwarn com.google.errorprone.annotations.Immutable