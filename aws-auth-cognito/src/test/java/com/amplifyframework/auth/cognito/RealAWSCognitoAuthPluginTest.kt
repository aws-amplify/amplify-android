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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AnalyticsMetadataType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AssociateSoftwareTokenResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChangePasswordResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeviceType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.EmailMfaSettingsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgetDeviceResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserAttributeVerificationCodeResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ListDevicesResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResendConfirmationCodeResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SetUserMfaPreferenceRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SetUserMfaPreferenceResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SmsMfaSettingsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SoftwareTokenMfaNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SoftwareTokenMfaSettingsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateUserAttributesResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifySoftwareTokenRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifySoftwareTokenResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifySoftwareTokenResponseType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifyUserAttributeResponse
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.helpers.SessionHelper
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributeOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributesOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthVerifyTOTPSetupOptions
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.cognito.usecases.ResetPasswordUseCase
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.auth.result.step.AuthUpdateAttributeStep
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.statemachine.codegen.errors.SessionError
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.testutils.await
import featureTest.utilities.APICaptorFactory.Companion.onError
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun testSignUpFailsIfNotConfigured() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthSignUpResult>>()
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val expectedAuthError = InvalidUserPoolConfigurationException()

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        // WHEN
        plugin.signUp("user", "pass", AuthSignUpOptions.builder().build(), onSuccess, onError)

        // THEN
        verify(exactly = 0) { onSuccess.accept(any()) }
        verify { onError.accept(expectedAuthError) }
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
    fun testGetCurrentUserSucceedsIfSignedIn() {
        // GIVEN
        val onSuccess = ConsumerWithLatch<AuthUser>()
        val onError = mockk<Consumer<AuthException>>()
        mockkObject(SessionHelper)
        every { SessionHelper.getUsername(any()) } returns "username"
        every { SessionHelper.getUserSub(any()) } returns "sub"
        // WHEN
        plugin.getCurrentUser(onSuccess, onError)

        // THEN
        onSuccess.shouldBeCalled()
        verify(exactly = 0) { onError.accept(any()) }
    }

    @Test
    fun testGetCurrentUserFailsWithInvalidStateException() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthUser>>()
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidStateException())

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        // WHEN
        plugin.getCurrentUser(onSuccess, onError)

        // THEN
        onError.shouldBeCalled()
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun testGetCurrentUserFailsWithSignedOutException() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthUser>>()
        val onError = ConsumerWithLatch<AuthException>(expect = SignedOutException())

        setupCurrentAuthState(
            authNState = AuthenticationState.SignedOut(mockk()),
            authZState = AuthorizationState.Configured()
        )
        // WHEN
        plugin.getCurrentUser(onSuccess, onError)

        // THEN
        onError.shouldBeCalled()
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun testGetCurrentUserFailsWithExpiredSessionException() {
        // GIVEN
        val onGetCurrentUserSuccess = mockk<Consumer<AuthUser>>()
        val onGetCurrentUserError = ConsumerWithLatch<AuthException>(expect = SessionExpiredException())
        val sessionExpiredException = SessionExpiredException()
        val sessionError = SessionError(sessionExpiredException, credentials)
        val authNState = AuthenticationState.SignedIn(mockk { every { username } returns "username" }, mockk())
        val authZState = AuthorizationState.Error(sessionError)

        setupCurrentAuthState(
            authNState = authNState,
            authZState = authZState
        )

        val sessionErrorState = mockk<AuthState> {
            every { this@mockk.authNState } returns AuthenticationState.SignedIn(
                mockk { every { username } returns "username" },
                mockk()
            )
            every { this@mockk.authZState } returns AuthorizationState.Error(sessionError)
        }

        every {
            authStateMachine.listen(any(), captureLambda(), null)
        } answers {
            lambda<(AuthState) -> Unit>().invoke(sessionErrorState)
        }

        // WHEN
        plugin.getCurrentUser(onGetCurrentUserSuccess, onGetCurrentUserError)

        // THEN
        onGetCurrentUserError.shouldBeCalled()
        verify(exactly = 0) { onGetCurrentUserSuccess.accept(any()) }
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
    fun testConfirmSignUpFailsIfNotConfigured() {
        // GIVEN
        val expectedAuthError = InvalidUserPoolConfigurationException()
        val onSuccess = mockk<Consumer<AuthSignUpResult>>()
        val onError = ConsumerWithLatch<AuthException>(expect = expectedAuthError)

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        // WHEN
        plugin.confirmSignUp("user", "pass", AuthConfirmSignUpOptions.defaults(), onSuccess, onError)

        // THEN
        onError.shouldBeCalled()
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
    fun testResendSignUpCodeFailsIfNotConfigured() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidUserPoolConfigurationException())

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        // WHEN
        plugin.resendSignUpCode("user", AuthResendSignUpCodeOptions.defaults(), mockk(), onError)

        // THEN
        onError.shouldBeCalled()
    }

    @Test
    fun `update password with success`() {
        // GIVEN
        val onSuccess = ActionWithLatch()

        coEvery {
            authService.cognitoIdentityProviderClient?.changePassword(any())
        } returns ChangePasswordResponse.invoke { }

        // WHEN
        plugin.updatePassword("old", "new", onSuccess, mockk())

        onSuccess.shouldBeCalled()
    }

    @Test
    fun `update password fails when not in SignedIn state`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidStateException())

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        // WHEN
        plugin.updatePassword("old", "new", mockk(), onError)
        onError.shouldBeCalled()
    }

    @Test
    fun `update password fails when cognitoIdentityProviderClient not set`() {
        val onError = ConsumerWithLatch<AuthException>()

        plugin.updatePassword("old", "new", mockk(), onError)
        onError.shouldBeCalled()
    }

    @Test
    fun `reset password fails if cognitoIdentityProviderClient is not set`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthResetPasswordResult>>()
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidUserPoolConfigurationException())

        val userPool = UserPoolConfiguration.invoke { appClientId = "app Client Id" }
        every { authService.cognitoIdentityProviderClient } returns null
        every { authConfiguration.userPool } returns userPool

        // WHEN
        plugin.resetPassword("user", AuthResetPasswordOptions.defaults(), onSuccess, onError)

        // THEN
        onError.shouldBeCalled()
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun `reset password fails if appClientId is not set`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthResetPasswordResult>>()
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidUserPoolConfigurationException())

        val userPool = UserPoolConfiguration.invoke { appClientId = null }
        every { authService.cognitoIdentityProviderClient } returns mockk()
        every { authConfiguration.userPool } returns userPool

        // WHEN
        plugin.resetPassword("user", AuthResetPasswordOptions.defaults(), onSuccess, onError)

        // THEN
        onError.shouldBeCalled()
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun `reset password executes ResetPasswordUseCase if required params are set`() {
        // GIVEN
        val onSuccess = ConsumerWithLatch<AuthResetPasswordResult>()
        val onError = mockk<Consumer<AuthException>>()
        val options = mockk<AuthResetPasswordOptions>()
        val username = "user"
        val pinpointAppId = "abc"
        val encodedData = "encodedData"

        coEvery { authEnvironment.getUserContextData(username) } returns encodedData
        every { authEnvironment.getPinpointEndpointId() } returns pinpointAppId

        mockkConstructor(ResetPasswordUseCase::class)

        every { authService.cognitoIdentityProviderClient } returns mockk()
        every { authConfiguration.userPool } returns UserPoolConfiguration.invoke { appClientId = "app Client Id" }

        coEvery {
            anyConstructed<ResetPasswordUseCase>().execute(any(), any(), any(), any(), any(), any())
        } answers {
            arg<Consumer<AuthResetPasswordResult>>(4).accept(mockk())
        }

        // WHEN
        plugin.resetPassword(username, options, onSuccess, onError)

        // THEN
        onSuccess.shouldBeCalled()
    }

    @Test
    fun `fetch user attributes with success`() {
        // GIVEN
        val onSuccess = ConsumerWithLatch<List<AuthUserAttribute>>()

        val userAttributes = listOf(
            AttributeType.invoke {
                name = "email"
                value = "email"
            },
            AttributeType.invoke {
                name = "nickname"
                value = "nickname"
            }
        )

        val expectedResult = buildList {
            userAttributes.mapTo(this) {
                AuthUserAttribute(
                    AuthUserAttributeKey.custom(it.name),
                    it.value
                )
            }
        }

        coEvery {
            authService.cognitoIdentityProviderClient?.getUser(any())
        } returns GetUserResponse.invoke {
            this.userAttributes = userAttributes
            username = ""
        }

        // WHEN
        plugin.fetchUserAttributes(onSuccess, mockk())

        onSuccess.shouldBeCalled()
        assertEquals(expectedResult, onSuccess.captured)
    }

    @Test
    fun `fetch user attributes fails when not in SignedIn state`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidStateException())

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        // WHEN
        plugin.fetchUserAttributes(mockk(), onError)

        onError.shouldBeCalled()
    }

    @Test
    fun `confirmResetPassword fails if authentication state is NotConfigured`() {
        // Given
        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())
        val expectedError = AuthException(
            "Confirm Reset Password failed.",
            "Cognito User Pool not configured. Please check amplifyconfiguration.json file."
        )
        val onError = ConsumerWithLatch<AuthException>(expect = expectedError)

        // When
        plugin.confirmResetPassword("user", "pass", "code", mockk(), mockk(), onError)

        // Then
        onError.shouldBeCalled()
    }

    @Test
    fun `confirmResetPassword calls confirmForgotPassword API with given arguments`() {
        // GIVEN
        val latch = CountDownLatch(1)
        val requestBuilderCaptor = slot<ConfirmForgotPasswordRequest>()
        coEvery { mockCognitoIPClient.confirmForgotPassword(capture(requestBuilderCaptor)) } coAnswers {
            ConfirmForgotPasswordResponse.invoke { }
        }

        val user = "username"
        val pass = "passworD"
        val code = "007"

        val expectedRequestBuilder: ConfirmForgotPasswordRequest.Builder.() -> Unit = {
            username = user
            password = pass
            confirmationCode = code
            clientMetadata = mapOf()
            clientId = appClientId
            secretHash = AuthHelper.getSecretHash(
                username,
                appClientId,
                appClientSecret
            )
            userContextData = null
            analyticsMetadata = AnalyticsMetadataType.invoke { analyticsEndpointId = expectedEndpointId }
        }

        // WHEN
        plugin.confirmResetPassword(
            user,
            pass,
            code,
            AuthConfirmResetPasswordOptions.defaults(),
            { latch.countDown() },
            { latch.countDown() }
        )

        // THEN
        assertTrue { latch.await(5, TimeUnit.SECONDS) }
        assertEquals(
            ConfirmForgotPasswordRequest.invoke(expectedRequestBuilder),
            requestBuilderCaptor.captured
        )
    }

    @Test
    fun `onSuccess is called when confirmResetPassword call succeeds`() {
        // GIVEN
        val onSuccess = ActionWithLatch()
        val user = "username"
        val pass = "passworD"
        val code = "007"

        coEvery { mockCognitoIPClient.confirmForgotPassword(any()) } coAnswers {
            ConfirmForgotPasswordResponse.invoke { }
        }

        // WHEN
        plugin.confirmResetPassword(user, pass, code, AuthConfirmResetPasswordOptions.defaults(), onSuccess, mockk())

        // THEN
        onSuccess.shouldBeCalled()
    }

    @Test
    fun `AuthException is thrown when confirmForgotPassword API call fails`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>()

        val user = "username"
        val pass = "passworD"
        val code = "007"

        val expectedException = CognitoIdentityProviderException("Some SDK Message")
        coEvery { mockCognitoIPClient.confirmForgotPassword(any()) } coAnswers {
            throw expectedException
        }

        // WHEN
        plugin.confirmResetPassword(user, pass, code, AuthConfirmResetPasswordOptions.defaults(), mockk(), onError)

        // THEN
        onError.shouldBeCalled()
        assertEquals(expectedException, onError.captured.cause)
    }

    @Test
    fun `test resend signup code API with given arguments and auth signed out`() {
        setupCurrentAuthState(AuthenticationState.SignedOut(mockk()))
        `test resend signup code API with given arguments`()
    }

    @Test
    fun `test resend signup code API with given arguments and auth signed in`() {
        `test resend signup code API with given arguments`()
    }

    private fun `test resend signup code API with given arguments`() {
        val latch = CountDownLatch(1)

        // GIVEN
        val username = "user"

        val requestCaptor = slot<ResendConfirmationCodeRequest>()
        coEvery {
            authService.cognitoIdentityProviderClient?.resendConfirmationCode(capture(requestCaptor))
        } coAnswers {
            latch.countDown()
            mockk()
        }

        val expectedRequest: ResendConfirmationCodeRequest.Builder.() -> Unit = {
            clientId = "app Client Id"
            this.username = username
            secretHash = "dummy Hash"
            analyticsMetadata = AnalyticsMetadataType.invoke { analyticsEndpointId = expectedEndpointId }
        }

        // WHEN
        plugin.resendSignUpCode(
            username,
            AuthResendSignUpCodeOptions.defaults(),
            { latch.countDown() },
            { latch.countDown() }
        )
        assertTrue { latch.await(5, TimeUnit.SECONDS) }

        // THEN
        assertEquals(ResendConfirmationCodeRequest.invoke(expectedRequest), requestCaptor.captured)
    }

    @Test
    fun `test resend signup code success`() {
        // GIVEN
        val onSuccess = ConsumerWithLatch<AuthCodeDeliveryDetails>()
        val username = "user"

        setupCurrentAuthState(authNState = AuthenticationState.SignedOut(mockk()))

        val deliveryDetails = mapOf(
            "DESTINATION" to "destination",
            "MEDIUM" to "EMAIL",
            "ATTRIBUTE" to "attributeName"
        )

        val expectedCodeDeliveryDetails = AuthCodeDeliveryDetails(
            deliveryDetails.getValue("DESTINATION"),
            AuthCodeDeliveryDetails.DeliveryMedium.fromString(deliveryDetails.getValue("MEDIUM")),
            deliveryDetails.getValue("ATTRIBUTE")
        )

        coEvery { authService.cognitoIdentityProviderClient?.resendConfirmationCode(any()) } coAnswers {
            ResendConfirmationCodeResponse.invoke {
                this.codeDeliveryDetails {
                    this.attributeName = "attributeName"
                    this.deliveryMedium = DeliveryMediumType.Email
                    this.destination = "destination"
                }
            }
        }

        // WHEN
        plugin.resendSignUpCode(username, AuthResendSignUpCodeOptions.defaults(), onSuccess, onError)
        onSuccess.shouldBeCalled()

        // THEN
        assertEquals(expectedCodeDeliveryDetails, onSuccess.captured)
    }

    @Test
    fun `update user attribute fails when not in SignedIn state`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidStateException())

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        // WHEN
        plugin.updateUserAttribute(
            AuthUserAttribute(AuthUserAttributeKey.email(), "test@test.com"),
            mockk(),
            onError
        )

        onError.shouldBeCalled()
    }

    @Test
    fun `update user attributes fails when not in SignedIn state`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidStateException())

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        // WHEN
        plugin.updateUserAttributes(
            mutableListOf(
                AuthUserAttribute(AuthUserAttributeKey.email(), "test@test.com"),
                AuthUserAttribute(AuthUserAttributeKey.nickname(), "test")
            ),
            AuthUpdateUserAttributesOptions.defaults(),
            mockk(),
            onError
        )

        onError.shouldBeCalled()
    }

    @Test
    fun `update single user attribute with no attribute options and delivery code success`() {
        // GIVEN
        val onSuccess = ConsumerWithLatch<AuthUpdateAttributeResult>()

        coEvery {
            authService.cognitoIdentityProviderClient?.updateUserAttributes(any())
        } returns UpdateUserAttributesResponse.invoke {}

        // WHEN
        plugin.updateUserAttribute(
            AuthUserAttribute(AuthUserAttributeKey.email(), "test@test.com"),
            onSuccess,
            onError
        )

        onSuccess.shouldBeCalled()
        assertTrue(onSuccess.captured.isUpdated, "attribute should be successfully updated")
        assertNotNull(onSuccess.captured.nextStep, "next step should not be null")
        assertNull(onSuccess.captured.nextStep.codeDeliveryDetails, "code delivery details should be null")
        assertEquals(
            onSuccess.captured.nextStep.updateAttributeStep,
            AuthUpdateAttributeStep.DONE,
            "next step should be done"
        )
    }

    @Test
    fun `update single user attribute with attribute options and no delivery code success`() {
        // GIVEN
        val onSuccess = ConsumerWithLatch<AuthUpdateAttributeResult>()

        coEvery {
            authService.cognitoIdentityProviderClient?.updateUserAttributes(any())
        } returns UpdateUserAttributesResponse.invoke {}

        val builder = AWSCognitoAuthUpdateUserAttributeOptions.builder().metadata(
            mapOf("x" to "x", "y" to "y", "z" to "z")
        )
        // WHEN
        plugin.updateUserAttribute(
            AuthUserAttribute(AuthUserAttributeKey.email(), "test@test.com"),
            builder.build(),
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertTrue(onSuccess.captured.isUpdated, "attribute should be successfully updated")
        assertNotNull(onSuccess.captured.nextStep, "next step should not be null")
        assertNull(onSuccess.captured.nextStep.codeDeliveryDetails, "code delivery details should be null")
        assertEquals(
            onSuccess.captured.nextStep.updateAttributeStep,
            AuthUpdateAttributeStep.DONE,
            "next step should be done"
        )
    }

    @Test
    fun `update user attributes with delivery code success`() {
        // GIVEN
        val onSuccess = ConsumerWithLatch<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>>()

        coEvery {
            authService.cognitoIdentityProviderClient?.updateUserAttributes(any())
        } returns UpdateUserAttributesResponse.invoke {
            codeDeliveryDetailsList = listOf(
                CodeDeliveryDetailsType.invoke {
                    attributeName = "email"
                    deliveryMedium = DeliveryMediumType.Email
                }
            )
        }

        val builder = AWSCognitoAuthUpdateUserAttributesOptions.builder().metadata(
            mapOf("x" to "x", "y" to "y", "z" to "z")
        )

        // WHEN
        plugin.updateUserAttributes(
            mutableListOf(
                AuthUserAttribute(AuthUserAttributeKey.email(), "test@test.com"),
                AuthUserAttribute(AuthUserAttributeKey.nickname(), "test")
            ),
            builder.build(),
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        assertEquals(onSuccess.captured.size, 2)
        // nickname
        assertNotNull(onSuccess.captured[AuthUserAttributeKey.nickname()], "nick name should be in result")
        assertTrue(
            onSuccess.captured[AuthUserAttributeKey.nickname()]?.isUpdated ?: false,
            "nickname attribute should be successfully updated"
        )
        assertNotNull(
            onSuccess.captured[AuthUserAttributeKey.nickname()]?.nextStep,
            "next step should not be null for nickname attribute"
        )
        assertNull(
            onSuccess.captured[AuthUserAttributeKey.nickname()]?.nextStep?.codeDeliveryDetails,
            "code delivery details should be null for nickname attribute"
        )
        assertEquals(
            onSuccess.captured[AuthUserAttributeKey.nickname()]?.nextStep?.updateAttributeStep,
            AuthUpdateAttributeStep.DONE,
            "next step for nickname attribute should be done"
        )

        // email
        assertNotNull(onSuccess.captured[AuthUserAttributeKey.email()], "email should be in result")
        assertFalse(
            onSuccess.captured[AuthUserAttributeKey.email()]?.isUpdated ?: false,
            "email attribute should not be successfully updated"
        )
        assertNotNull(
            onSuccess.captured[AuthUserAttributeKey.email()]?.nextStep,
            "next step should not be null for email attribute"
        )
        assertNotNull(
            onSuccess.captured[AuthUserAttributeKey.email()]?.nextStep?.codeDeliveryDetails,
            "code delivery details should not be null for email attribute"
        )
        assertEquals(
            onSuccess.captured[AuthUserAttributeKey.email()]?.nextStep?.codeDeliveryDetails?.attributeName,
            "email",
            "email attribute should not be successfully updated"
        )
        assertEquals(
            onSuccess.captured[AuthUserAttributeKey.email()]?.nextStep?.updateAttributeStep,
            AuthUpdateAttributeStep.CONFIRM_ATTRIBUTE_WITH_CODE,
            "next step for email attribute should be confirm_attribute_with_code"
        )
    }

    @Test
    fun `confirm user attribute fails when not in SignedIn state`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidStateException())

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        // WHEN
        plugin.confirmUserAttribute(
            AuthUserAttributeKey.email(),
            "000000",
            mockk(),
            onError
        )

        onError.shouldBeCalled()
    }

    @Test
    fun `confirm user attribute fails when access token is invalid`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidUserPoolConfigurationException())

        val invalidCredentials = AmplifyCredential.UserPool(
            SignedInData(
                "userId",
                "username",
                Date(),
                SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
                CognitoUserPoolTokens(null, null, null, 120L)
            )
        )

        setupCurrentAuthState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.SessionEstablished(invalidCredentials)
        )

        // WHEN
        plugin.confirmUserAttribute(
            AuthUserAttributeKey.email(),
            "000000",
            mockk(),
            onError
        )

        onError.shouldBeCalled()
    }

    @Test
    fun `confirm user attributes with cognito api call error`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>()

        val expectedException = CognitoIdentityProviderException("Some Cognito Message")
        coEvery {
            authService.cognitoIdentityProviderClient?.verifyUserAttribute(any())
        } throws expectedException

        plugin.confirmUserAttribute(
            AuthUserAttributeKey.email(),
            "000000",
            mockk(),
            onError
        )

        onError.shouldBeCalled()
        assertEquals(expectedException, onError.captured.cause)
    }

    @Test
    fun `confirm user attributes with success`() {
        // GIVEN
        val onSuccess = ActionWithLatch()

        coEvery {
            authService.cognitoIdentityProviderClient?.verifyUserAttribute(any())
        } returns VerifyUserAttributeResponse.invoke {}

        plugin.confirmUserAttribute(
            AuthUserAttributeKey.email(),
            "000000",
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
    }

    @Test
    fun `resend user attribute confirmation code fails when not in SignedIn state`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidStateException())

        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        // WHEN
        plugin.resendUserAttributeConfirmationCode(
            AuthUserAttributeKey.email(),
            mockk(),
            onError
        )

        onError.shouldBeCalled()
    }

    @Test
    fun `resend user attribute confirmation code fails when access token is invalid`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>(expect = InvalidUserPoolConfigurationException())

        val invalidCredentials = AmplifyCredential.UserPool(
            SignedInData(
                "userId",
                "username",
                Date(),
                SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
                CognitoUserPoolTokens(null, null, null, 120L)
            )
        )

        setupCurrentAuthState(
            authNState = AuthenticationState.SignedIn(mockk(), mockk()),
            authZState = AuthorizationState.SessionEstablished(invalidCredentials)
        )

        // WHEN
        plugin.resendUserAttributeConfirmationCode(
            AuthUserAttributeKey.email(),
            mockk(),
            onError
        )

        onError.shouldBeCalled()
    }

    @Test
    fun `resend user attribute confirmation code with cognito api call error`() {
        // GIVEN
        val onError = ConsumerWithLatch<AuthException>()

        val expectedException = CognitoIdentityProviderException("Some Cognito Message")
        coEvery {
            authService.cognitoIdentityProviderClient?.getUserAttributeVerificationCode(
                any()
            )
        } answers {
            throw expectedException
        }

        plugin.resendUserAttributeConfirmationCode(
            AuthUserAttributeKey.email(),
            mockk(),
            onError
        )

        onError.shouldBeCalled()
        assertEquals(expectedException, onError.captured.cause)
    }

    @Test
    fun `resend user attribute confirmation code with delivery code success`() {
        // GIVEN
        val onSuccess = ConsumerWithLatch<AuthCodeDeliveryDetails>()

        coEvery {
            authService.cognitoIdentityProviderClient?.getUserAttributeVerificationCode(
                any()
            )
        } returns GetUserAttributeVerificationCodeResponse.invoke {
            codeDeliveryDetails = CodeDeliveryDetailsType.invoke {
                attributeName = "email"
                deliveryMedium = DeliveryMediumType.Email
                destination = "test"
            }
        }

        val builder = AWSCognitoAuthResendUserAttributeConfirmationCodeOptions.builder().metadata(
            mapOf("x" to "x", "y" to "y", "z" to "z")
        )

        // WHEN
        plugin.resendUserAttributeConfirmationCode(
            AuthUserAttributeKey.email(),
            builder.build(),
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
        // nickname
        assertEquals(
            onSuccess.captured.attributeName,
            "email",
            "attribute name should be email"
        )
        assertEquals(
            onSuccess.captured.destination,
            "test",
            "destination for code delivery do not match expected"
        )

        assertNotNull(
            onSuccess.captured.deliveryMedium,
            "Delivery medium should not be null"
        )

        assertEquals(
            onSuccess.captured.deliveryMedium,
            AuthCodeDeliveryDetails.DeliveryMedium.EMAIL,
            "Delivery medium did not match expected value"
        )
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
    fun `setupTOTP on success`() {
        val onSuccess = ConsumerWithLatch<TOTPSetupDetails>()

        val session = "SESSION"
        val secretCode = "SECRET_CODE"
        coEvery { mockCognitoIPClient.associateSoftwareToken(any()) }.answers {
            AssociateSoftwareTokenResponse.invoke {
                this.session = session
                this.secretCode = secretCode
            }
        }

        plugin.setUpTOTP(onSuccess, mockk())

        onSuccess.shouldBeCalled()
        assertEquals(secretCode, onSuccess.captured.sharedSecret)
    }

    @Test
    fun `setupTOTP on error`() {
        val onError = ConsumerWithLatch<AuthException>()

        val expectedErrorMessage = "Software token MFA not enabled"
        coEvery { mockCognitoIPClient.associateSoftwareToken(any()) }.answers {
            throw SoftwareTokenMfaNotFoundException.invoke {
                message = expectedErrorMessage
            }
        }

        plugin.setUpTOTP(mockk(), onError)

        onError.shouldBeCalled()
        assertEquals(expectedErrorMessage, onError.captured.cause?.message)
    }

    @Test
    fun `verifyTOTP on success`() {
        val onSuccess = ActionWithLatch()
        val code = "123456"
        val friendlyDeviceName = "DEVICE_NAME"
        coEvery {
            mockCognitoIPClient.verifySoftwareToken(
                VerifySoftwareTokenRequest.invoke {
                    userCode = code
                    this.friendlyDeviceName = friendlyDeviceName
                    accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
                }
            )
        }.answers {
            VerifySoftwareTokenResponse.invoke {
                status = VerifySoftwareTokenResponseType.Success
            }
        }

        plugin.verifyTOTPSetup(
            code,
            AWSCognitoAuthVerifyTOTPSetupOptions.CognitoBuilder().friendlyDeviceName(friendlyDeviceName).build(),
            onSuccess,
            mockk()
        )

        onSuccess.shouldBeCalled()
    }

    @Test
    fun `verifyTOTP on error`() {
        val onError = ConsumerWithLatch<AuthException>()

        val code = "123456"
        val errorMessage = "Invalid code"
        coEvery {
            mockCognitoIPClient.verifySoftwareToken(
                VerifySoftwareTokenRequest.invoke {
                    userCode = code
                    accessToken = credentials.signedInData.cognitoUserPoolTokens.accessToken
                }
            )
        }.answers {
            VerifySoftwareTokenResponse.invoke {
                throw CodeMismatchException.invoke {
                    message = errorMessage
                }
            }
        }

        plugin.verifyTOTPSetup(
            code,
            AWSCognitoAuthVerifyTOTPSetupOptions.CognitoBuilder().build(),
            mockk(),
            onError
        )

        onError.shouldBeCalled()
        assertEquals(errorMessage, onError.captured.cause?.message)
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

    @Test
    fun `forget device invokes ForgetDevice api`() {
        val onSuccess = ActionWithLatch()

        coEvery { mockCognitoIPClient.forgetDevice(any()) } answers { ForgetDeviceResponse.invoke {} }

        coEvery {
            authEnvironment.getDeviceMetadata("username")
        } returns DeviceMetadata.Metadata(deviceKey = "test", deviceGroupKey = "group")

        plugin.forgetDevice(onSuccess, mockk())

        onSuccess.shouldBeCalled()
        coVerify { mockCognitoIPClient.forgetDevice(match { it.deviceKey == "test" }) }
    }

    @Test
    fun `forget device emits API error`() {
        val onError = ConsumerWithLatch<AuthException>()

        coEvery { mockCognitoIPClient.forgetDevice(any()) } throws Exception("failed")

        coEvery {
            authEnvironment.getDeviceMetadata("username")
        } returns DeviceMetadata.Metadata(deviceKey = "test", deviceGroupKey = "group")

        plugin.forgetDevice(mockk(), onError)

        onError.shouldBeCalled()
        assertEquals("failed", onError.captured.cause?.message)
    }

    @Test
    fun `forget specific device invokes ForgetDevice api`() {
        val onSuccess = ActionWithLatch()

        coEvery { mockCognitoIPClient.forgetDevice(any()) } answers { ForgetDeviceResponse.invoke {} }

        plugin.forgetDevice(AuthDevice.fromId("test"), onSuccess, mockk())

        onSuccess.shouldBeCalled()
        coVerify { mockCognitoIPClient.forgetDevice(match { it.deviceKey == "test" }) }
    }

    @Test
    fun `forget specific device emits API error`() {
        val onError = ConsumerWithLatch<AuthException>()

        coEvery { mockCognitoIPClient.forgetDevice(any()) } throws Exception("failed")

        plugin.forgetDevice(AuthDevice.fromId("test"), mockk(), onError)

        onError.shouldBeCalled()
        assertEquals("failed", onError.captured.cause?.message)
    }

    @Test
    fun `fetch devices returns device id and name`() {
        val onSuccess = ConsumerWithLatch<List<AuthDevice>>()

        coEvery { mockCognitoIPClient.listDevices(any()) } returns ListDevicesResponse.invoke {
            devices = listOf(
                DeviceType.invoke {
                    deviceKey = "id1"
                    deviceAttributes = listOf(
                        AttributeType.invoke {
                            name = "device_name"
                            value = "name1"
                        }
                    )
                }
            )
        }

        plugin.fetchDevices(onSuccess, mockk())

        onSuccess.shouldBeCalled()
        assertEquals("id1", onSuccess.captured.first().id)
        assertEquals("name1", onSuccess.captured.first().name)
    }

    @Test
    fun `fetch devices returns error if listDevices fails`() {
        val onError = ConsumerWithLatch<AuthException>()
        coEvery { mockCognitoIPClient.listDevices(any()) } throws Exception("bad")

        plugin.fetchDevices(mockk(), onError)

        onError.shouldBeCalled()
    }

    @Test
    fun `fetch devices returns error if signed out`() {
        val onError = ConsumerWithLatch<AuthException>()
        setupCurrentAuthState(authNState = AuthenticationState.SignedOut(mockk()))

        plugin.fetchDevices(mockk(), onError)

        onError.shouldBeCalled()
        assertEquals(SignedOutException(), onError.captured)
    }

    @Test
    fun `fetch devices returns error if not signed in`() {
        val onError = ConsumerWithLatch<AuthException>()
        setupCurrentAuthState(authNState = AuthenticationState.NotConfigured())

        plugin.fetchDevices(mockk(), onError)

        onError.shouldBeCalled()
        assertEquals(InvalidStateException(), onError.captured)
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
