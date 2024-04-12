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
import com.amplifyframework.testutils.configuration.amplifyOutputsData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class AuthConfigurationTest {
    @Test
    fun `configures with amplify outputs`() {
        val data = amplifyOutputsData {
            auth {
                awsRegion = "test-region"
                userPoolId = "userpool"
                userPoolClientId = "userpool-client"
                identityPoolId = "identity-pool"
                authenticationFlowType = AmplifyOutputsData.Auth.AuthenticationFlowType.CUSTOM_AUTH
                passwordPolicy {
                    requireLowercase = true
                    requireSymbols = true
                }
                oauth {
                    cognitoDomain = "https://test.com"
                    identityProviders += AmplifyOutputsData.Auth.Oauth.IdentityProviders.GOOGLE
                    scopes += listOf("myScope", "myScope2")
                    redirectSignInUri += "https://test.com/signin"
                    redirectSignOutUri += "https://test.com/signout"
                    responseType = AmplifyOutputsData.Auth.Oauth.ResponseType.TOKEN
                }
                standardRequiredAttributes += AuthUserAttributeKey.email()
                usernameAttributes += AmplifyOutputsData.Auth.UsernameAttributes.EMAIL
                userVerificationTypes += AmplifyOutputsData.Auth.UserVerificationTypes.EMAIL
                mfaConfiguration = AmplifyOutputsData.Auth.MfaConfiguration.REQUIRED
                mfaMethods += AmplifyOutputsData.Auth.MfaMethods.SMS
            }
        }

        val configuration = AuthConfiguration.from(data)

        configuration.authFlowType shouldBe AuthFlowType.CUSTOM_AUTH
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
                authenticationFlowType = AmplifyOutputsData.Auth.AuthenticationFlowType.CUSTOM_AUTH
            }
        }

        val configuration = AuthConfiguration.from(data)

        configuration.authFlowType shouldBe AuthFlowType.CUSTOM_AUTH
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
    fun `uses custom oauth domain if specified`() {
        val data = amplifyOutputsData {
            auth {
                oauth {
                    cognitoDomain = "cognito"
                    customDomain = "custom"
                }
            }
        }

        val configuration = AuthConfiguration.from(data)

        configuration.oauth?.domain shouldBe "custom"
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
}
