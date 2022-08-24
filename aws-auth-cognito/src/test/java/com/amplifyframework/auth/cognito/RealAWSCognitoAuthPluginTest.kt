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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChangePasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChangePasswordResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmSignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmSignUpResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResendConfirmationCodeResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateUserAttributesRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateUserAttributesResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifyUserAttributeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifyUserAttributeResponse
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributeOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributesOptions
import com.amplifyframework.auth.cognito.usecases.ResetPasswordUseCase
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.auth.result.step.AuthUpdateAttributeStep
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.invoke
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class RealAWSCognitoAuthPluginTest {

    private val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IkpvaG4gRG9lIiwiZXhwIjoxNTE2Mj" +
        "M5MDIyfQ.e4RpZTfAb3oXkfq3IwHtR_8Zhn0U1JDV7McZPlBXyhw"

    private var logger = mockk<Logger>(relaxed = true)
    private val appClientId = "app Client Id"
    private var authConfiguration = mockk<AuthConfiguration> {
        every { userPool } returns UserPoolConfiguration.invoke {
            this.appClientId = this@RealAWSCognitoAuthPluginTest.appClientId
        }
    }

    private val credentials = AmplifyCredential.UserPool(
        CognitoUserPoolTokens(dummyToken, dummyToken, dummyToken, 120L)
    )

    private val mockCognitoIPClient = mockk<CognitoIdentityProviderClient>()
    private var authService = mockk<AWSCognitoAuthServiceBehavior> {
        every { cognitoIdentityProviderClient } returns mockCognitoIPClient
    }

    private var authEnvironment = mockk<AuthEnvironment> {
        every { configuration } returns authConfiguration
        every { logger } returns this@RealAWSCognitoAuthPluginTest.logger
        every { cognitoAuthService } returns authService
    }

    private var currentState: AuthenticationState = AuthenticationState.Configured()

    private var authStateMachine = mockk<AuthStateMachine>(relaxed = true) {
        every { getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(
                mockk {
                    every { authNState } returns currentState
                }
            )
        }
    }
    private var credentialStoreStateMachine = mockk<CredentialStoreStateMachine>(relaxed = true)

    private lateinit var plugin: RealAWSCognitoAuthPlugin

    @Before
    fun setup() {
        plugin = RealAWSCognitoAuthPlugin(
            authConfiguration,
            authEnvironment,
            authStateMachine,
            credentialStoreStateMachine,
            logger
        )

        mockkStatic("com.amplifyframework.auth.cognito.AWSCognitoAuthSessionKt")
        every { any<AmplifyCredential>().isValid() } returns true

        // set up user pool
        coEvery { authConfiguration.userPool } returns UserPoolConfiguration.invoke {
            appClientId = "app Client Id"
            appClientSecret = "app Client Secret"
        }

        // set up SRP helper
        mockkObject(SRPHelper)
        coEvery { SRPHelper.getSecretHash(any(), any(), any()) } returns "dummy Hash"
    }

    @Test
    fun signUpFailsIfNotConfigured() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthSignUpResult>>()
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val expectedAuthError = AuthException(
            "Sign up failed.",
            "Cognito User Pool not configured. Please check amplifyconfiguration.json file."
        )
        currentState = AuthenticationState.NotConfigured()

        // WHEN
        plugin.signUp("user", "pass", AuthSignUpOptions.builder().build(), onSuccess, onError)

        // THEN
        verify(exactly = 0) { onSuccess.accept(any()) }
        verify { onError.accept(expectedAuthError) }
    }

    @Test
    fun `update password with success`() {
        // GIVEN
        val latch = CountDownLatch(1)
        val onSuccess = mockk<Action> {
            every { call() } answers { latch.countDown() }
        }
        val onError = mockk<Consumer<AuthException>>(relaxed = true)

        val currentAuthState = mockk<AuthState> {
            every { authNState } returns AuthenticationState.SignedIn(mockk())
            every { authZState } returns AuthorizationState.SessionEstablished(credentials)
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        coEvery {
            authService.cognitoIdentityProviderClient?.changePassword(any<ChangePasswordRequest>())
        } returns ChangePasswordResponse.invoke { }

        // WHEN
        plugin.updatePassword("old", "new", onSuccess, onError)

        assertTrue { latch.await(5, TimeUnit.SECONDS) }

        verify { onSuccess.call() }
        coVerify(exactly = 0) { onError.accept(any()) }
    }

    @Test
    fun `update password fails when not in SignedIn state`() {
        // GIVEN
        val onSuccess = mockk<Action>(relaxed = true)
        val latch = CountDownLatch(1)
        val onError = mockk<Consumer<AuthException>> {
            every { accept(AuthException.InvalidStateException()) } answers { latch.countDown() }
        }

        currentState = AuthenticationState.NotConfigured()

        // WHEN
        plugin.updatePassword("old", "new", onSuccess, onError)
        assertTrue { latch.await(5, TimeUnit.SECONDS) }

        verify(exactly = 0) { onSuccess.call() }
        verify { onError.accept(AuthException.InvalidStateException()) }
    }

    @Test
    @Ignore("fix use case")
    fun `update password fails when cognitoIdentityProviderClient not set`() {
        val onSuccess = mockk<Action>(relaxed = true)
        val latch = CountDownLatch(1)
        val onError = mockk<Consumer<AuthException>> {
            every { accept(any()) } answers { latch.countDown() }
        }
        val currentAuthState = mockk<AuthState> {
            every { authNState } returns AuthenticationState.SignedIn(mockk())
            every { authZState } returns AuthorizationState.SessionEstablished(credentials)
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        plugin.updatePassword("old", "new", onSuccess, onError)
        assertTrue { latch.await(5, TimeUnit.SECONDS) }

        verify(exactly = 0) { onSuccess.call() }
        coVerify { onError.accept(any()) }
    }

    @Test
    fun `reset password fails if cognitoIdentityProviderClient is not set`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthResetPasswordResult>>()
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val expectedAuthError = AuthException.InvalidUserPoolConfigurationException(
            IllegalArgumentException("Required value was null.")
        )

        val userPool = UserPoolConfiguration.invoke { appClientId = "app Client Id" }
        every { authService.cognitoIdentityProviderClient } returns null
        every { authConfiguration.userPool } returns userPool

        val errorCaptor = slot<AuthException.InvalidUserPoolConfigurationException>()
        justRun { onError.accept(capture(errorCaptor)) }

        // WHEN
        plugin.resetPassword("user", AuthResetPasswordOptions.defaults(), onSuccess, onError)

        // THEN
        verify(exactly = 0) { onSuccess.accept(any()) }
        assertEquals(expectedAuthError.toString(), errorCaptor.captured.toString())
    }

    @Test
    fun `reset password fails if appClientId is not set`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthResetPasswordResult>>()
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val expectedAuthError = AuthException.InvalidUserPoolConfigurationException(
            IllegalArgumentException("Required value was null.")
        )

        val userPool = UserPoolConfiguration.invoke { appClientId = null }
        every { authService.cognitoIdentityProviderClient } returns mockk()
        every { authConfiguration.userPool } returns userPool

        val errorCaptor = slot<AuthException.InvalidUserPoolConfigurationException>()
        justRun { onError.accept(capture(errorCaptor)) }

        // WHEN
        plugin.resetPassword("user", AuthResetPasswordOptions.defaults(), onSuccess, onError)

        // THEN
        verify(exactly = 0) { onSuccess.accept(any()) }
        assertEquals(expectedAuthError.toString(), errorCaptor.captured.toString())
    }

    @Ignore("Test fails in build server")
    @Test
    fun `reset password executes ResetPasswordUseCase if required params are set`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthResetPasswordResult>>()
        val onError = mockk<Consumer<AuthException>>()
        val options = mockk<AuthResetPasswordOptions>()
        val username = "user"

        mockkConstructor(ResetPasswordUseCase::class)

        every { authService.cognitoIdentityProviderClient } returns mockk()
        every { authConfiguration.userPool } returns UserPoolConfiguration.invoke { appClientId = "app Client Id" }
        coJustRun { anyConstructed<ResetPasswordUseCase>().execute(username, options, onSuccess, onError) }

        // WHEN
        plugin.resetPassword(username, options, onSuccess, onError)

        // THEN
        coVerify { anyConstructed<ResetPasswordUseCase>().execute(username, options, onSuccess, onError) }
    }

    @Test
    fun `fetch user attributes with success`() {
        // GIVEN
        val onSuccess = mockk<Consumer<MutableList<AuthUserAttribute>>>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        val currentAuthState = mockk<AuthState> {
            every { authNState } returns AuthenticationState.SignedIn(mockk())
            every { authZState } returns AuthorizationState.SessionEstablished(credentials)
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

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
            authService.cognitoIdentityProviderClient?.getUser(any<GetUserRequest>())
        } returns GetUserResponse.invoke {
            this.userAttributes = userAttributes
        }

        every {
            onSuccess.accept(expectedResult.toMutableList())
        } answers {
            listenLatch.countDown()
        }

        // WHEN
        plugin.fetchUserAttributes(onSuccess, onError)

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        coVerify(exactly = 1) { onSuccess.accept(expectedResult.toMutableList()) }
        coVerify(exactly = 0) { onError.accept(any()) }
    }

    @Test
    fun `fetch user attributes fails when not in SignedIn state`() {
        // GIVEN
        val onSuccess = mockk<Consumer<MutableList<AuthUserAttribute>>>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        currentState = AuthenticationState.NotConfigured()

        every {
            onError.accept(AuthException.InvalidStateException())
        } answers {
            listenLatch.countDown()
        }

        // WHEN
        plugin.fetchUserAttributes(onSuccess, onError)

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        coVerify(exactly = 1) { onError.accept(AuthException.InvalidStateException()) }
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun `confirmResetPassword fails if authentication state is NotConfigured`() {
        // Given
        val latch = CountDownLatch(1)
        currentState = AuthenticationState.NotConfigured()
        val onSuccess = mockk<Action> { every { call() } answers { latch.countDown() } }
        val onError = mockk<Consumer<AuthException>> { every { accept(any()) } answers { latch.countDown() } }
        val expectedError = AuthException(
            "Confirm Reset Password failed.",
            "Cognito User Pool not configured. Please check amplifyconfiguration.json file."
        )

        // When
        plugin.confirmResetPassword("user", "pass", "code", mockk(), onSuccess, onError)
        assertTrue { latch.await(5, TimeUnit.SECONDS) }

        // Then
        verify(exactly = 0) { onSuccess.call() }
        verify { onError.accept(expectedError) }
    }

    @Test
    fun `confirmResetPassword calls confirmForgotPassword API with given arguments`() {
        // GIVEN
        val latch = CountDownLatch(1)
        val requestBuilderCaptor = slot<ConfirmForgotPasswordRequest.Builder.() -> Unit>()
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

        println(ConfirmForgotPasswordRequest.invoke(expectedRequestBuilder))
        println(ConfirmForgotPasswordRequest.invoke(requestBuilderCaptor.captured))
        assertEquals(
            ConfirmForgotPasswordRequest.invoke(expectedRequestBuilder),
            ConfirmForgotPasswordRequest.invoke(requestBuilderCaptor.captured)
        )
    }

    @Test
    fun `onSuccess is called when confirmResetPassword call succeeds`() {
        // GIVEN
        val latch = CountDownLatch(1)
        val onSuccess = mockk<Action> {
            every { call() } answers { latch.countDown() }
        }
        val onError = mockk<Consumer<AuthException>>()
        val user = "username"
        val pass = "passworD"
        val code = "007"

        coEvery { mockCognitoIPClient.confirmForgotPassword(captureLambda()) } coAnswers {
            ConfirmForgotPasswordResponse.invoke { }
        }

        // WHEN
        plugin.confirmResetPassword(user, pass, code, AuthConfirmResetPasswordOptions.defaults(), onSuccess, onError)

        // THEN
        assertTrue { latch.await(5, TimeUnit.SECONDS) }
        verify(exactly = 0) { onError.accept(any()) }
        verify(exactly = 1) { onSuccess.call() }
    }

    @Test
    fun `AuthException is thrown when confirmForgotPassword API call fails`() {
        // GIVEN
        val latch = CountDownLatch(1)
        val onSuccess = mockk<Action>()
        val onError = mockk<Consumer<AuthException>>()

        val user = "username"
        val pass = "passworD"
        val code = "007"

        val expectedException = CognitoIdentityProviderException("Some SDK Message")
        coEvery { mockCognitoIPClient.confirmForgotPassword(captureLambda()) } coAnswers {
            throw expectedException
        }

        val resultCaptor = slot<AuthException>()
        every { onError.accept(capture(resultCaptor)) } answers { latch.countDown() }

        // WHEN
        plugin.confirmResetPassword(user, pass, code, AuthConfirmResetPasswordOptions.defaults(), onSuccess, onError)

        // THEN
        assertTrue { latch.await(5, TimeUnit.SECONDS) }
        verify(exactly = 0) { onSuccess.call() }
        verify { onError.accept(resultCaptor.captured) }

        assertEquals(expectedException, resultCaptor.captured.cause)
    }

    @Test
    fun `test signup API with given arguments`() {
        val latch = CountDownLatch(1)

        // GIVEN
        val username = "user"
        val password = "password"
        val email = "user@domain.com"
        val options = AuthSignUpOptions.builder().userAttribute(AuthUserAttributeKey.email(), email).build()

        val currentAuthState = mockk<AuthState> {
            coEvery { authNState } returns AuthenticationState.Configured()
        }
        coEvery { authStateMachine.getCurrentState(captureLambda()) } coAnswers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        val requestCaptor = slot<SignUpRequest.Builder.() -> Unit>()
        coEvery { authService.cognitoIdentityProviderClient?.signUp(capture(requestCaptor)) } coAnswers {
            latch.countDown()
            mockk()
        }

        val expectedRequest: SignUpRequest.Builder.() -> Unit = {
            clientId = "app Client Id"
            this.username = username
            this.password = password
            userAttributes = listOf(
                AttributeType {
                    name = "email"
                    value = email
                }
            )
            secretHash = "dummy Hash"
        }

        // WHEN
        plugin.signUp(username, password, options, {}, {})
        assertTrue { latch.await(5, TimeUnit.SECONDS) }

        // THEN
        assertEquals(SignUpRequest.invoke(expectedRequest), SignUpRequest.invoke(requestCaptor.captured))
    }

    @Test
    fun `test signup success`() {
        val latch = CountDownLatch(1)

        // GIVEN
        val onSuccess = mockk<Consumer<AuthSignUpResult>>()
        val onError = mockk<Consumer<AuthException>>()
        val username = "user"
        val password = "password"
        val email = "user@domain.com"
        val options = AuthSignUpOptions.builder().userAttribute(AuthUserAttributeKey.email(), email).build()

        val resultCaptor = slot<AuthSignUpResult>()
        every { onSuccess.accept(capture(resultCaptor)) } answers { latch.countDown() }

        val currentAuthState = mockk<AuthState> {
            coEvery { authNState } returns AuthenticationState.Configured()
        }
        coEvery { authStateMachine.getCurrentState(captureLambda()) } coAnswers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        val deliveryDetails = mapOf(
            "DESTINATION" to email,
            "MEDIUM" to "EMAIL",
            "ATTRIBUTE" to "attributeName"
        )

        val expectedAuthSignUpResult = AuthSignUpResult(
            false,
            AuthNextSignUpStep(
                AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                mapOf(),
                AuthCodeDeliveryDetails(
                    deliveryDetails.getValue("DESTINATION"),
                    AuthCodeDeliveryDetails.DeliveryMedium.fromString(deliveryDetails.getValue("MEDIUM")),
                    deliveryDetails.getValue("ATTRIBUTE")
                )
            ),
            AuthUser("", username)
        )

        coEvery { authService.cognitoIdentityProviderClient?.signUp(captureLambda()) } coAnswers {
            SignUpResponse.invoke {
                this.codeDeliveryDetails {
                    this.attributeName = "attributeName"
                    this.deliveryMedium = DeliveryMediumType.Email
                    this.destination = email
                }
            }
        }

        // WHEN
        plugin.signUp(username, password, options, onSuccess, onError)
        assertTrue { latch.await(5, TimeUnit.SECONDS) }

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify(exactly = 1) { onSuccess.accept(resultCaptor.captured) }

        assertEquals(expectedAuthSignUpResult, resultCaptor.captured)
    }

    @Test
    fun `test confirm signup API with given arguments`() {
        val latch = CountDownLatch(1)

        // GIVEN
        val username = "user"
        val confirmationCode = "123456"
        val options = AuthConfirmSignUpOptions.defaults()

        val currentAuthState = mockk<AuthState> {
            coEvery { authNState } returns AuthenticationState.Configured()
        }
        coEvery { authStateMachine.getCurrentState(captureLambda()) } coAnswers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        val requestCaptor = slot<ConfirmSignUpRequest.Builder.() -> Unit>()
        coEvery { authService.cognitoIdentityProviderClient?.confirmSignUp(capture(requestCaptor)) } coAnswers {
            latch.countDown()
            mockk()
        }

        val expectedRequest: ConfirmSignUpRequest.Builder.() -> Unit = {
            clientId = "app Client Id"
            this.username = username
            this.confirmationCode = confirmationCode
            secretHash = "dummy Hash"
        }

        // WHEN
        plugin.confirmSignUp(username, confirmationCode, options, {}, {})
        assertTrue { latch.await(5, TimeUnit.SECONDS) }

        // THEN
        assertEquals(ConfirmSignUpRequest.invoke(expectedRequest), ConfirmSignUpRequest.invoke(requestCaptor.captured))
    }

    @Test
    fun `test signup code success`() {
        val latch = CountDownLatch(1)

        // GIVEN
        val onSuccess = mockk<Consumer<AuthSignUpResult>>()
        val onError = mockk<Consumer<AuthException>>()
        val username = "user"
        val confirmationCode = "123456"
        val options = AuthConfirmSignUpOptions.defaults()

        val resultCaptor = slot<AuthSignUpResult>()
        every { onSuccess.accept(capture(resultCaptor)) } answers { latch.countDown() }

        val currentAuthState = mockk<AuthState> {
            coEvery { authNState } returns AuthenticationState.Configured()
        }
        coEvery { authStateMachine.getCurrentState(captureLambda()) } coAnswers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        val expectedAuthSignUpResult = AuthSignUpResult(
            true,
            AuthNextSignUpStep(AuthSignUpStep.DONE, mapOf(), null),
            null
        )

        coEvery { authService.cognitoIdentityProviderClient?.confirmSignUp(captureLambda()) } coAnswers {
            ConfirmSignUpResponse.invoke { }
        }

        // WHEN
        plugin.confirmSignUp(username, confirmationCode, options, onSuccess, onError)
        assertTrue { latch.await(5, TimeUnit.SECONDS) }

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify(exactly = 1) { onSuccess.accept(resultCaptor.captured) }

        assertEquals(expectedAuthSignUpResult, resultCaptor.captured)
    }

    @Test
    fun `test resend signup code API with given arguments`() {
        val latch = CountDownLatch(1)

        // GIVEN
        val username = "user"

        val currentAuthState = mockk<AuthState> {
            coEvery { authNState } returns AuthenticationState.Configured()
        }
        coEvery { authStateMachine.getCurrentState(captureLambda()) } coAnswers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        val requestCaptor = slot<ResendConfirmationCodeRequest.Builder.() -> Unit>()
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
        assertEquals(
            ResendConfirmationCodeRequest.invoke(expectedRequest),
            ResendConfirmationCodeRequest.invoke(requestCaptor.captured)
        )
    }

    @Test
    fun `test resend signup code success`() {
        val latch = CountDownLatch(1)

        // GIVEN
        val onSuccess = mockk<Consumer<AuthSignUpResult>>()
        val onError = mockk<Consumer<AuthException>>()
        val username = "user"

        val resultCaptor = slot<AuthSignUpResult>()
        every { onSuccess.accept(capture(resultCaptor)) } answers { latch.countDown() }

        val currentAuthState = mockk<AuthState> {
            coEvery { authNState } returns AuthenticationState.Configured()
        }
        coEvery { authStateMachine.getCurrentState(captureLambda()) } coAnswers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        val deliveryDetails = mapOf(
            "DESTINATION" to "destination",
            "MEDIUM" to "EMAIL",
            "ATTRIBUTE" to "attributeName"
        )

        val expectedAuthSignUpResult = AuthSignUpResult(
            false,
            AuthNextSignUpStep(
                AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                mapOf(),
                AuthCodeDeliveryDetails(
                    deliveryDetails.getValue("DESTINATION"),
                    AuthCodeDeliveryDetails.DeliveryMedium.fromString(deliveryDetails.getValue("MEDIUM")),
                    deliveryDetails.getValue("ATTRIBUTE")
                )
            ),
            AuthUser("", username)
        )

        coEvery { authService.cognitoIdentityProviderClient?.resendConfirmationCode(captureLambda()) } coAnswers {
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
        assertTrue { latch.await(5, TimeUnit.SECONDS) }

        // THEN
        verify(exactly = 0) { onError.accept(any()) }
        verify(exactly = 1) { onSuccess.accept(resultCaptor.captured) }

        assertEquals(expectedAuthSignUpResult, resultCaptor.captured)
    }

    @Test
    fun `update user attribute fails when not in SignedIn state`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthUpdateAttributeResult>>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        currentState = AuthenticationState.NotConfigured()

        val resultCaptor = slot<AuthException.InvalidStateException>()
        every { onError.accept(capture(resultCaptor)) } answers { listenLatch.countDown() }

        // WHEN
        plugin.updateUserAttribute(
            AuthUserAttribute(AuthUserAttributeKey.email(), "test@test.com"),
            onSuccess,
            onError
        )

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        verify(exactly = 1) { onError.accept(resultCaptor.captured) }
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun `update user attributes fails when not in SignedIn state`() {
        // GIVEN
        val onSuccess = mockk<Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        currentState = AuthenticationState.NotConfigured()

        val resultCaptor = slot<AuthException.InvalidStateException>()
        every { onError.accept(capture(resultCaptor)) } answers { listenLatch.countDown() }

        // WHEN
        plugin.updateUserAttributes(
            mutableListOf(
                AuthUserAttribute(AuthUserAttributeKey.email(), "test@test.com"),
                AuthUserAttribute(AuthUserAttributeKey.nickname(), "test")
            ),
            AuthUpdateUserAttributesOptions.defaults(),
            onSuccess,
            onError
        )

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        verify(exactly = 1) { onError.accept(resultCaptor.captured) }
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun `update single user attribute with no attribute options and delivery code success`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthUpdateAttributeResult>>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        val currentAuthState = mockk<AuthState> {
            every { authNState } returns AuthenticationState.SignedIn(mockk())
            every { authZState } returns AuthorizationState.SessionEstablished(credentials)
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        coEvery {
            authService.cognitoIdentityProviderClient?.updateUserAttributes(any<UpdateUserAttributesRequest>())
        } returns UpdateUserAttributesResponse.invoke {
        }

        val slot = slot<AuthUpdateAttributeResult>()

        every {
            onSuccess.accept(capture(slot))
        } answers {
            listenLatch.countDown()
        }

        // WHEN
        plugin.updateUserAttribute(
            AuthUserAttribute(AuthUserAttributeKey.email(), "test@test.com"),
            onSuccess,
            onError
        )

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        coVerify(exactly = 1) { onSuccess.accept(slot.captured) }
        assertTrue(slot.captured.isUpdated, "attribute should be successfully updated")
        assertNotNull(slot.captured.nextStep, "next step should not be null")
        assertNull(slot.captured.nextStep.codeDeliveryDetails, "code delivery details should be null")
        assertEquals(
            slot.captured.nextStep.updateAttributeStep,
            AuthUpdateAttributeStep.DONE,
            "next step should be done"
        )
        coVerify(exactly = 0) { onError.accept(any()) }
    }

    @Test
    fun `update single user attribute with attribute options and no delivery code success`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthUpdateAttributeResult>>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        val currentAuthState = mockk<AuthState> {
            every { authNState } returns AuthenticationState.SignedIn(mockk())
            every { authZState } returns AuthorizationState.SessionEstablished(credentials)
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        coEvery {
            authService.cognitoIdentityProviderClient?.updateUserAttributes(any<UpdateUserAttributesRequest>())
        } returns UpdateUserAttributesResponse.invoke {
        }

        val slot = slot<AuthUpdateAttributeResult>()

        every {
            onSuccess.accept(capture(slot))
        } answers {
            listenLatch.countDown()
        }

        val builder = AWSCognitoAuthUpdateUserAttributeOptions.builder().metadata(
            mapOf("x" to "x", "y" to "y", "z" to "z")
        )
        // WHEN
        plugin.updateUserAttribute(
            AuthUserAttribute(AuthUserAttributeKey.email(), "test@test.com"),
            builder.build(),
            onSuccess,
            onError
        )

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        coVerify(exactly = 1) { onSuccess.accept(slot.captured) }
        assertTrue(slot.captured.isUpdated, "attribute should be successfully updated")
        assertNotNull(slot.captured.nextStep, "next step should not be null")
        assertNull(slot.captured.nextStep.codeDeliveryDetails, "code delivery details should be null")
        assertEquals(
            slot.captured.nextStep.updateAttributeStep,
            AuthUpdateAttributeStep.DONE,
            "next step should be done"
        )
        coVerify(exactly = 0) { onError.accept(any()) }
    }

    @Test
    fun `update user attributes with delivery code success`() {
        // GIVEN
        val onSuccess = mockk<Consumer<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        val currentAuthState = mockk<AuthState> {
            every { authNState } returns AuthenticationState.SignedIn(mockk())
            every { authZState } returns AuthorizationState.SessionEstablished(credentials)
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        coEvery {
            authService.cognitoIdentityProviderClient?.updateUserAttributes(any<UpdateUserAttributesRequest>())
        } returns UpdateUserAttributesResponse.invoke {
            codeDeliveryDetailsList = listOf(
                CodeDeliveryDetailsType.invoke {
                    attributeName = "email"
                    deliveryMedium = DeliveryMediumType.Email
                }
            )
        }

        val slot = slot<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>()

        every {
            onSuccess.accept(capture(slot))
        } answers {
            listenLatch.countDown()
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
            onError
        )

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        coVerify(exactly = 1) { onSuccess.accept(slot.captured) }
        assertEquals(slot.captured.size, 2)
        // nickname
        assertNotNull(slot.captured[AuthUserAttributeKey.nickname()], "nick name should be in result")
        assertTrue(
            slot.captured[AuthUserAttributeKey.nickname()]?.isUpdated ?: false,
            "nickname attribute should be successfully updated"
        )
        assertNotNull(
            slot.captured[AuthUserAttributeKey.nickname()]?.nextStep,
            "next step should not be null for nickname attribute"
        )
        assertNull(
            slot.captured[AuthUserAttributeKey.nickname()]?.nextStep?.codeDeliveryDetails,
            "code delivery details should be null for nickname attribute"
        )
        assertEquals(
            slot.captured[AuthUserAttributeKey.nickname()]?.nextStep?.updateAttributeStep,
            AuthUpdateAttributeStep.DONE,
            "next step for nickname attribute should be done"
        )

        // email
        assertNotNull(slot.captured[AuthUserAttributeKey.email()], "email should be in result")
        assertFalse(
            slot.captured[AuthUserAttributeKey.email()]?.isUpdated ?: false,
            "email attribute should not be successfully updated"
        )
        assertNotNull(
            slot.captured[AuthUserAttributeKey.email()]?.nextStep,
            "next step should not be null for email attribute"
        )
        assertNotNull(
            slot.captured[AuthUserAttributeKey.email()]?.nextStep?.codeDeliveryDetails,
            "code delivery details should not be null for email attribute"
        )
        assertEquals(
            slot.captured[AuthUserAttributeKey.email()]?.nextStep?.codeDeliveryDetails?.attributeName,
            "email",
            "email attribute should not be successfully updated"
        )
        assertEquals(
            slot.captured[AuthUserAttributeKey.email()]?.nextStep?.updateAttributeStep,
            AuthUpdateAttributeStep.CONFIRM_ATTRIBUTE_WITH_CODE,
            "next step for email attribute should be confirm_attribute_with_code"
        )
        coVerify(exactly = 0) { onError.accept(any()) }
    }

    @Test
    fun `confirm user attribute fails when not in SignedIn state`() {
        // GIVEN
        val onSuccess = mockk<Action>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        currentState = AuthenticationState.NotConfigured()

        every {
            onError.accept(AuthException.InvalidStateException())
        } answers {
            listenLatch.countDown()
        }

        // WHEN
        plugin.confirmUserAttribute(
            AuthUserAttributeKey.email(),
            "000000",
            onSuccess,
            onError
        )

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        coVerify(exactly = 1) { onError.accept(AuthException.InvalidStateException()) }
        verify(exactly = 0) { onSuccess.call() }
    }

    @Test
    fun `confirm user attribute fails when access token is invalid`() {
        // GIVEN
        val onSuccess = mockk<Action>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        val invalidCredentials = AmplifyCredential.UserPool(
            CognitoUserPoolTokens(null, null, null, 120L)
        )

        val currentAuthState = mockk<AuthState> {
            every { authNState } returns AuthenticationState.SignedIn(mockk())
            every { authZState } returns AuthorizationState.SessionEstablished(invalidCredentials)
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        every {
            onError.accept(AuthException.InvalidUserPoolConfigurationException())
        } answers {
            listenLatch.countDown()
        }

        // WHEN
        plugin.confirmUserAttribute(
            AuthUserAttributeKey.email(),
            "000000",
            onSuccess,
            onError
        )

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        coVerify(exactly = 1) { onError.accept(AuthException.InvalidUserPoolConfigurationException()) }
        verify(exactly = 0) { onSuccess.call() }
    }

    @Test
    fun `confirm user attributes with cognito api call error`() {
        // GIVEN
        val onSuccess = mockk<Action>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>()
        val listenLatch = CountDownLatch(1)

        val currentAuthState = mockk<AuthState> {
            every { authNState } returns AuthenticationState.SignedIn(mockk())
            every { authZState } returns AuthorizationState.SessionEstablished(credentials)
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        val expectedException = CognitoIdentityProviderException("Some Cognito Message")
        coEvery {
            authService.cognitoIdentityProviderClient?.verifyUserAttribute(any<VerifyUserAttributeRequest>())
        } answers {
            throw expectedException
        }

        val resultCaptor = slot<AuthException>()
        every { onError.accept(capture(resultCaptor)) } answers { listenLatch.countDown() }

        plugin.confirmUserAttribute(
            AuthUserAttributeKey.email(),
            "000000",
            onSuccess,
            onError
        )

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        assertEquals(expectedException, resultCaptor.captured.cause)
        coVerify(exactly = 1) { onError.accept(resultCaptor.captured) }
        coVerify(exactly = 0) { onSuccess.call() }
    }

    @Test
    fun `confirm user attributes with success`() {
        // GIVEN
        val onSuccess = mockk<Action>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        val currentAuthState = mockk<AuthState> {
            every { authNState } returns AuthenticationState.SignedIn(mockk())
            every { authZState } returns AuthorizationState.SessionEstablished(credentials)
        }
        every { authStateMachine.getCurrentState(captureLambda()) } answers {
            lambda<(AuthState) -> Unit>().invoke(currentAuthState)
        }

        coEvery {
            authService.cognitoIdentityProviderClient?.verifyUserAttribute(any<VerifyUserAttributeRequest>())
        } returns VerifyUserAttributeResponse.invoke {
        }
        every {
            onSuccess.call()
        } answers {
            listenLatch.countDown()
        }

        plugin.confirmUserAttribute(
            AuthUserAttributeKey.email(),
            "000000",
            onSuccess,
            onError
        )

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
        coVerify(exactly = 1) { onSuccess.call() }
        coVerify(exactly = 0) { onError.accept(any()) }
    }
}
