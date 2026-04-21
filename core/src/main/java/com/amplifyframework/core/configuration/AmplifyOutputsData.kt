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

@file:UseSerializers(AuthUserAttributeKeySerializer::class)

package com.amplifyframework.core.configuration

import android.content.Context
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthUserAttributeKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject

/**
 * Concrete representation of the data provided via the [AmplifyOutputs] API
 */
@Serializable
data class AmplifyOutputsData(
    val analytics: Analytics? = null,
    val auth: Auth? = null,
    val data: Data? = null,
    val geo: Geo? = null,
    val notifications: Notifications? = null,
    val storage: Storage? = null,
    val custom: JsonObject? = null
) {
    // The schema version we are supporting
    val version = "1.1"

    @Serializable
    data class Analytics(
        val amazonPinpoint: AmazonPinpoint? = null
    ) {
        @Serializable
        data class AmazonPinpoint(
            val awsRegion: String,
            val appId: String
        )
    }

    @Serializable
    data class Auth(
        val awsRegion: String,
        val userPoolId: String,
        val userPoolClientId: String,
        val identityPoolId: String? = null,
        val passwordPolicy: PasswordPolicy? = null,
        val oauth: Oauth? = null,
        val standardRequiredAttributes: List<AuthUserAttributeKey> = emptyList(),
        val usernameAttributes: List<UsernameAttributes> = emptyList(),
        val userVerificationTypes: List<UserVerificationTypes> = emptyList(),
        val unauthenticatedIdentitiesEnabled: Boolean = true,
        val mfaConfiguration: MfaConfiguration? = null,
        val mfaMethods: List<MfaMethods> = emptyList()
    ) {
        @Serializable
        data class PasswordPolicy(
            val minLength: Int,
            val requireNumbers: Boolean,
            val requireLowercase: Boolean,
            val requireUppercase: Boolean,
            val requireSymbols: Boolean
        )

        @Serializable
        data class Oauth(
            val identityProviders: List<IdentityProviders>,
            val domain: String,
            val scopes: List<String>,
            val redirectSignInUri: List<String>,
            val redirectSignOutUri: List<String>,
            val responseType: ResponseType
        ) {
            enum class IdentityProviders {
                GOOGLE,
                FACEBOOK,
                LOGIN_WITH_AMAZON,
                SIGN_IN_WITH_APPLE
            }

            @Serializable
            enum class ResponseType {
                @SerialName("code")
                Code,

                @SerialName("token")
                Token
            }
        }

        @Serializable
        enum class UsernameAttributes {
            @SerialName("username")
            Username,

            @SerialName("email")
            Email,

            @SerialName("phone_number")
            PhoneNumber
        }

        @Serializable
        enum class UserVerificationTypes {
            @SerialName("email")
            Email,

            @SerialName("phone_number")
            PhoneNumber
        }

        enum class MfaConfiguration { NONE, OPTIONAL, REQUIRED }

        enum class MfaMethods { SMS, TOTP }
    }

    @Serializable
    data class Data(
        val awsRegion: String,
        val url: String,
        val defaultAuthorizationType: AwsAppsyncAuthorizationType,
        val authorizationTypes: List<AwsAppsyncAuthorizationType>,
        val apiKey: String? = null
    )

    @Serializable
    data class Geo(
        val awsRegion: String,
        val maps: Maps? = null,
        val searchIndices: SearchIndices? = null,
        val geofenceCollections: GeofenceCollections? = null
    ) {
        @Serializable
        data class Maps(
            val items: Map<String, AmazonLocationServiceConfig>,
            val default: String
        )

        @Serializable
        data class SearchIndices(
            val items: Set<String>,
            val default: String
        )

        @Serializable
        data class GeofenceCollections(
            val items: Set<String>,
            val default: String
        )
    }

    @Serializable
    data class Notifications(
        val awsRegion: String,
        val amazonPinpointAppId: String,
        val channels: List<AmazonPinpointChannels>
    )

    @Serializable
    data class Storage(
        val awsRegion: String,
        val bucketName: String,
        val buckets: List<StorageBucket> = emptyList()
    )

    @Serializable
    data class StorageBucket(
        val name: String,
        val awsRegion: String,
        val bucketName: String
    )

    @Serializable
    data class AmazonLocationServiceConfig(
        val style: String
    )

    enum class AwsAppsyncAuthorizationType {
        AMAZON_COGNITO_USER_POOLS,
        API_KEY,
        AWS_IAM,
        AWS_LAMBDA,
        OPENID_CONNECT
    }

    enum class AmazonPinpointChannels {
        IN_APP_MESSAGING,
        FCM,
        APNS,
        EMAIL,
        SMS
    }

    companion object {
        @JvmStatic // JvmStatic because the Amplify main class implementation is currently in Java
        fun deserialize(context: Context, amplifyOutputs: AmplifyOutputs): AmplifyOutputsData {
            val content = when (amplifyOutputs) {
                is AmplifyOutputsResource -> amplifyOutputs.readContent(context)
                is AmplifyOutputsString -> amplifyOutputs.json
            }

            return deserialize(content)
        }

        @OptIn(ExperimentalSerializationApi::class)
        internal fun deserialize(content: String): AmplifyOutputsData {
            val json = Json {
                ignoreUnknownKeys = true
                namingStrategy = JsonNamingStrategy.SnakeCase
                explicitNulls = false
            }
            try {
                return json.decodeFromString<AmplifyOutputsData>(content)
            } catch (e: Exception) {
                throw AmplifyException(
                    "Could not decode AmplifyOutputs",
                    e,
                    "Ensure the AmplifyOutputs data is correct"
                )
            }
        }

        private fun AmplifyOutputsResource.readContent(context: Context) = context.resources.openRawResource(resourceId)
            .bufferedReader().use { it.readText() }
    }
}
