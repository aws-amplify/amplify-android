/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://aws.oss.sonatype.org/content/repositories/snapshots/")
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        // Versions of libraries that have both production and test code
        val navigationVersion = "2.3.4"

        // Dependencies for testing
        create("testDependency") {
            // JUnit
            library("junit", "junit:junit:4.13.2")

            // Mockito
            library("mockito", "org.mockito:mockito-core:3.9.0")
            library("mockitoinline", "org.mockito:mockito-inline:3.11.2")

            // MockK
            version("mockk", "1.12.3")
            library("mockk", "io.mockk", "mockk").versionRef("mockk")
            library("mockk-android", "io.mockk", "mockk-android").versionRef("mockk")

            // Kotlin
            library("kotlin-test-junit", "org.jetbrains.kotlin:kotlin-test-junit:1.5.31")
            library("kotlin-test-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.3")
            library("kotlin-test-kotlinTest", "org.jetbrains.kotlin:kotlin-test:1.7.10")
            library("kotlin-reflection", "org.jetbrains.kotlin:kotlin-reflect:1.7.10")

            // AndroidX
            version("navigation", navigationVersion)
            library("androidx-test-core", "androidx.test:core:1.3.0")
            library("androidx-test-core-ktx", "androidx.test:core-ktx:1.3.0")
            library("androidx-test-runner", "androidx.test:runner:1.3.0")
            library("androidx-test-junit", "androidx.test.ext:junit:1.1.2")
            library("androidx-test-espresso", "androidx.test.espresso:espresso-core:3.3.0")
            library("androidx-test-orchestrator", "androidx.test:orchestrator:1.3.0")
            library("androidx-test-navigation", "androidx.navigation", "navigation-testing").versionRef("navigation")
            library("androidx-test-fragment", "androidx.fragment:fragment-testing:1.3.1")
            library("androidx-test-workmanager", "androidx.work:work-testing:2.7.1")

            // Misc
            library("mockwebserver", "com.squareup.okhttp3:mockwebserver:5.0.0-alpha.11")
            library("robolectric", "org.robolectric:robolectric:4.7")
            library("jsonassert", "org.skyscreamer:jsonassert:1.5.0")
            library("json", "org.json:json:20210307")

            // AWS
            library("aws-sdk-core", "com.amazonaws:aws-android-sdk-core:2.62.2")
        }
        // Library dependencies
        create("dependency") {
            // Android Tools
            library("android-desugartools", "com.android.tools:desugar_jdk_libs:1.2.0")

            // AndroidX
            val nagivation = "navigation"
            version(nagivation, navigationVersion)
            library("androidx-v4support", "androidx.legacy:legacy-support-v4:1.0.0")
            library("androidx-activity", "androidx.activity:activity:1.2.0")
            library("androidx-annotation", "androidx.annotation:annotation:1.1.0")
            library("androidx-appcompat", "androidx.appcompat:appcompat:1.2.0")
            library("androidx-browser", "androidx.browser:browser:1.4.0")
            library("androidx-core", "androidx.core:core:1.3.2")
            library("androidx-core-ktx", "androidx.core:core-ktx:1.3.2")
            library("androidx-workmanager", "androidx.work:work-runtime-ktx:2.7.1")
            library("androidx-security", "androidx.security:security-crypto:1.0.0")
            library("androidx-nav-fragment", "androidx.navigation", "navigation-fragment").versionRef(nagivation)
            library("androidx-nav-ui", "androidx.navigation", "navigation-ui").versionRef(nagivation)
            library("androidx-lifecycle-runtime", "androidx.lifecycle:lifecycle-runtime:2.4.1")
            library("androidx-sqlite", "androidx.sqlite:sqlite:2.2.0")

            // AWS
            val awsKotlinSdk = "awsKotlinSdk"
            val awsSmithyKotlin = "awsSmithyKotlin"
            /* When updating kotlin sdk or kotlin smithy,
            ensure compatible versions used in aws-sdk-kotlin root gradle.properties
             */
            version(awsKotlinSdk, "0.28.0-beta") // ensure proper awsSmithyKotlin version also set
            version(awsSmithyKotlin, "0.22.0") // ensure proper awsKotlinSdk version also set

            library("aws-credentials", "aws.smithy.kotlin", "aws-credentials").versionRef(awsSmithyKotlin)
            library("aws-signing", "aws.smithy.kotlin", "aws-signing-default").versionRef(awsSmithyKotlin)
            library("aws-http", "aws.sdk.kotlin", "aws-http").versionRef(awsKotlinSdk)
            library("aws-cognitoidentity", "aws.sdk.kotlin", "cognitoidentity").versionRef(awsKotlinSdk)
            library(
                "aws-cognitoidentityprovider",
                "aws.sdk.kotlin",
                "cognitoidentityprovider",
            ).versionRef(awsKotlinSdk)
            library("aws-comprehend", "aws.sdk.kotlin", "comprehend").versionRef(awsKotlinSdk)
            library("aws-location", "aws.sdk.kotlin", "location").versionRef(awsKotlinSdk)
            library("aws-s3", "aws.sdk.kotlin", "s3").versionRef(awsKotlinSdk)
            library("aws-pinpoint", "aws.sdk.kotlin", "pinpoint").versionRef(awsKotlinSdk)
            library("aws-polly", "aws.sdk.kotlin", "polly").versionRef(awsKotlinSdk)
            library("aws-rekognition", "aws.sdk.kotlin", "rekognition").versionRef(awsKotlinSdk)
            library("aws-textract", "aws.sdk.kotlin", "textract").versionRef(awsKotlinSdk)
            library("aws-translate", "aws.sdk.kotlin", "translate").versionRef(awsKotlinSdk)
            library("aws-cloudwatchlogs", "aws.sdk.kotlin", "cloudwatchlogs").versionRef(awsKotlinSdk)

            // Kotlin
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
            library("kotlin-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")
            library("kotlin-coroutines-android", "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.3")
            library("kotlin-serializationJson", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
            library("kotlin-futures", "androidx.concurrent:concurrent-futures-ktx:1.1.0")

            // MapLibre
            library("maplibre-sdk", "org.maplibre.gl:android-sdk:9.6.0")
            library("maplibre-annotations", "org.maplibre.gl:android-plugin-annotation-v9:1.0.0")

            // RxJava
            library("rxandroid", "io.reactivex.rxjava3:rxandroid:3.0.0")
            library("rxjava", "io.reactivex.rxjava3:rxjava:3.0.6")

            // Google
            library("google-material", "com.google.android.material:material:1.8.0")
            library("firebasemessaging", "com.google.firebase:firebase-messaging-ktx:23.1.0")

            // Misc
            library("oauth2", "com.google.auth:google-auth-library-oauth2-http:0.26.0")
            library("okhttp", "com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
            library("gson", "com.google.code.gson:gson:2.8.9")
            library("tensorflow", "org.tensorflow:tensorflow-lite:2.0.0")
            library("uuidgen", "com.fasterxml.uuid:java-uuid-generator:4.0.1")
            library("sqlcipher", "net.zetetic:android-database-sqlcipher:4.5.4")
        }
    }
}

include(":annotations")
include(":aws-core")
include(":core")
include(":common-core")
include(":aws-auth-plugins-core")

// Plugin Modules
include(":aws-analytics-pinpoint")
include(":aws-api")
include(":aws-auth-cognito")
include(":aws-datastore")
include(":aws-geo-location")
include(":aws-predictions")
include(":aws-predictions-tensorflow")
include(":aws-push-notifications-pinpoint")
include(":aws-storage-s3")

// Test Utilities and assets
include(":testutils")
include(":testmodels")

// Bindings and accessory modules
include(":core-kotlin")
include(":rxbindings")
include(":aws-api-appsync")
include(":maplibre-adapter")
include(":aws-pinpoint-core")
include(":aws-push-notifications-pinpoint-common")
include(":aws-logging-cloudwatch")

includeBuild("../smithy-starter")
