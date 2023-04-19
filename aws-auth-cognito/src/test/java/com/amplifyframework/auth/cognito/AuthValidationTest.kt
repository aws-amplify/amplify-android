/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.app.Activity
import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InvalidPasswordException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotFoundException
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.featuretest.generators.authstategenerators.AuthStateJsonGenerator.dummyToken
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.core.Consumer
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val configurationPath = "/feature-test/configuration/authconfiguration_oauth.json"
private const val username1 = "username"
private const val username2 = "username2"
private const val password1 = "password"
private const val password2 = "password2"
private const val incorrectPassword = "wrong"
private const val invalidUsername = "invalid"
private const val userId = "userId"

private val validLogins = mapOf(
    username1 to password1,
    username2 to password2
)

/**
 * Contains validation tests for the SRP + Hosted UI test matrix.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class AuthValidationTest {

    private val logger = mockk<Logger>(relaxed = true)

    private val configuration = loadConfiguration()
    private val identityClient = mockk<CognitoIdentityClient>()
    private val identityProviderClient = mockk<CognitoIdentityProviderClient>()
    private val hostedUIClient = mockk<HostedUIClient> {
        every { fetchToken(any()) } returns CognitoUserPoolTokens(
            idToken = dummyToken,
            accessToken = dummyToken,
            refreshToken = dummyToken,
            expiration = 300
        )
        every { createSignOutUri() } returns mockk()
    }

    private val activity = mockk<Activity>()

    private val environment = AuthEnvironment(
        context = mockk(),
        configuration = configuration,
        cognitoAuthService = mockk {
            every { cognitoIdentityClient } returns identityClient
            every { cognitoIdentityProviderClient } returns identityProviderClient
        },
        credentialStoreClient = mockk(relaxed = true) {
            coEvery { loadCredentials(any()) } returns AmplifyCredential.DeviceData(DeviceMetadata.Empty)
        },
        userContextDataProvider = null,
        hostedUIClient = hostedUIClient,
        logger = logger
    )
    private val stateMachine = AuthStateMachine(
        environment,
        initialState = AuthState.Configured(
            authNState = AuthenticationState.SignedOut(signedOutData = SignedOutData()),
            authZState = AuthorizationState.Configured()
        )
    )

    private val plugin = RealAWSCognitoAuthPlugin(
        configuration = configuration,
        authEnvironment = environment,
        authStateMachine = stateMachine,
        logger = logger
    )

    private val mainThreadSurrogate = newSingleThreadContext("Main thread")

//region Setup/Teardown

    @Before
    fun setup() {
        Dispatchers.setMain(mainThreadSurrogate)

        mockkStatic("com.amplifyframework.auth.cognito.AWSCognitoAuthSessionKt")
        every { any<AmplifyCredential>().isValid() } returns true

        mockkObject(AuthHelper)
        coEvery { AuthHelper.getSecretHash(any(), any(), any()) } returns "a hash"
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

//endregion

//region SRP

    // SRP 1
    // Expected: Sign in and sign out successful
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out`() {
        signIn(username1, password1)
        assertSignedInAs(username1)
        signOut()
        assertSignedOut()
    }

    // SRP 2
    // Expected: Sign in with incorrect password fails
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, SRP sign in same user with incorrect password`() { // ktlint-disable max-line-length
        signIn(username1, password1)
        signOut()
        assertSignedOut()
        assertFails { signIn(username1, incorrectPassword) }
    }

    // SPR 3
    // Expected: Each action is successful
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, SRP sign in different user with correct password`() { // ktlint-disable max-line-length
        signIn(username1, password1)
        signOut()
        assertSignedOut()
        signIn(username2, password2)
        assertSignedInAs(username2)
    }

    // SPR 4
    // Expected: Sign in with incorrect password fails
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, SRP sign in different user with incorrect password`() { // ktlint-disable max-line-length
        signIn(username1, password1)
        signOut()
        assertSignedOut()
        assertFails { signIn(username2, incorrectPassword) }
    }

    // SPR 5
    // Expected: Sign in with non-existent user fails
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, SRP sign in non-existent user`() {
        signIn(username1, password1)
        signOut()
        assertSignedOut()
        assertFails { signIn(invalidUsername, password1) }
    }

//endregion
//region HostedUI

    // Hosted 1
    // Expected: Sign in and sign out successful
    @Test
    fun `Hosted UI sign in, Hosted UI sign out`() {
        val result = signInHostedUi()
        assertTrue(result.isSignedIn)
        signOutHostedUi()
        assertSignedOut()
    }

    // Hosted 2
    // Expected: Second sign in, user is auto signed-in
    @Test
    fun ` Hosted UI sign in, Hosted UI sign out, Hosted UI sign in`() {
        signInHostedUi()
        signOutHostedUi()
        assertSignedOut()
        val result = signInHostedUi()
        assertTrue(result.isSignedIn)
    }

//endregion
//region SRP and HostedUI

    // SRP/Hosted 1
    // Expected: SRP sign in succeeded, Hosted UI sign in fails, user is still signed in
    @Test
    fun `SRP sign in existing user with correct password, Hosted UI sign in`() {
        signIn(username1, password1)
        assertFails { signInHostedUi() }
        assertSignedInAs(username1)
    }

    // SRP/Hosted 2
    // Expected: Hosted UI sign in succeeded, SRP sign in fails, user is still signed in
    @Test
    fun `Hosted UI sign in, SRP sign in existing user with correct password`() {
        signInHostedUi()
        assertFails { signIn(username1, password1) }
        assertSignedInAs(username1)
    }

    // SRP/Hosted 3
    // Expected: Hosted UI sign in succeeded, SRP sign in fails, user is still signed in
    @Test
    fun `Hosted UI sign in, SRP sign in existing user with incorrect password`() {
        signInHostedUi()
        assertFails { signIn(username1, incorrectPassword) }
        assertSignedInAs(username1)
    }

    // SRP/Hosted 4
    // Expected: Hosted UI sign in succeeded, SRP sign in fails, user is still signed in
    @Test
    fun `Hosted UI sign in, SRP sign in non-existent user`() {
        signInHostedUi()
        assertFails { signIn(invalidUsername, password1) }
    }

    // SRP/Hosted 5
    // Expected: Each action is successful
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in`() {
        signIn(username1, password1)
        signOut()
        signInHostedUi()
        assertSignedInAs(username1)
    }

    // SRP/Hosted 6
    // Expected: Each action is successful
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password`() {
        signInHostedUi()
        signOutHostedUi()
        signIn(username1, password1)
        assertSignedInAs(username1)
    }

    // SRP/Hosted 7
    // Expected: SRP sign in with incorrect password fails
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with incorrect password`() {
        signInHostedUi()
        signOutHostedUi()
        assertFails { signIn(username1, incorrectPassword) }
    }

    // SRP/Hosted 8
    // Expected: SRP sign in with non-existent user fails
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in non-existent user`() {
        signInHostedUi()
        signOutHostedUi()
        assertFails { signIn(invalidUsername, password1) }
    }

    // SRP/Hosted 9
    // Expected: Each action is successful
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password, SRP sign out, SRP sign in same user with correct password`() { // ktlint-disable max-line-length
        signInHostedUi()
        signOutHostedUi()
        signIn(username1, password1)
        signOut()
        signIn(username1, password1)
        assertSignedInAs(username1)
    }

    // SRP/Hosted 10
    // Expected: SRP sign in with incorrect password fails
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password, SRP sign out, SRP sign in same user with incorrect password`() { // ktlint-disable max-line-length
        signInHostedUi()
        signOutHostedUi()
        signIn(username1, password1)
        signOut()
        assertFails { signIn(username1, incorrectPassword) }
    }

    // SRP/Hosted 11
    // Expected: Each action is successful
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password, SRP sign out, SRP sign in different user with correct password`() { // ktlint-disable max-line-length
        signInHostedUi()
        signOutHostedUi()
        signIn(username1, password1)
        signOut()
        assertSignedOut()
        signIn(username2, password2)
        assertSignedInAs(username2)
    }

    // SRP/Hosted 12
    // Expected: SRP sign in different user with incorrect password fails
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password, SRP sign out, SRP sign in different user with incorrect password`() { // ktlint-disable max-line-length
        signInHostedUi()
        signOutHostedUi()
        signIn(username1, password1)
        signOut()
        assertFails { signIn(username2, incorrectPassword) }
    }

    // SRP/Hosted 13
    // Expected: SRP sign in with non-existent user fails
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password, SRP sign out, SRP sign in non-existent user`() { // ktlint-disable max-line-length
        signInHostedUi()
        signOutHostedUi()
        signIn(username1, password1)
        signOut()
        assertFails { signIn(invalidUsername, password1) }
    }

    // SRP/Hosted 14
    // Expected: Each action is successful
    @Test
    fun ` SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in, Hosted UI sign out, SRP sign in same user with correct password`() { // ktlint-disable max-line-length
        signIn(username1, password1)
        signOut()
        signInHostedUi()
        signOutHostedUi()
        signIn(username1, password1)
    }

    // SRP/Hosted 15
    // Expected: Last SRP sign in with incorrect password fails
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in, Hosted UI sign out, SRP sign in same user with incorrect password`() { // ktlint-disable max-line-length
        signIn(username1, password1)
        signOut()
        signInHostedUi()
        signOutHostedUi()
        assertFails { signIn(username1, incorrectPassword) }
    }

    // SRP/Hosted 16
    // Expected: Each action is successful
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in, Hosted UI sign out, SRP sign in different user with correct password`() { // ktlint-disable max-line-length
        signIn(username1, password1)
        signOut()
        signInHostedUi()
        signOutHostedUi()
        signIn(username2, password2)
    }

    // SRP/Hosted 17
    // Expected: Last SRP sign in with incorrect password fails
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in, Hosted UI sign out, SRP sign in different user with incorrect password`() { // ktlint-disable max-line-length
        signIn(username1, password1)
        signOut()
        signInHostedUi()
        signOutHostedUi()
        assertFails { signIn(username2, incorrectPassword) }
    }

    // SRP/Hosted 18
    // Expected: SRP sign in with non-existent user fails
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in, Hosted UI sign out, SRP sign in non-existent user`() { // ktlint-disable max-line-length
        signIn(username1, password1)
        signOut()
        signInHostedUi()
        signOutHostedUi()
        assertFails { signIn(invalidUsername, password1) }
    }

//endregion

    private fun signIn(username: String, password: String): AuthSignInResult {
        clearMocks(identityProviderClient)
        if (!validLogins.containsKey(username)) {
            setupMockResponseForInvalidUser()
        } else if (validLogins[username] != password) {
            setupMockResponseForIncorrectPassword(username)
        } else {
            setupMockResponseForSuccessfulSrp(username)
        }

        return blockForResult { success, error ->
            plugin.signIn(username, password, success, error)
        }
    }

    private fun signOut() = blockForResult { complete ->
        plugin.signOut(complete)
    }

    private fun signInHostedUi(): AuthSignInResult {
        every { hostedUIClient.launchCustomTabsSignIn(any()) } answers {
            GlobalScope.launch(mainThreadSurrogate) {
                plugin.handleWebUISignInResponse(
                    mockk { every { data } returns mockk() }
                )
            }
        }
        return blockForResult { success, error ->
            plugin.signInWithWebUI(activity, success, error)
        }
    }

    private fun signOutHostedUi() = blockForResult { complete ->
        plugin.signOut(complete)
    }

    private fun assertSignedOut() {
        val result = blockForResult { continuation -> stateMachine.getCurrentState { continuation.accept(it) } }
        assertTrue(result.authNState is AuthenticationState.SignedOut)
    }

    private fun assertSignedInAs(username: String) {
        val result = blockForResult { continuation -> stateMachine.getCurrentState { continuation.accept(it) } }
        val state = result.authNState
        assertTrue(state is AuthenticationState.SignedIn)
        assertEquals(username, state.signedInData.username)
    }

    private fun <T> blockForResult(
        timeoutMillis: Long = 100000,
        function: (success: Consumer<T>, error: Consumer<AuthException>) -> Unit
    ): T = runBlockingWithTimeout(timeoutMillis) { continuation ->
        function(
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }
        )
    }

    private fun <T> blockForResult(
        timeoutMillis: Long = 100000,
        function: (complete: Consumer<T>) -> Unit
    ): T = runBlockingWithTimeout(timeoutMillis) { continuation -> function { continuation.resume(it) } }

    // Helper that runs the supplied function in a coroutine, blocking the thread until the continuation is invoked or
    // the timeout is reached
    private fun <T> runBlockingWithTimeout(
        timeoutMillis: Long,
        function: (continuation: CancellableContinuation<T>) -> Unit
    ): T = runBlocking {
        withTimeout(timeoutMillis) { suspendCancellableCoroutine(function) }
    }

    private fun setupMockResponseForInvalidUser() {
        coEvery { identityProviderClient.initiateAuth(any()) } throws mockk<UserNotFoundException>()
    }

    private fun setupMockResponseForIncorrectPassword(username: String) {
        coEvery { identityProviderClient.initiateAuth(any()) } returns mockInitiateAuthSuccessResponse(username)
        coEvery { identityProviderClient.respondToAuthChallenge(any()) } throws mockk<InvalidPasswordException>()
    }

    private fun setupMockResponseForSuccessfulSrp(username: String) {
        coEvery { identityProviderClient.initiateAuth(any()) } returns mockInitiateAuthSuccessResponse(username)
        coEvery { identityProviderClient.respondToAuthChallenge(any()) } returns
            mockRespondToAuthChallengeSuccessResponse
    }

    private fun loadConfiguration(): AuthConfiguration {
        val configFileUrl =
            this::class.java.getResource(configurationPath)
        val configJSONObject =
            JSONObject(File(configFileUrl!!.file).readText())
                .getJSONObject("auth")
                .getJSONObject("plugins")
                .getJSONObject("awsCognitoAuthPlugin")
        return AuthConfiguration.fromJson(configJSONObject)
    }

    private fun mockInitiateAuthSuccessResponse(username: String) = mockk<InitiateAuthResponse> {
        every { challengeName } returns ChallengeNameType.PasswordVerifier
        every { challengeParameters } returns mapOf(
            "SALT" to "abc",
            "SECRET_BLOCK" to "secretBlock",
            "SRP_B" to "def",
            "USERNAME" to username,
            "USER_ID_FOR_SRP" to userId
        )
        every { session } returns "session"
    }

    private val mockRespondToAuthChallengeSuccessResponse = mockk<RespondToAuthChallengeResponse>(relaxed = true) {
        every { authenticationResult } returns mockk {
            every { idToken } returns dummyToken
            every { accessToken } returns dummyToken
            every { refreshToken } returns dummyToken
            every { expiresIn } returns 300
            every { newDeviceMetadata } returns null
        }
    }
}
