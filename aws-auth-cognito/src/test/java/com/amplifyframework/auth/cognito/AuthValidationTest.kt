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
import com.amplifyframework.auth.cognito.featuretest.generators.authstategenerators.AuthStateJsonGenerator.DUMMY_TOKEN
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.usecases.SignOutUseCase
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.core.Consumer
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignUpState
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

private const val CONFIGURATION_PATH = "/feature-test/configuration/authconfiguration_oauth.json"
private const val USERNAME_1 = "username"
private const val USERNAME_2 = "username2"
private const val PASSWORD_1 = "password"
private const val PASSWORD_2 = "password2"
private const val INCORRECT_PASSWORD = "wrong"
private const val INVALID_USERNAME = "invalid"
private const val USER_ID = "userId"

private val validLogins = mapOf(
    USERNAME_1 to PASSWORD_1,
    USERNAME_2 to PASSWORD_2
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
            idToken = DUMMY_TOKEN,
            accessToken = DUMMY_TOKEN,
            refreshToken = DUMMY_TOKEN,
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
            authZState = AuthorizationState.Configured(),
            authSignUpState = SignUpState.NotStarted()
        )
    )

    private val plugin = RealAWSCognitoAuthPlugin(
        configuration = configuration,
        authEnvironment = environment,
        authStateMachine = stateMachine,
        logger = logger
    )

    private val signOutUseCase = SignOutUseCase(
        stateMachine = stateMachine
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
        signIn(USERNAME_1, PASSWORD_1)
        assertSignedInAs(USERNAME_1)
        signOut()
        assertSignedOut()
    }

    // SRP 2
    // Expected: Sign in with incorrect password fails
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, SRP sign in same user with incorrect password`() {
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        assertSignedOut()
        assertFails { signIn(USERNAME_1, INCORRECT_PASSWORD) }
    }

    // SPR 3
    // Expected: Each action is successful
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, SRP sign in different user with correct password`() {
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        assertSignedOut()
        signIn(USERNAME_2, PASSWORD_2)
        assertSignedInAs(USERNAME_2)
    }

    // SPR 4
    // Expected: Sign in with incorrect password fails
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, SRP sign in different user with incorrect password`() {
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        assertSignedOut()
        assertFails { signIn(USERNAME_2, INCORRECT_PASSWORD) }
    }

    // SPR 5
    // Expected: Sign in with non-existent user fails
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, SRP sign in non-existent user`() {
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        assertSignedOut()
        assertFails { signIn(INVALID_USERNAME, PASSWORD_1) }
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
        signIn(USERNAME_1, PASSWORD_1)
        assertFails { signInHostedUi() }
        assertSignedInAs(USERNAME_1)
    }

    // SRP/Hosted 2
    // Expected: Hosted UI sign in succeeded, SRP sign in fails, user is still signed in
    @Test
    fun `Hosted UI sign in, SRP sign in existing user with correct password`() {
        signInHostedUi()
        assertFails { signIn(USERNAME_1, PASSWORD_1) }
        assertSignedInAs(USERNAME_1)
    }

    // SRP/Hosted 3
    // Expected: Hosted UI sign in succeeded, SRP sign in fails, user is still signed in
    @Test
    fun `Hosted UI sign in, SRP sign in existing user with incorrect password`() {
        signInHostedUi()
        assertFails { signIn(USERNAME_1, INCORRECT_PASSWORD) }
        assertSignedInAs(USERNAME_1)
    }

    // SRP/Hosted 4
    // Expected: Hosted UI sign in succeeded, SRP sign in fails, user is still signed in
    @Test
    fun `Hosted UI sign in, SRP sign in non-existent user`() {
        signInHostedUi()
        assertFails { signIn(INVALID_USERNAME, PASSWORD_1) }
    }

    // SRP/Hosted 5
    // Expected: Each action is successful
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in`() {
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        signInHostedUi()
        assertSignedInAs(USERNAME_1)
    }

    // SRP/Hosted 6
    // Expected: Each action is successful
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password`() {
        signInHostedUi()
        signOutHostedUi()
        signIn(USERNAME_1, PASSWORD_1)
        assertSignedInAs(USERNAME_1)
    }

    // SRP/Hosted 7
    // Expected: SRP sign in with incorrect password fails
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with incorrect password`() {
        signInHostedUi()
        signOutHostedUi()
        assertFails { signIn(USERNAME_1, INCORRECT_PASSWORD) }
    }

    // SRP/Hosted 8
    // Expected: SRP sign in with non-existent user fails
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in non-existent user`() {
        signInHostedUi()
        signOutHostedUi()
        assertFails { signIn(INVALID_USERNAME, PASSWORD_1) }
    }

    // SRP/Hosted 9
    // Expected: Each action is successful
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password, SRP sign out, SRP sign in same user with correct password`() {
        signInHostedUi()
        signOutHostedUi()
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        signIn(USERNAME_1, PASSWORD_1)
        assertSignedInAs(USERNAME_1)
    }

    // SRP/Hosted 10
    // Expected: SRP sign in with incorrect password fails
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password, SRP sign out, SRP sign in same user with incorrect password`() {
        signInHostedUi()
        signOutHostedUi()
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        assertFails { signIn(USERNAME_1, INCORRECT_PASSWORD) }
    }

    // SRP/Hosted 11
    // Expected: Each action is successful
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password, SRP sign out, SRP sign in different user with correct password`() {
        signInHostedUi()
        signOutHostedUi()
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        assertSignedOut()
        signIn(USERNAME_2, PASSWORD_2)
        assertSignedInAs(USERNAME_2)
    }

    // SRP/Hosted 12
    // Expected: SRP sign in different user with incorrect password fails
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password, SRP sign out, SRP sign in different user with incorrect password`() {
        signInHostedUi()
        signOutHostedUi()
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        assertFails { signIn(USERNAME_2, INCORRECT_PASSWORD) }
    }

    // SRP/Hosted 13
    // Expected: SRP sign in with non-existent user fails
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Hosted UI sign in, Hosted UI sign out, SRP sign in existing user with correct password, SRP sign out, SRP sign in non-existent user`() {
        signInHostedUi()
        signOutHostedUi()
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        assertFails { signIn(INVALID_USERNAME, PASSWORD_1) }
    }

    // SRP/Hosted 14
    // Expected: Each action is successful
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun ` SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in, Hosted UI sign out, SRP sign in same user with correct password`() {
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        signInHostedUi()
        signOutHostedUi()
        signIn(USERNAME_1, PASSWORD_1)
    }

    // SRP/Hosted 15
    // Expected: Last SRP sign in with incorrect password fails
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in, Hosted UI sign out, SRP sign in same user with incorrect password`() {
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        signInHostedUi()
        signOutHostedUi()
        assertFails { signIn(USERNAME_1, INCORRECT_PASSWORD) }
    }

    // SRP/Hosted 16
    // Expected: Each action is successful
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in, Hosted UI sign out, SRP sign in different user with correct password`() {
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        signInHostedUi()
        signOutHostedUi()
        signIn(USERNAME_2, PASSWORD_2)
    }

    // SRP/Hosted 17
    // Expected: Last SRP sign in with incorrect password fails
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in, Hosted UI sign out, SRP sign in different user with incorrect password`() {
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        signInHostedUi()
        signOutHostedUi()
        assertFails { signIn(USERNAME_2, INCORRECT_PASSWORD) }
    }

    // SRP/Hosted 18
    // Expected: SRP sign in with non-existent user fails
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `SRP sign in existing user with correct password, SRP sign out, Hosted UI sign in, Hosted UI sign out, SRP sign in non-existent user`() {
        signIn(USERNAME_1, PASSWORD_1)
        signOut()
        signInHostedUi()
        signOutHostedUi()
        assertFails { signIn(INVALID_USERNAME, PASSWORD_1) }
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

    private fun signOut() = runBlocking { withTimeout(10000L) { signOutUseCase.execute() } }

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

    private fun signOutHostedUi() = signOut()

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

    private fun <T> blockForResult(timeoutMillis: Long = 100000, function: (complete: Consumer<T>) -> Unit): T =
        runBlockingWithTimeout(timeoutMillis) { continuation -> function { continuation.resume(it) } }

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
            this::class.java.getResource(CONFIGURATION_PATH)
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
            "USER_ID_FOR_SRP" to USER_ID
        )
        every { session } returns "session"
    }

    private val mockRespondToAuthChallengeSuccessResponse = mockk<RespondToAuthChallengeResponse>(relaxed = true) {
        every { authenticationResult } returns mockk {
            every { idToken } returns DUMMY_TOKEN
            every { accessToken } returns DUMMY_TOKEN
            every { refreshToken } returns DUMMY_TOKEN
            every { expiresIn } returns 300
            every { newDeviceMetadata } returns null
        }
    }

    @Test
    fun `test getActiveUsername returns correct username when active and userIDforSRP is null`() {
        val username = AuthHelper.getActiveUsername("username", null, null)
        assertEquals(username, "username")
    }

    @Test
    fun `getActiveUsername returns correct username when userIDforSRP is null & alternate is same as username`() {
        val username = AuthHelper.getActiveUsername("username", "username", null)
        assertEquals(username, "username")
    }

    @Test
    fun `getActiveUsername returns correct username when userIDforSRP is null & alternate is different as username`() {
        val username = AuthHelper.getActiveUsername(
            "username",
            "userID12322",
            null
        )
        assertEquals(username, "userID12322")
    }

    @Test
    fun `test getActiveUsername returns correct username when userIDforSRP is not null null and alternate is null`() {
        val username = AuthHelper.getActiveUsername(
            "username",
            null,
            "userID12322"
        )
        assertEquals(username, "userID12322")
    }
}
