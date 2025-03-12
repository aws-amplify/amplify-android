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

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://aws.oss.sonatype.org/content/repositories/snapshots/")
        }
        google()
        mavenCentral()
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

// Events API
include(":aws-appsync-core")
include(":aws-appsync-core-amplify")
include(":aws-appsync-events")
include(":aws-appsync-events-amplify")
project(":aws-appsync-core").projectDir = file("appsync/aws-appsync-core")
project(":aws-appsync-core-amplify").projectDir = file("appsync/aws-appsync-core-amplify")
project(":aws-appsync-events").projectDir = file("appsync/aws-appsync-events")
project(":aws-appsync-events-amplify").projectDir = file("appsync/aws-appsync-events-amplify")


// Apollo Extensions
include(":apollo-appsync")
include(":apollo-appsync-amplify")
project(":apollo-appsync").projectDir = file("apollo/apollo-appsync")
project(":apollo-appsync-amplify").projectDir = file("apollo/apollo-appsync-amplify")
