/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.testutils.configuration

import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.core.configuration.AmplifyOutputsData
import kotlinx.serialization.json.JsonObject

// Internal DSL builder that pre-populates required fields with default test data
fun amplifyOutputsData(func: AmplifyOutputsDataBuilder.() -> Unit): AmplifyOutputsData =
    AmplifyOutputsDataBuilder().apply(func).build()

class AmplifyOutputsDataBuilder {
    var analytics: AmplifyOutputsData.Analytics? = null
    var auth: AmplifyOutputsData.Auth? = null
    var data: AmplifyOutputsData.Data? = null
    var geo: AmplifyOutputsData.Geo? = null
    var notifications: AmplifyOutputsData.Notifications? = null
    var storage: AmplifyOutputsData.Storage? = null
    var custom: JsonObject? = null

    fun analytics(func: AnalyticsBuilder.() -> Unit) {
        analytics = AnalyticsBuilder().apply(func).build()
    }

    fun auth(func: AuthBuilder.() -> Unit) {
        auth = AuthBuilder().apply(func).build()
    }

    fun data(func: DataBuilder.() -> Unit) {
        data = DataBuilder().apply(func).build()
    }

    fun geo(func: GeoBuilder.() -> Unit) {
        geo = GeoBuilder().apply(func).build()
    }

    fun notifications(func: NotificationsBuilder.() -> Unit) {
        notifications = NotificationsBuilder().apply(func).build()
    }

    fun storage(func: StorageBuilder.() -> Unit) {
        storage = StorageBuilder().apply(func).build()
    }

    fun build() = AmplifyOutputsData(
        analytics = analytics,
        auth = auth,
        data = data,
        geo = geo,
        notifications = notifications,
        storage = storage,
        custom = custom
    )
}

class AnalyticsBuilder {
    var amazonPinpoint: AmplifyOutputsData.Analytics.AmazonPinpoint? = null

    fun amazonPinpoint(func: AmazonPinpointBuilder.() -> Unit) {
        amazonPinpoint = AmazonPinpointBuilder().apply(func).build()
    }

    fun build() = AmplifyOutputsData.Analytics(
        amazonPinpoint = amazonPinpoint
    )
}

class AmazonPinpointBuilder {
    var awsRegion: String = "us-east-1"
    var appId: String = "analytics-app-id"
    fun build() = AmplifyOutputsData.Analytics.AmazonPinpoint(
        awsRegion = awsRegion,
        appId = appId
    )
}

class AuthBuilder {
    var awsRegion: String = "us-east-1"
    var userPoolId: String = "user-pool-id"
    var userPoolClientId: String = "user-pool-client-id"
    var identityPoolId: String? = null
    var passwordPolicy: AmplifyOutputsData.Auth.PasswordPolicy? = null
    var oauth: AmplifyOutputsData.Auth.Oauth? = null
    val standardRequiredAttributes: MutableList<AuthUserAttributeKey> = mutableListOf()
    val usernameAttributes: MutableList<AmplifyOutputsData.Auth.UsernameAttributes> = mutableListOf()
    val userVerificationTypes: MutableList<AmplifyOutputsData.Auth.UserVerificationTypes> = mutableListOf()
    var unauthenticatedIdentitiesEnabled: Boolean = true
    var mfaConfiguration: AmplifyOutputsData.Auth.MfaConfiguration? = null
    val mfaMethods: MutableList<AmplifyOutputsData.Auth.MfaMethods> = mutableListOf()

    fun passwordPolicy(func: PasswordPolicyBuilder.() -> Unit) {
        passwordPolicy = PasswordPolicyBuilder().apply(func).build()
    }

    fun oauth(func: OauthBuilder.() -> Unit) {
        oauth = OauthBuilder().apply(func).build()
    }

    fun build() = AmplifyOutputsData.Auth(
        awsRegion = awsRegion,
        userPoolId = userPoolId,
        userPoolClientId = userPoolClientId,
        identityPoolId = identityPoolId,
        passwordPolicy = passwordPolicy,
        oauth = oauth,
        standardRequiredAttributes = standardRequiredAttributes,
        usernameAttributes = usernameAttributes,
        userVerificationTypes = userVerificationTypes,
        unauthenticatedIdentitiesEnabled = unauthenticatedIdentitiesEnabled,
        mfaConfiguration = mfaConfiguration,
        mfaMethods = mfaMethods
    )
}

class PasswordPolicyBuilder {
    var minLength: Int = 6
    var requireNumbers: Boolean = false
    var requireLowercase: Boolean = false
    var requireUppercase: Boolean = false
    var requireSymbols: Boolean = false

    fun build() = AmplifyOutputsData.Auth.PasswordPolicy(
        minLength = minLength,
        requireNumbers = requireNumbers,
        requireLowercase = requireLowercase,
        requireUppercase = requireUppercase,
        requireSymbols = requireSymbols
    )
}

class OauthBuilder {
    val identityProviders: MutableList<AmplifyOutputsData.Auth.Oauth.IdentityProviders> = mutableListOf()
    var domain: String = "domain"
    val scopes: MutableList<String> = mutableListOf()
    val redirectSignInUri: MutableList<String> = mutableListOf()
    val redirectSignOutUri: MutableList<String> = mutableListOf()
    var responseType: AmplifyOutputsData.Auth.Oauth.ResponseType =
        AmplifyOutputsData.Auth.Oauth.ResponseType.Code

    fun build() = AmplifyOutputsData.Auth.Oauth(
        identityProviders = identityProviders,
        domain = domain,
        scopes = scopes,
        redirectSignInUri = redirectSignInUri,
        redirectSignOutUri = redirectSignOutUri,
        responseType = responseType
    )
}

class DataBuilder {
    var awsRegion: String = "us-east-1"
    var url: String = "https://test.com"
    var apiKey: String? = null
    var defaultAuthorizationType: AmplifyOutputsData.AwsAppsyncAuthorizationType =
        AmplifyOutputsData.AwsAppsyncAuthorizationType.AMAZON_COGNITO_USER_POOLS
    val authorizationTypes: MutableList<AmplifyOutputsData.AwsAppsyncAuthorizationType> =
        mutableListOf(AmplifyOutputsData.AwsAppsyncAuthorizationType.AMAZON_COGNITO_USER_POOLS)

    fun build() = AmplifyOutputsData.Data(
        awsRegion = awsRegion,
        url = url,
        apiKey = apiKey,
        defaultAuthorizationType = defaultAuthorizationType,
        authorizationTypes = authorizationTypes
    )
}

class GeoBuilder {
    var awsRegion: String = "us-east-1"
    var maps: AmplifyOutputsData.Geo.Maps? = null
    var searchIndices: AmplifyOutputsData.Geo.SearchIndices? = null
    var geofenceCollections: AmplifyOutputsData.Geo.GeofenceCollections? = null

    fun maps(func: GeoMapsBuilder.() -> Unit) {
        maps = GeoMapsBuilder().apply(func).build()
    }

    fun searchIndices(func: GeoSearchIndicesBuilder.() -> Unit) {
        searchIndices = GeoSearchIndicesBuilder().apply(func).build()
    }

    fun build() = AmplifyOutputsData.Geo(
        awsRegion = awsRegion,
        maps = maps,
        searchIndices = searchIndices,
        geofenceCollections = geofenceCollections
    )
}

class GeoMapsBuilder {
    val items: MutableMap<String, AmplifyOutputsData.AmazonLocationServiceConfig> = mutableMapOf()
    var default: String = ""

    fun map(name: String, style: String) {
        items += name to AmplifyOutputsData.AmazonLocationServiceConfig(style)
    }

    fun build() = AmplifyOutputsData.Geo.Maps(
        items = items,
        default = default
    )
}

class GeoSearchIndicesBuilder {
    val items: MutableSet<String> = mutableSetOf()
    var default: String = ""

    fun build() = AmplifyOutputsData.Geo.SearchIndices(
        items = items,
        default = default
    )
}

class NotificationsBuilder {
    var awsRegion: String = "us-east-1"
    var amazonPinpointAppId: String = "pinpoint-app-id"
    val channels: MutableList<AmplifyOutputsData.AmazonPinpointChannels> = mutableListOf(
        AmplifyOutputsData.AmazonPinpointChannels.FCM
    )

    fun build() = AmplifyOutputsData.Notifications(
        awsRegion = awsRegion,
        amazonPinpointAppId = amazonPinpointAppId,
        channels = channels
    )
}

class StorageBuilder {
    var awsRegion: String = "us-east-1"
    var bucketName: String = "bucket-name"
    val buckets: MutableList<AmplifyOutputsData.StorageBucket> = mutableListOf()
    fun buckets(func: StorageBucketBuilder.() -> Unit) {
        buckets += StorageBucketBuilder().apply(func).build()
    }

    fun build() = AmplifyOutputsData.Storage(
        awsRegion = awsRegion,
        bucketName = bucketName,
        buckets = buckets
    )
}

class StorageBucketBuilder {
    var name: String = "test-name"
    var awsRegion: String = "us-east-1"
    var bucketName: String = "bucket-name"

    fun build() = AmplifyOutputsData.StorageBucket(
        name = name,
        awsRegion = awsRegion,
        bucketName = bucketName
    )
}
