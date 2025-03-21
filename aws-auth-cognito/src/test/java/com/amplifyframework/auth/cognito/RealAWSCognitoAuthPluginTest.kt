/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.getUser
import aws.sdk.kotlin.services.cognitoidentityprovider.model.EmailMfaSettingsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SetUserMfaPreferenceRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SetUserMfaPreferenceResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SmsMfaSettingsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SoftwareTokenMfaSettingsType
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.testutils.await
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealAWSCognitoAuthPluginTest {

    private val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IkpvaG4gRG9lIiwiZXhwIjoxNTE2Mj" +
        "M5MDIyfQ.e4RpZTfAb3oXkfq3IwHtR_8Zhn0U1JDV7McZPlBXyhw"

    private var logger = mockk<Logger>(relaxed = true)
    private val appClientId = "app Client Id"
    private val appClientSecret = "app Client Secret"
    private var authConfiguration = mockk<AuthConfiguration> {
        every { userPool } returns UserPoolConfiguration.invoke {
            this.appClientId = this@RealAWSCognitoAuthPluginTest.appClientId
            this.appClientSecret = this@RealAWSCognitoAuthPluginTest.appClientSecret
            this.pinpointAppId = null
        }
    }

    private val credentials = AmplifyCredential.UserPool(
        SignedInData(
            "userId",
            "username",
            Date(0),
            SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
            CognitoUserPoolTokens(dummyToken, dummyToken, dummyToken, 120L)
        )
    )

    private val mockCognitoIPClient = mockk<CognitoIdentityProviderClient>()
    private var authService = mockk<AWSCognitoAuthService> {
        every { cognitoIdentityProviderClient } returns mockCognitoIPClient
    }

    private val expectedEndpointId = "test-endpoint-id"

    private var authEnvironment = mockk<AuthEnvironment> {
        every { context } returns mockk()
        every { configuration } returns authConfiguration
        every { logger } returns this@RealAWSCognitoAuthPluginTest.logger
        every { cognitoAuthService } returns authService
        every { getPinpointEndpointId() } returns expectedEndpointId
    }

    private var authStateMachine = mockk<AuthStateMachine>(relaxed = true)

    private lateinit var plugin: RealAWSCognitoAuthPlugin

    @Before
    fun setup() {
        plugin = RealAWSCognitoAuthPlugin(
            authConfiguration,
            authEnvironment,
            authStateMachine,
            logger
        )

        mockkStatic("com.amplifyframework.auth.cognito.AWSCognitoAuthSessionKt")
        every { any<AmplifyCredential>().isValid() } returns true

        // set up user pool
        coEvery { authConfiguration.userPool } returns UserPoolConfiguration.invoke {
            appClientId = "app Client Id"
            appClientSecret = "app Client Secret"
        }

        coEvery { authEnvironment.getUserContextData(any()) } returns null

        // set up SRP helper
        mockkObject(SRPHelper)
        mockkObject(AuthHelper)
        coEvery { AuthHelper.getSecretHash(any(), any(), any()) } returns "dummy Hash"

        setupCurrentAuthState(
            authNState = AuthenticationState.SignedIn(
                mockk {
                    every { username } returns "username"
                },
                mockk()
            ),
            authZState = AuthorizationState.SessionEstablished(credentials)
        )
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun testFetchAuthSessionSucceedsIfSignedOut() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthSession>>()
        val onError = mockk<Consumer<AuthException>>(relaxed = true)

        setupCurrentAuthState(
            authNState = AuthenticationState.SignedOut(mockk()),
            authZState = AuthorizationState.Configured()
        )

        // WHEN
        plugin.fetchAuthSession(onSuccess, onError)

        // THEN
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun testCustomSignInWithSRPSucceedsWithChallenge() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthSignInResult>>()
        val onError = mockk<Consumer<AuthException>>(relaxed = true)

        setupCurrentAuthState(authNState = AuthenticationState.SignedOut(mockk()))

        // WHEN
        plugin.signIn(
            "username",
            "password",
            AWSCognitoAuthSignInOptions.builder().authFlowType(AuthFlowType.CUSTOM_AUTH_WITH_SRP).build(),
            onSuccess,
            onError
        )

        // THEN
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun testSignInFailsIfNotConfigured() {
        // GIVEN
        val expectedAuthError = InvalidUserPoolConfigurationException()
        val onSuccess = mockk<Consumer<AuthSignInResult>>()
        val onError = ConsumerWithLatch<AuthException>(expect = expectedAuthError)

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        coEvery { authConfiguration.authFlowType } returns AuthFlowType.USER_SRP_AUTH
        // WHEN
        plugin.signIn("user", "password", AuthSignInOptions.defaults(), onSuccess, onError)

        // THEN
        onError.shouldBeCalled()
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun testSignInFailsIfAlreadySignedIn() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>()
        coEvery { authConfiguration.authFlowType } returns AuthFlowType.USER_SRP_AUTH

        setupCurrentAuthState(
            authNState = AuthenticationState.SignedIn(
                SignedInData(
                    "userId",
                    "user",
                    Date(),
                    SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
                    CognitoUserPoolTokens("", "", "", 0)
                ),
                mockk()
            )
        )

        // WHEN
        plugin.signIn("user", "password", AuthSignInOptions.defaults(), mockk(), onError)

        // THEN
        onError.shouldBeCalled()
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
    fun fetchMFAPreferences() {
        val onSuccess = ConsumerWithLatch<UserMFAPreference>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("SMS_MFA", "SOFTWARE_TOKEN_MFA")
                preferredMfaSetting = "SOFTWARE_TOKEN_MFA"
                userAttributes = listOf()
                username = ""
            }
        }
        plugin.fetchMFAPreference(onSuccess, mockk())

        onSuccess.shouldBeCalled()
        assertEquals(setOf(MFAType.SMS, MFAType.TOTP), onSuccess.captured.enabled)
        assertEquals(MFAType.TOTP, onSuccess.captured.preferred)
    }

    @Test
    fun updateMFAPreferences() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()
        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }
        plugin.updateMFAPreference(MFAPreference.ENABLED, MFAPreference.PREFERRED, null, onSuccess, mockk())

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
    }

    @Test
    fun `updateMFAPreferences when currentpreference is totp both provided sms and totp preference are enabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("SMS_MFA", "SOFTWARE_TOKEN_MFA")
                preferredMfaSetting = "SOFTWARE_TOKEN_MFA"
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            null,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
    }

    @Test
    fun `updateMFAPreferences when currentpreference is sms both provided sms and totp preference are enabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("SMS_MFA", "SOFTWARE_TOKEN_MFA")
                preferredMfaSetting = "SMS_MFA"
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            null,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
    }

    @Test
    fun `updateMFAPreferences when current preference is email with additional sms and totp preferences are enabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("SMS_MFA", "SOFTWARE_TOKEN_MFA", "EMAIL_OTP")
                preferredMfaSetting = "EMAIL_OTP"
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            onSuccess,
            mockk()
        )

        assertTrue { onSuccess.latch.await(5, TimeUnit.SECONDS) }
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
    }

    @Test
    fun `updateMFAPreferences when provided email sms and totp preference are null and cognito throws an exception`() {
        val onError = ConsumerWithLatch<AuthException>()
        coEvery { mockCognitoIPClient.setUserMfaPreference(any()) } throws Exception()
        plugin.updateMFAPreference(null, null, null, mockk(), onError)

        onError.shouldBeCalled()
    }

    @Test
    fun `updateMFAPreferences when fetch preferences throws an exception`() {
        val onError = ConsumerWithLatch<AuthException>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        } throws Exception()
        plugin.updateMFAPreference(null, null, null, mockk(), onError)

        onError.shouldBeCalled()
    }

    @Test
    fun `updatepref  when currentpref is null and TOTP, SMS, and email are enabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is null and TOTP is enabled and SMS and email are disabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.DISABLED,
            MFAPreference.DISABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is null and TOTP and email are disabled and SMS is enabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.DISABLED,
            MFAPreference.ENABLED,
            MFAPreference.DISABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is null and TOTP and SMS are disabled and email is enabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.DISABLED,
            MFAPreference.DISABLED,
            MFAPreference.ENABLED,
            onSuccess,
            mockk()
        )

        assertTrue { onSuccess.latch.await(5, TimeUnit.SECONDS) }
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is null and SMS is preferred and TOTP and email are enabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.PREFERRED,
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is null and SMS and email are enabled and TOTP is preferred`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.PREFERRED,
            MFAPreference.ENABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is null and SMS and TOTP are enabled and email is preferred`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            MFAPreference.PREFERRED,
            onSuccess,
            mockk()
        )

        assertTrue { onSuccess.latch.await(5, TimeUnit.SECONDS) }
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is null and TOTP is preferred and SMS and email are disabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.DISABLED,
            MFAPreference.PREFERRED,
            MFAPreference.DISABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is null and TOTP and email are disabled and SMS is preferred`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.PREFERRED,
            MFAPreference.DISABLED,
            MFAPreference.DISABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is null and TOTP and sms are disabled and email is preferred`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = null
                preferredMfaSetting = null
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.DISABLED,
            MFAPreference.DISABLED,
            MFAPreference.PREFERRED,
            onSuccess,
            mockk()
        )

        assertTrue { onSuccess.latch.await(5, TimeUnit.SECONDS) }
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is TOTP preferred and TOTP parameter is disabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("SOFTWARE_TOKEN_MFA")
                preferredMfaSetting = "SOFTWARE_TOKEN_MFA"
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.DISABLED,
            MFAPreference.ENABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is SMS preferred and SMS parameter is disabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("SMS_MFA")
                preferredMfaSetting = "SMS_MFA"
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.DISABLED,
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is email preferred and email parameter is disabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("EMAIL_OTP")
                preferredMfaSetting = "EMAIL_OTP"
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            MFAPreference.DISABLED,
            onSuccess,
            mockk()
        )

        assertTrue { onSuccess.latch.await(5, TimeUnit.SECONDS) }
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref errors out when fetchMFA fails`() {
        val onSuccess = ActionWithLatch()
        val onError = mockk<Consumer<AuthException>>()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("SMS_MFA")
                preferredMfaSetting = "SMS_MFA"
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.DISABLED,
            MFAPreference.ENABLED,
            MFAPreference.DISABLED,
            onSuccess,
            onError
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = false
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is TOTP preferred and params include SMS preferred and TOTP and email enabled`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("SOFTWARE_TOKEN_MFA")
                preferredMfaSetting = "SOFTWARE_TOKEN_MFA"
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.PREFERRED,
            MFAPreference.ENABLED,
            MFAPreference.ENABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is SMS preferred and params include SMS and email enabled and TOTP preferred`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("SMS_MFA")
                preferredMfaSetting = "SMS_MFA"
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.PREFERRED,
            MFAPreference.ENABLED,
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    @Test
    fun `updatepref when currentpref is email preferred and params include SMS and email enabled and TOTP preferred`() {
        val onSuccess = ActionWithLatch()
        val setUserMFAPreferenceRequest = slot<SetUserMfaPreferenceRequest>()

        coEvery {
            mockCognitoIPClient.getUser {
                accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
            }
        }.answers {
            GetUserResponse.invoke {
                userMfaSettingList = listOf("EMAIL_OTP")
                preferredMfaSetting = "EMAIL_OTP"
                userAttributes = listOf()
                username = ""
            }
        }

        coEvery { mockCognitoIPClient.setUserMfaPreference(capture(setUserMFAPreferenceRequest)) }.answers {
            SetUserMfaPreferenceResponse.invoke {}
        }
        plugin.updateMFAPreference(
            MFAPreference.ENABLED,
            MFAPreference.PREFERRED,
            MFAPreference.ENABLED,
            onSuccess,
            mockk()
        )

        assertTrue { onSuccess.latch.await(5, TimeUnit.SECONDS) }
        assertTrue(setUserMFAPreferenceRequest.isCaptured)
        assertEquals(
            SmsMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.smsMfaSettings
        )
        assertEquals(
            SoftwareTokenMfaSettingsType.invoke {
                enabled = true
                preferredMfa = true
            },
            setUserMFAPreferenceRequest.captured.softwareTokenMfaSettings
        )
        assertEquals(
            EmailMfaSettingsType.invoke {
                enabled = true
                preferredMfa = false
            },
            setUserMFAPreferenceRequest.captured.emailMfaSettings
        )
    }

    private fun setupCurrentAuthState(authNState: AuthenticationState? = null, authZState: AuthorizationState? = null) {
        val currentAuthState = mockk<AuthState> {
            every { this@mockk.authNState } returns authNState
            every { this@mockk.authZState } returns authZState
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }
    }

    private class ActionWithLatch(count: Int = 1) : Action {
        val latch = CountDownLatch(count)
        override fun call() {
            assertTrue(latch.count > 0)
            latch.countDown()
        }

        fun shouldBeCalled(timeout: Duration = 5.seconds) = latch.await(timeout).shouldBeTrue()
    }

    private class ConsumerWithLatch<T : Any>(private val expect: T? = null, count: Int = 1) : Consumer<T> {
        val latch = CountDownLatch(count)
        lateinit var captured: T
            private set
        override fun accept(value: T) {
            if (expect == null || value == expect) {
                assertTrue(latch.count > 0)
                captured = value
                latch.countDown()
            }
        }

        fun shouldBeCalled(timeout: Duration = 5.seconds) = latch.await(timeout).shouldBeTrue()
    }
}
