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
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.core.configuration.AmplifyOutputsData.AmazonPinpointChannels
import com.amplifyframework.core.configuration.AmplifyOutputsData.Auth.AuthenticationFlowType
import com.amplifyframework.core.configuration.AmplifyOutputsData.Auth.MfaConfiguration
import com.amplifyframework.core.configuration.AmplifyOutputsData.Auth.MfaMethods
import com.amplifyframework.core.configuration.AmplifyOutputsData.Auth.Oauth.IdentityProviders
import com.amplifyframework.core.configuration.AmplifyOutputsData.Auth.Oauth.ResponseType
import com.amplifyframework.core.configuration.AmplifyOutputsData.Auth.UserVerificationTypes
import com.amplifyframework.core.configuration.AmplifyOutputsData.Auth.UsernameAttributes
import com.amplifyframework.core.configuration.AmplifyOutputsData.AwsAppsyncAuthorizationType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject

// Note: All inner types must also be annotated with a non-public marker to avoid flagging the API validator.
// See https://github.com/Kotlin/binary-compatibility-validator/issues/91 for status of feature request to remove this
// requirement
@InternalAmplifyApi
interface AmplifyOutputsData {
    val version: String
    val analytics: Analytics?
    val auth: Auth?
    val data: Data?
    val geo: Geo?
    val notifications: Notifications?
    val storage: Storage?
    val custom: JsonObject?

    @InternalAmplifyApi
    interface Analytics {
        val amazonPinpoint: AmazonPinpoint?

        @InternalAmplifyApi
        interface AmazonPinpoint {
            val awsRegion: String
            val appId: String
        }
    }

    @InternalAmplifyApi
    interface Auth {
        val awsRegion: String
        val authenticationFlowType: AuthenticationFlowType
        val userPoolId: String
        val userPoolClientId: String
        val identityPoolId: String?
        val passwordPolicy: PasswordPolicy?
        val oauth: Oauth?
        val standardRequiredAttributes: List<AuthUserAttributeKey>
        val usernameAttributes: List<UsernameAttributes>
        val userVerificationTypes: List<UserVerificationTypes>
        val unauthenticatedIdentitiesEnabled: Boolean
        val mfaConfiguration: MfaConfiguration?
        val mfaMethods: List<MfaMethods>

        @InternalAmplifyApi
        interface PasswordPolicy {
            val minLength: Int?
            val requireNumbers: Boolean?
            val requireLowercase: Boolean?
            val requireUppercase: Boolean?
            val requireSymbols: Boolean?
        }

        @InternalAmplifyApi
        interface Oauth {
            val identityProviders: List<IdentityProviders>
            val cognitoDomain: String
            val customDomain: String?
            val scopes: List<String>
            val redirectSignInUri: List<String>
            val redirectSignOutUri: List<String>
            val responseType: ResponseType

            @InternalAmplifyApi
            enum class IdentityProviders {
                GOOGLE, FACEBOOK, LOGIN_WITH_AMAZON, SIGN_IN_WITH_APPLE
            }

            @InternalAmplifyApi
            @Serializable
            enum class ResponseType {
                @SerialName("code")
                Code,

                @SerialName("token")
                Token
            }
        }

        @InternalAmplifyApi
        enum class AuthenticationFlowType { USER_SRP_AUTH, CUSTOM_AUTH }

        @InternalAmplifyApi
        @Serializable
        enum class UsernameAttributes {
            @SerialName("username")
            Username,

            @SerialName("email")
            Email,

            @SerialName("phone_number")
            PhoneNumber
        }

        @InternalAmplifyApi
        @Serializable
        enum class UserVerificationTypes {
            @SerialName("email")
            Email,

            @SerialName("phone_number")
            PhoneNumber
        }

        @InternalAmplifyApi
        enum class MfaConfiguration { NONE, OPTIONAL, REQUIRED }

        @InternalAmplifyApi
        enum class MfaMethods { SMS, TOTP }
    }

    @InternalAmplifyApi
    interface Data {
        val awsRegion: String
        val url: String
        val apiKey: String?
        val defaultAuthorizationType: AwsAppsyncAuthorizationType
        val authorizationTypes: List<AwsAppsyncAuthorizationType>
    }

    @InternalAmplifyApi
    interface Geo {
        val awsRegion: String
        val maps: Maps?
        val searchIndices: SearchIndices?
        val geofenceCollections: GeofenceCollections?

        @InternalAmplifyApi
        interface Maps {
            val items: Map<String, AmazonLocationServiceConfig>
            val default: String
        }

        @InternalAmplifyApi
        interface SearchIndices {
            val items: Set<String>
            val default: String
        }

        @InternalAmplifyApi
        interface GeofenceCollections {
            val items: Set<String>
            val default: String
        }
    }

    @InternalAmplifyApi
    interface Notifications {
        val awsRegion: String
        val amazonPinpointAppId: String
        val channels: List<AmazonPinpointChannels>
    }

    @InternalAmplifyApi
    interface Storage {
        val awsRegion: String
        val bucketName: String
    }

    @InternalAmplifyApi
    interface AmazonLocationServiceConfig {
        val style: String
    }

    @InternalAmplifyApi
    enum class AwsAppsyncAuthorizationType {
        AMAZON_COGNITO_USER_POOLS,
        API_KEY,
        AWS_IAM,
        AWS_LAMBDA,
        OPENID_CONNECT
    }

    @InternalAmplifyApi
    enum class AmazonPinpointChannels {
        IN_APP_MESSAGING,
        FCM,
        APNS,
        EMAIL,
        SMS
    }

    @InternalAmplifyApi
    companion object {
        @JvmStatic // JvmStatic because the Amplify main class implementation is currently in Java
        @InternalAmplifyApi
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
                return json.decodeFromString<AmplifyOutputsDataImpl>(content)
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

/**
 * Concrete type for the [AmplifyOutputsData] API
 */
@Serializable
internal data class AmplifyOutputsDataImpl(
    override val version: String,
    override val analytics: Analytics?,
    override val auth: Auth?,
    override val data: Data?,
    override val geo: Geo?,
    override val notifications: Notifications?,
    override val storage: Storage?,
    override val custom: JsonObject?
) : AmplifyOutputsData {
    @Serializable
    data class Analytics(
        override val amazonPinpoint: AmazonPinpoint?
    ) : AmplifyOutputsData.Analytics {
        @Serializable
        data class AmazonPinpoint(
            override val awsRegion: String,
            override val appId: String
        ) : AmplifyOutputsData.Analytics.AmazonPinpoint
    }

    @Serializable
    data class Auth(
        override val awsRegion: String,
        override val authenticationFlowType: AuthenticationFlowType = AuthenticationFlowType.USER_SRP_AUTH,
        override val userPoolId: String,
        override val userPoolClientId: String,
        override val identityPoolId: String?,
        override val passwordPolicy: PasswordPolicy?,
        override val oauth: Oauth?,
        override val standardRequiredAttributes: List<AuthUserAttributeKey> = emptyList(),
        override val usernameAttributes: List<UsernameAttributes> = emptyList(),
        override val userVerificationTypes: List<UserVerificationTypes> = emptyList(),
        override val unauthenticatedIdentitiesEnabled: Boolean = true,
        override val mfaConfiguration: MfaConfiguration?,
        override val mfaMethods: List<MfaMethods> = emptyList()
    ) : AmplifyOutputsData.Auth {
        @Serializable
        data class PasswordPolicy(
            override val minLength: Int?,
            override val requireNumbers: Boolean?,
            override val requireLowercase: Boolean?,
            override val requireUppercase: Boolean?,
            override val requireSymbols: Boolean?
        ) : AmplifyOutputsData.Auth.PasswordPolicy

        @Serializable
        data class Oauth(
            override val identityProviders: List<IdentityProviders>,
            override val cognitoDomain: String,
            override val customDomain: String?,
            override val scopes: List<String>,
            override val redirectSignInUri: List<String>,
            override val redirectSignOutUri: List<String>,
            override val responseType: ResponseType
        ) : AmplifyOutputsData.Auth.Oauth
    }

    @Serializable
    data class Data(
        override val awsRegion: String,
        override val url: String,
        override val apiKey: String?,
        override val defaultAuthorizationType: AwsAppsyncAuthorizationType,
        override val authorizationTypes: List<AwsAppsyncAuthorizationType>
    ) : AmplifyOutputsData.Data

    @Serializable
    data class Geo(
        override val awsRegion: String,
        override val maps: Maps?,
        override val searchIndices: SearchIndices?,
        override val geofenceCollections: GeofenceCollections?
    ) : AmplifyOutputsData.Geo {
        @Serializable
        data class Maps(
            override val items: Map<String, AmazonLocationServiceConfig>,
            override val default: String
        ) : AmplifyOutputsData.Geo.Maps

        @Serializable
        data class SearchIndices(
            override val items: Set<String>,
            override val default: String
        ) : AmplifyOutputsData.Geo.SearchIndices

        @Serializable
        data class GeofenceCollections(
            override val items: Set<String>,
            override val default: String
        ) : AmplifyOutputsData.Geo.GeofenceCollections
    }

    @Serializable
    data class Notifications(
        override val awsRegion: String,
        override val amazonPinpointAppId: String,
        override val channels: List<AmazonPinpointChannels>
    ) : AmplifyOutputsData.Notifications

    @Serializable
    data class Storage(
        override val awsRegion: String,
        override val bucketName: String
    ) : AmplifyOutputsData.Storage

    @Serializable
    data class AmazonLocationServiceConfig(
        override val style: String
    ) : AmplifyOutputsData.AmazonLocationServiceConfig
}
