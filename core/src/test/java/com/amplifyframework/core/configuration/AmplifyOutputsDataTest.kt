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

package com.amplifyframework.core.configuration

import com.amplifyframework.auth.AuthUserAttributeKey
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import org.junit.Test

class AmplifyOutputsDataTest {

    @Test
    fun `parses version string`() {
        val json = createJson(
            Keys.version to "2.4"
        )
        val outputs = AmplifyOutputsData.deserialize(json)
        outputs.version shouldBe "2.4"
    }

    @Test
    fun `parses Analytics configuration`() {
        val json = createJson(
            Keys.analytics to mapOf(
                Keys.region to "us-east-1",
                Keys.appId to "testAppId"
            )
        )

        val outputs = AmplifyOutputsData.deserialize(json)

        outputs.analytics.shouldNotBeNull()
        outputs.analytics?.run {
            awsRegion shouldBe "us-east-1"
            appId shouldBe "testAppId"
        }
    }

    @Test
    fun `parses Auth configuration`() {
        val json = createJson(
            Keys.auth to mapOf(
                Keys.region to "us-east-1",
                Keys.userPoolId to "user-pool",
                Keys.userPoolClientId to "user-pool-client",
                Keys.identityPoolId to "identity-pool",
                Keys.passwordPolicy to mapOf(
                    Keys.passwordMinLength to 10,
                    Keys.passwordNumbers to true,
                    Keys.passwordSymbols to true,
                    Keys.passwordLower to true,
                    Keys.passwordUpper to true
                ),
                Keys.oauth to mapOf(
                    Keys.oauthIdentityProviders to listOf(
                        AmplifyOutputsData.Auth.Oauth.IdentityProviders.FACEBOOK.name,
                        AmplifyOutputsData.Auth.Oauth.IdentityProviders.GOOGLE.name
                    ),
                    Keys.oauthDomain to "https://oauth.com",
                    Keys.oauthScopes to listOf("scope1", "scope2"),
                    Keys.oauthSignInUri to listOf("https://oauth.com/signin"),
                    Keys.oauthSignOutUri to listOf("https://oauth.com/signout"),
                    Keys.oauthResponseType to "code"
                ),
                Keys.requiredAttributes to listOf(
                    AuthUserAttributeKey.email().keyString,
                    AuthUserAttributeKey.gender().keyString
                ),
                Keys.usernameAttributes to listOf(
                    AmplifyOutputsData.Auth.UsernameAttributes.Username.name,
                    AmplifyOutputsData.Auth.UsernameAttributes.Email.name
                ),
                Keys.userVerificationTypes to listOf(
                    AmplifyOutputsData.Auth.UserVerificationTypes.Email.name,
                    AmplifyOutputsData.Auth.UserVerificationTypes.PhoneNumber.name
                ),
                Keys.unauthenticatedIdentitiesEnabled to true,
                Keys.mfaConfiguration to AmplifyOutputsData.Auth.MfaConfiguration.OPTIONAL.name,
                Keys.mfaMethods to listOf(
                    AmplifyOutputsData.Auth.MfaMethods.SMS.name,
                    AmplifyOutputsData.Auth.MfaMethods.TOTP.name
                )
            )
        )

        val outputs = AmplifyOutputsData.deserialize(json)

        outputs.auth.shouldNotBeNull()
        outputs.auth?.run {
            awsRegion shouldBe "us-east-1"
            userPoolId shouldBe "user-pool"
            userPoolClientId shouldBe "user-pool-client"
            identityPoolId shouldBe "identity-pool"
            passwordPolicy!!.run {
                minLength shouldBe 10
                requireLowercase?.shouldBeTrue()
                requireUppercase?.shouldBeTrue()
                requireNumbers?.shouldBeTrue()
                requireSymbols?.shouldBeTrue()
            }
            oauth!!.run {
                identityProviders shouldContainExactly listOf(
                    AmplifyOutputsData.Auth.Oauth.IdentityProviders.FACEBOOK,
                    AmplifyOutputsData.Auth.Oauth.IdentityProviders.GOOGLE
                )
                domain shouldBe "https://oauth.com"
                scopes shouldContainExactly listOf("scope1", "scope2")
                redirectSignInUri shouldContainExactly listOf("https://oauth.com/signin")
                redirectSignOutUri shouldContainExactly listOf("https://oauth.com/signout")
                responseType shouldBe AmplifyOutputsData.Auth.Oauth.ResponseType.Code
            }
            standardRequiredAttributes shouldContainExactly listOf(
                AuthUserAttributeKey.email(),
                AuthUserAttributeKey.gender()
            )
            usernameAttributes shouldContainExactly listOf(
                AmplifyOutputsData.Auth.UsernameAttributes.Username,
                AmplifyOutputsData.Auth.UsernameAttributes.Email
            )
            userVerificationTypes shouldContainExactly listOf(
                AmplifyOutputsData.Auth.UserVerificationTypes.Email,
                AmplifyOutputsData.Auth.UserVerificationTypes.PhoneNumber
            )
            unauthenticatedIdentitiesEnabled.shouldBeTrue()
            mfaConfiguration shouldBe AmplifyOutputsData.Auth.MfaConfiguration.OPTIONAL
            mfaMethods shouldContainExactly listOf(
                AmplifyOutputsData.Auth.MfaMethods.SMS,
                AmplifyOutputsData.Auth.MfaMethods.TOTP
            )
        }
    }

    @Test
    fun `parses Data configuration`() {
        val json = createJson(
            Keys.data to mapOf(
                Keys.region to "us_east_1",
                Keys.url to "http://www.test.com",
                Keys.apiKey to "myApiKey",
                Keys.defaultAuthType to AmplifyOutputsData.AwsAppsyncAuthorizationType.API_KEY.name,
                Keys.authTypes to listOf(
                    AmplifyOutputsData.AwsAppsyncAuthorizationType.AWS_IAM.name,
                    AmplifyOutputsData.AwsAppsyncAuthorizationType.API_KEY.name
                )
            )
        )

        val outputs = AmplifyOutputsData.deserialize(json)

        outputs.data.shouldNotBeNull()
        outputs.data?.run {
            awsRegion shouldBe "us_east_1"
            url shouldBe "http://www.test.com"
            apiKey shouldBe "myApiKey"
            defaultAuthorizationType shouldBe AmplifyOutputsData.AwsAppsyncAuthorizationType.API_KEY
            authorizationTypes shouldContainExactly listOf(
                AmplifyOutputsData.AwsAppsyncAuthorizationType.AWS_IAM,
                AmplifyOutputsData.AwsAppsyncAuthorizationType.API_KEY
            )
        }
    }

    @Test
    fun `parses Geo configuration`() {
        val json = createJson(
            Keys.geo to mapOf(
                Keys.region to "us-east-1",
                Keys.maps to mapOf(
                    Keys.items to mapOf(
                        "default_map" to mapOf(Keys.style to "default_style")
                    ),
                    Keys.default to "default_map"
                ),
                Keys.searchIndices to mapOf(
                    Keys.items to listOf("some_search", "default_search"),
                    Keys.default to "default_search"
                ),
                Keys.geofenceCollections to mapOf(
                    Keys.items to listOf("default_geofence"),
                    Keys.default to "default_geofence"
                )
            )
        )

        val outputs = AmplifyOutputsData.deserialize(json)

        outputs.geo.shouldNotBeNull()
        outputs.geo?.run {
            awsRegion shouldBe "us-east-1"

            maps?.default shouldBe "default_map"
            maps?.items?.get(maps?.default)?.style shouldBe "default_style"

            searchIndices?.items shouldContainExactly listOf("some_search", "default_search")
            searchIndices?.default shouldBe "default_search"

            geofenceCollections?.items shouldContainExactly listOf("default_geofence")
            geofenceCollections?.default shouldBe "default_geofence"
        }
    }

    @Test
    fun `parses notifications configuration`() {
        val json = createJson(
            Keys.notifications to mapOf(
                Keys.region to "us-east-1",
                Keys.pinpointApp to "myPinpointApp",
                Keys.channels to listOf("FCM")
            )
        )

        val outputs = AmplifyOutputsData.deserialize(json)

        outputs.notifications.shouldNotBeNull()
        outputs.notifications?.run {
            awsRegion shouldBe "us-east-1"
            amazonPinpointAppId shouldBe "myPinpointApp"
            channels shouldContainExactly listOf(AmplifyOutputsData.AmazonPinpointChannels.FCM)
        }
    }

    @Test
    fun `parses storage configuration`() {
        val json = createJson(
            Keys.storage to mapOf(
                Keys.region to "us-east-1",
                Keys.bucket to "myBucket"
            )
        )

        val outputs = AmplifyOutputsData.deserialize(json)

        outputs.storage.shouldNotBeNull()
        outputs.storage?.run {
            awsRegion shouldBe "us-east-1"
            bucketName shouldBe "myBucket"
        }
    }

    @Test
    fun `parses custom`() {
        val json = createJson(
            Keys.custom to mapOf(
                "foo" to "bar"
            )
        )

        val outputs = AmplifyOutputsData.deserialize(json)

        outputs.custom?.get("foo")?.jsonPrimitive?.content shouldBe "bar"
    }

    private fun createJson(
        vararg entries: Pair<String, Any>
    ): String {
        val data = mutableMapOf(*entries).apply {
            put("\$schema", "./schema.json")
            if (!containsKey("version")) {
                put("version", "1")
            }
        }

        return JSONObject(data as Map<*, *>).toString()
    }

    object Keys {
        const val version = "version"
        const val region = "aws_region"
        const val items = "items"
        const val default = "default"

        // Analytics
        const val analytics = "analytics"
        const val appId = "app_id"

        // Auth
        const val auth = "auth"
        const val userPoolId = "user_pool_id"
        const val userPoolClientId = "user_pool_client_id"
        const val identityPoolId = "identity_pool_id"
        const val passwordPolicy = "password_policy"
        const val passwordMinLength = "min_length"
        const val passwordNumbers = "require_numbers"
        const val passwordSymbols = "require_symbols"
        const val passwordUpper = "require_uppercase"
        const val passwordLower = "require_lowercase"
        const val oauth = "oauth"
        const val oauthIdentityProviders = "identity_providers"
        const val oauthDomain = "domain"
        const val oauthScopes = "scopes"
        const val oauthSignInUri = "redirect_sign_in_uri"
        const val oauthSignOutUri = "redirect_sign_out_uri"
        const val oauthResponseType = "response_type"
        const val requiredAttributes = "standard_required_attributes"
        const val usernameAttributes = "username_attributes"
        const val userVerificationTypes = "user_verification_types"
        const val unauthenticatedIdentitiesEnabled = "unauthenticated_identities_enabled"
        const val mfaConfiguration = "mfa_configuration"
        const val mfaMethods = "mfa_methods"

        // Data
        const val data = "data"
        const val url = "url"
        const val apiKey = "api_key"
        const val defaultAuthType = "default_authorization_type"
        const val authTypes = "authorization_types"

        // Geo
        const val geo = "geo"
        const val maps = "maps"
        const val style = "style"
        const val searchIndices = "search_indices"
        const val geofenceCollections = "geofence_collections"

        // Notifications
        const val notifications = "notifications"
        const val pinpointApp = "amazon_pinpoint_app_id"
        const val channels = "channels"

        // Storage
        const val storage = "storage"
        const val bucket = "bucket_name"

        // Custom
        const val custom = "custom"
    }
}
