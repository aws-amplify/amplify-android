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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateUserAttributesRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateUserAttributesResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifyUserAttributeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifyUserAttributeResponse
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.usecases.ResetPasswordUseCase
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult
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
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class RealAWSCognitoAuthPluginTest {

    private val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IkpvaG4gRG9lIiwiZXhwIjoxNTE2Mj" +
        "M5MDIyfQ.e4RpZTfAb3oXkfq3IwHtR_8Zhn0U1JDV7McZPlBXyhw"

    private var logger = mockk<Logger>(relaxed = true)
    private val appClientId = "topSecretClient"
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
    fun `update user attribute fails when not in SignedIn state`() {
        // GIVEN
        val onSuccess = mockk<Consumer<AuthUpdateAttributeResult>>(relaxed = true)
        val onError = mockk<Consumer<AuthException>>(relaxed = true)
        val listenLatch = CountDownLatch(1)

        currentState = AuthenticationState.NotConfigured()

        every {
            onError.accept(AuthException.InvalidStateException())
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
        coVerify(exactly = 1) { onError.accept(AuthException.InvalidStateException()) }
        verify(exactly = 0) { onSuccess.accept(any()) }
    }

    @Test
    fun `update user attribute with success`() {
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

        every {
            onSuccess.accept(any<AuthUpdateAttributeResult>())
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
        coVerify(exactly = 1) { onSuccess.accept(any<AuthUpdateAttributeResult>()) }
        coVerify(exactly = 0) { onError.accept(any()) }
    }

    @Test
    fun `update user attributes with success`() {
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
        }

        every {
            onSuccess.accept(any<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>())
        } answers {
            listenLatch.countDown()
        }

        // WHEN
        plugin.updateUserAttributes(
            mutableListOf(
                AuthUserAttribute(AuthUserAttributeKey.email(), "banjijolly@gmail.com"),
                AuthUserAttribute(AuthUserAttributeKey.nickname(), "test")
            ),
            AuthUpdateUserAttributesOptions.defaults(),
            onSuccess,
            onError
        )

        assertTrue { listenLatch.await(60, TimeUnit.SECONDS) }
        coVerify(exactly = 1) { onSuccess.accept(any<MutableMap<AuthUserAttributeKey, AuthUpdateAttributeResult>>()) }
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

        assertTrue { listenLatch.await(60, TimeUnit.SECONDS) }
        coVerify(exactly = 1) { onSuccess.call() }
        coVerify(exactly = 0) { onError.accept(any()) }
    }
}
