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

package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.core.configuration.AmplifyOutputsData
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.testutils.configuration.amplifyOutputsData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.assertEquals
import org.json.JSONObject
import org.junit.Test

class AuthConfigurationTest {
    val jsonObject = JSONObject(
        """
            {
                "UserAgent": "aws-amplify-cli/0.1.0",
                "Version": "0.1.0",
                "IdentityManager": {
                    "Default": {}
                },
                "CredentialsProvider": {
                    "CognitoIdentity": {
                        "Default": {
                            "PoolId": "identity-pool-id",
                            "Region": "us-east-1"
                        }
                    }
                },
                "CognitoUserPool": {
                    "Default": {
                        "PoolId": "user-pool-id",
                        "AppClientId": "user-app-id",
                        "Region": "us-east-1"
                    }
                },
                "Auth": {
                    "Default": {
                        "authenticationFlowType": "USER_SRP_AUTH",
                        "OAuth": {
                          "AppClientId": "testAppClientId",
                          "AppClientSecret": "testAppClientSecret",
                          "WebDomain": "webDomain",
                          "Scopes": ["oauth"],
                          "SignInRedirectURI": "http://example.com/signin",
                          "SignOutRedirectURI": "http://example.com/signout"
                        },
                        "socialProviders": [],
                        "usernameAttributes": ["USERNAME", "PHONE_NUMBER"],
                        "signupAttributes": [
                            "EMAIL", "NAME", "BIRTHDATE"
                        ],
                        "passwordProtectionSettings": {
                            "passwordPolicyMinLength": 10,
                            "passwordPolicyCharacters": ["REQUIRES_NUMBERS", "REQUIRES_LOWERCASE"]
                        },
                        "mfaConfiguration": "OFF",
                        "mfaTypes": [
                            "SMS"
                        ],
                        "verificationMechanisms": [
                            "PHONE_NUMBER", "EMAIL"
                        ]
                    }
                }
            }
        """.trimIndent()
    )

    @Test
    fun `parses auth configuration`() {
        val configuration = AuthConfiguration.fromJson(jsonObject)

        configuration.authFlowType shouldBe AuthFlowType.USER_SRP_AUTH
        configuration.usernameAttributes shouldContainExactly listOf(
            UsernameAttribute.Username,
            UsernameAttribute.PhoneNumber
        )
        configuration.signUpAttributes shouldContainExactly listOf(
            AuthUserAttributeKey.email(),
            AuthUserAttributeKey.name(),
            AuthUserAttributeKey.birthdate()
        )
        configuration.passwordProtectionSettings?.shouldNotBeNull()
        configuration.passwordProtectionSettings?.run {
            length shouldBe 10
            requiresUpper.shouldBeFalse()
            requiresLower.shouldBeTrue()
            requiresSpecial.shouldBeFalse()
            requiresNumber.shouldBeTrue()
        }
        configuration.verificationMechanisms shouldContainExactly listOf(
            VerificationMechanism.PhoneNumber,
            VerificationMechanism.Email
        )
    }

    @Test
    fun `signupAttributes is empty if json field is missing`() {
        getAuthConfig().remove("signupAttributes")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.signUpAttributes.shouldBeEmpty()
    }

    @Test
    fun `usernameAttributes is empty if json field is missing`() {
        getAuthConfig().remove("usernameAttributes")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.usernameAttributes.shouldBeEmpty()
    }

    @Test
    fun `verificationMechanisms is empty if json field is missing`() {
        getAuthConfig().remove("verificationMechanisms")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.verificationMechanisms.shouldBeEmpty()
    }

    @Test
    fun `passwordProtectionSettings is null if json field is missing`() {
        getAuthConfig().remove("passwordProtectionSettings")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.passwordProtectionSettings?.shouldBeNull()
    }

    @Test
    fun `password min length defaults to zero if json field is missing`() {
        getPasswordSettings().remove("passwordPolicyMinLength")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.passwordProtectionSettings?.length shouldBe 0
    }

    @Test
    fun `password character requirements are false if json field is missing`() {
        getPasswordSettings().remove("passwordPolicyCharacters")
        val configuration = AuthConfiguration.fromJson(jsonObject)
        configuration.passwordProtectionSettings?.requiresLower?.shouldBeFalse()
        configuration.passwordProtectionSettings?.requiresUpper?.shouldBeFalse()
        configuration.passwordProtectionSettings?.requiresNumber?.shouldBeFalse()
        configuration.passwordProtectionSettings?.requiresSpecial?.shouldBeFalse()
    }

    @Test
    fun `rebuilds valid Gen1 JSON`() {
        // Go JSON -> Config -> JSON -> Config
        val configuration1 = AuthConfiguration.fromJson(jsonObject)
        val newJson = configuration1.toGen1Json()
        val configuration2 = AuthConfiguration.fromJson(newJson)

        // Verify that the two configuration objects are identical
        configuration2 shouldBe configuration1
    }

    @Test
    fun `configures with amplify outputs`() {
        val data = amplifyOutputsData {
            auth {
                awsRegion = "test-region"
                userPoolId = "userpool"
                userPoolClientId = "userpool-client"
                identityPoolId = "identity-pool"
                passwordPolicy {
                    requireLowercase = true
                    requireSymbols = true
                }
                oauth {
                    domain = "https://test.com"
                    identityProviders += AmplifyOutputsData.Auth.Oauth.IdentityProviders.GOOGLE
                    scopes += listOf("myScope", "myScope2")
                    redirectSignInUri += "https://test.com/signin"
                    redirectSignOutUri += "https://test.com/signout"
                    responseType = AmplifyOutputsData.Auth.Oauth.ResponseType.Token
                }
                standardRequiredAttributes += AuthUserAttributeKey.email()
                usernameAttributes += AmplifyOutputsData.Auth.UsernameAttributes.Email
                userVerificationTypes += AmplifyOutputsData.Auth.UserVerificationTypes.Email
                mfaConfiguration = AmplifyOutputsData.Auth.MfaConfiguration.REQUIRED
                mfaMethods += AmplifyOutputsData.Auth.MfaMethods.SMS
            }
        }

        val configuration = AuthConfiguration.from(data)

        configuration.authFlowType shouldBe AuthFlowType.USER_SRP_AUTH
        configuration.userPool.shouldNotBeNull().run {
            region shouldBe "test-region"
            poolId shouldBe "userpool"
            appClient shouldBe "userpool-client"
            appClientSecret.shouldBeNull()
            endpoint.shouldBeNull()
            pinpointAppId.shouldBeNull()
        }
        configuration.oauth.shouldNotBeNull().run {
            appClient shouldBe "userpool-client"
            appSecret.shouldBeNull()
            domain shouldBe "https://test.com"
            scopes shouldContainExactly listOf("myScope", "myScope2")
            signInRedirectURI shouldBe "https://test.com/signin"
            signOutRedirectURI shouldBe "https://test.com/signout"
        }
        configuration.identityPool.shouldNotBeNull().run {
            region shouldBe "test-region"
            poolId shouldBe "identity-pool"
        }
        configuration.passwordProtectionSettings.shouldNotBeNull().run {
            length shouldBe 6
            requiresLower.shouldBeTrue()
            requiresSpecial.shouldBeTrue()
            requiresUpper.shouldBeFalse()
            requiresNumber.shouldBeFalse()
        }
        configuration.signUpAttributes shouldContainExactly listOf(AuthUserAttributeKey.email())
        configuration.usernameAttributes shouldContainExactly listOf(UsernameAttribute.Email)
        configuration.verificationMechanisms shouldContainExactly listOf(VerificationMechanism.Email)
    }

    @Test
    fun `configures with minimal amplify outputs`() {
        val data = amplifyOutputsData {
            auth {
                awsRegion = "test-region"
                userPoolId = "userpool"
                userPoolClientId = "userpool-client"
            }
        }

        val configuration = AuthConfiguration.from(data)

        configuration.authFlowType shouldBe AuthFlowType.USER_SRP_AUTH
        configuration.userPool.shouldNotBeNull().run {
            region shouldBe "test-region"
            poolId shouldBe "userpool"
            appClient shouldBe "userpool-client"
        }

        configuration.oauth.shouldBeNull()
        configuration.passwordProtectionSettings.shouldBeNull()
        configuration.identityPool.shouldBeNull()
    }

    @Test
    fun `throws exception if auth is not configured in amplify outputs`() {
        val data = amplifyOutputsData {
            // do not configure auth
        }

        shouldThrow<ConfigurationException> {
            AuthConfiguration.from(data)
        }
    }

    @Test
    fun `custom endpoint with query fails`() {
        val configJsonObject = JSONObject()
        configJsonObject.put("PoolId", "TestUserPool")
        configJsonObject.put("AppClientId", "0000000000")
        configJsonObject.put("Region", "test-region")
        val invalidEndpoint = "fsjjdh.com?q=id"
        configJsonObject.put("Endpoint", invalidEndpoint)
        val expectedErrorMessage = "Invalid endpoint value $invalidEndpoint. Expected fully qualified hostname with " +
            "no scheme, no path and no query"

        shouldThrowWithMessage<Exception>(expectedErrorMessage) {
            UserPoolConfiguration.fromJson(configJsonObject).build()
        }
    }

    @Test
    fun `custom endpoint with path fails`() {
        val configJsonObject = JSONObject()
        configJsonObject.put("PoolId", "TestUserPool")
        configJsonObject.put("AppClientId", "0000000000")
        configJsonObject.put("Region", "test-region")
        val invalidEndpoint = "fsjjdh.com/id"
        configJsonObject.put("Endpoint", invalidEndpoint)
        val expectedErrorMessage = "Invalid endpoint value $invalidEndpoint. Expected fully qualified hostname with " +
            "no scheme, no path and no query"

        shouldThrowWithMessage<Exception>(expectedErrorMessage) {
            UserPoolConfiguration.fromJson(configJsonObject).build()
        }
    }

    @Test
    fun `custom endpoint with scheme fails`() {
        val configJsonObject = JSONObject()
        configJsonObject.put("PoolId", "TestUserPool")
        configJsonObject.put("AppClientId", "0000000000")
        configJsonObject.put("Region", "test-region")

        val invalidEndpoint = "https://fsjjdh.com"
        configJsonObject.put("Endpoint", invalidEndpoint)
        val expectedErrorMessage = "Invalid endpoint value $invalidEndpoint. Expected fully qualified hostname with " +
            "no scheme, no path and no query"

        shouldThrowWithMessage<Exception>(expectedErrorMessage) {
            UserPoolConfiguration.fromJson(configJsonObject).build()
        }
    }

    @Test
    fun `custom endpoint with no query,path, scheme success`() {
        val configJsonObject = JSONObject()
        val poolId = "TestUserPool"
        val region = "test-region"
        val appClientId = "0000000000"
        val endpoint = "fsjjdh.com"
        configJsonObject.put("PoolId", poolId)
        configJsonObject.put("AppClientId", appClientId)
        configJsonObject.put("Region", region)
        configJsonObject.put("Endpoint", endpoint)

        val userPool = UserPoolConfiguration.fromJson(configJsonObject).build()
        assertEquals(userPool.region, region, "Regions do not match expected")
        assertEquals(userPool.poolId, poolId, "Pool id do not match expected")
        assertEquals(userPool.appClient, appClientId, "AppClientId do not match expected")
        assertEquals(userPool.endpoint, "https://$endpoint", "Endpoint do not match expected")
    }

    @Test
    fun `validate auth flow type defaults to user_srp_auth for invalid types`() {
        val configJsonObject = JSONObject()
        val configAuthJsonObject = JSONObject()
        val configAuthDefaultJsonObject = JSONObject()
        configAuthDefaultJsonObject.put("authenticationFlowType", "INVALID_FLOW_TYPE")
        configAuthJsonObject.put("Default", configAuthDefaultJsonObject)
        configJsonObject.put("Auth", configAuthJsonObject)
        val configuration = AuthConfiguration.fromJson(configJsonObject)
        assertEquals(configuration.authFlowType, AuthFlowType.USER_SRP_AUTH, "Auth flow types do not match expected")
    }

    @Test
    fun `validate auth flow type success`() {
        val configJsonObject = JSONObject()
        val configAuthJsonObject = JSONObject()
        val configAuthDefaultJsonObject = JSONObject()
        configAuthDefaultJsonObject.put("authenticationFlowType", "USER_PASSWORD_AUTH")
        configAuthJsonObject.put("Default", configAuthDefaultJsonObject)
        configJsonObject.put("Auth", configAuthJsonObject)
        val configuration = AuthConfiguration.fromJson(configJsonObject)
        assertEquals(
            configuration.authFlowType,
            AuthFlowType.USER_PASSWORD_AUTH,
            "Auth flow types do not match expected"
        )
    }

    @Test
    fun `selects non-HTTP URI from multiple redirect URIs`() {
        val data = amplifyOutputsData {
            auth {
                awsRegion = "test-region"
                userPoolId = "userpool"
                userPoolClientId = "userpool-client"
                oauth {
                    redirectSignInUri += "https://test.com/signin"
                    redirectSignInUri += "myapp://signin"
                    redirectSignOutUri += "myapp://signout"
                    redirectSignOutUri += "https://test.com/signout"
                }
            }
        }

        val configuration = AuthConfiguration.from(data)
        configuration.oauth.shouldNotBeNull().run {
            signInRedirectURI shouldBe "myapp://signin"
            signOutRedirectURI shouldBe "myapp://signout"
        }
    }

    @Test
    fun `uses first URI when all are HTTP or HTTPS`() {
        val data = amplifyOutputsData {
            auth {
                awsRegion = "test-region"
                userPoolId = "userpool"
                userPoolClientId = "userpool-client"
                oauth {
                    scopes += listOf("openid")
                    redirectSignInUri += "https://test.com/signin"
                    redirectSignInUri += "http://localhost/callback"
                    redirectSignOutUri += "https://test.com/signout"
                    redirectSignOutUri += "http://localhost/logout"
                }
            }
        }

        val configuration = AuthConfiguration.from(data)
        configuration.oauth.shouldNotBeNull().run {
            signInRedirectURI shouldBe "https://test.com/signin"
            signOutRedirectURI shouldBe "https://test.com/signout"
        }
    }

    private fun getAuthConfig() = jsonObject.getJSONObject("Auth").getJSONObject("Default")
    private fun getPasswordSettings() = jsonObject.getJSONObject("Auth").getJSONObject("Default")
        .getJSONObject("passwordProtectionSettings")
}
