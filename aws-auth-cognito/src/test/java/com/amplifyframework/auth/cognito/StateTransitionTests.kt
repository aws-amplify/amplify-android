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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.StateChangeListenerToken
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInData
import com.amplifyframework.statemachine.codegen.data.SignOutData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.CustomSignInState
import com.amplifyframework.statemachine.codegen.states.DeleteUserState
import com.amplifyframework.statemachine.codegen.states.DeviceSRPSignInState
import com.amplifyframework.statemachine.codegen.states.FetchAuthSessionState
import com.amplifyframework.statemachine.codegen.states.HostedUISignInState
import com.amplifyframework.statemachine.codegen.states.MigrateSignInState
import com.amplifyframework.statemachine.codegen.states.RefreshSessionState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SetupTOTPState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StateTransitionTests : StateTransitionTestBase() {

    private val mainThreadSurrogate = newSingleThreadContext("Main thread")

    internal lateinit var stateMachine: AuthStateMachine

    @Mock
    private lateinit var storeClient: CredentialStoreClient

    @Before
    fun setUp() {
        setupAuthActions()
        setupAuthNActions()
        setupAuthZActions()
        setupSignInActions()
        setupSRPActions()
        setupSignOutActions()
        setupFetchAuthActions()
        setupStateMachine()
        Dispatchers.setMain(mainThreadSurrogate)
    }

    private fun setupStateMachine() {
        stateMachine = AuthStateMachine(
            AuthState.Resolver(
                AuthenticationState.Resolver(
                    SignInState.Resolver(
                        SRPSignInState.Resolver(mockSRPActions),
                        CustomSignInState.Resolver(mockSignInCustomActions),
                        MigrateSignInState.Resolver(mockMigrateAuthActions),
                        SignInChallengeState.Resolver(mockSignInChallengeActions),
                        HostedUISignInState.Resolver(mockHostedUIActions),
                        DeviceSRPSignInState.Resolver(mockDeviceSRPSignInActions),
                        SetupTOTPState.Resolver(mockSetupTOTPActions),
                        mockSignInActions
                    ),
                    SignOutState.Resolver(mockSignOutActions),
                    mockAuthenticationActions
                ),
                AuthorizationState.Resolver(
                    FetchAuthSessionState.Resolver(mockFetchAuthSessionActions),
                    RefreshSessionState.Resolver(
                        FetchAuthSessionState.Resolver(mockFetchAuthSessionActions),
                        mockFetchAuthSessionActions
                    ),
                    DeleteUserState.Resolver(mockDeleteUserActions),
                    mockAuthorizationActions
                ),
                mockAuthActions
            ),
            AuthEnvironment(mockk(), configuration, cognitoAuthService, storeClient, null, null, mockk())
        )
    }

    private fun setupConfigureSignedIn() {
        Mockito.`when`(
            mockAuthenticationActions.configureAuthenticationAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.InitializedSignedIn(signedInData, DeviceMetadata.Empty)
                        )
                    )
                    dispatcher.send(
                        AuthEvent(
                            AuthEvent.EventType.ConfiguredAuthentication(configuration, credentials)
                        )
                    )
                }
            )

        Mockito.`when`(
            mockAuthActions.initializeAuthorizationConfigurationAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthorizationEvent(AuthorizationEvent.EventType.CachedCredentialsAvailable(credentials))
                    )
                }
            )
    }

    private fun setupConfigureSignedOut() {
        Mockito.`when`(
            mockAuthenticationActions.configureAuthenticationAction(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        AuthenticationEvent(AuthenticationEvent.EventType.InitializedSignedOut(SignedOutData()))
                    )
                    dispatcher.send(
                        AuthEvent(AuthEvent.EventType.ConfiguredAuthentication(configuration, credentials))
                    )
                }
            )
    }

    private fun setupRevokeTokenSignOut() {
        Mockito.`when`(
            mockAuthenticationActions.initiateSignOutAction(
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject()
            )
        ).thenReturn(
            Action { dispatcher, _ ->
                dispatcher.send(
                    SignOutEvent(
                        SignOutEvent.EventType.RevokeToken(signedInData)
                    )
                )
            }
        )
    }

    private fun setupLocalSignOut() {
        Mockito.`when`(
            mockAuthenticationActions.initiateSignOutAction(
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject()
            )
        ).thenReturn(
            Action { dispatcher, _ ->
                dispatcher.send(
                    SignOutEvent(
                        SignOutEvent.EventType.SignOutLocally(signedInData)
                    )
                )
            }
        )
    }

    @After
    fun tearDown() {
        // reset main dispatcher to the original Main dispatcher
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    @Test
    fun getDefaultState() {
        val testLatch = CountDownLatch(1)
        stateMachine.getCurrentState {
            assertEquals(AuthState.NotConfigured(""), it)
            testLatch.countDown()
        }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testConfigureSignedOut() {
        setupConfigureSignedOut()
        val listenLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            {
                if (it is AuthState.Configured &&
                    it.authNState is AuthenticationState.SignedOut &&
                    it.authZState is AuthorizationState.Configured
                ) {
                    stateMachine.cancel(token)
                    listenLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        val configure = AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        stateMachine.send(configure)

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testConfigureSignedIn() {
        setupConfigureSignedIn()

        val listenLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            {
                if (it is AuthState.Configured &&
                    it.authNState is AuthenticationState.SignedIn &&
                    it.authZState is AuthorizationState.SessionEstablished
                ) {
                    stateMachine.cancel(token)
                    listenLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        val configure =
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        stateMachine.send(configure)

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testSignInSRP() {
        setupConfigureSignedOut()

        Mockito.`when`(mockAuthenticationActions.initiateSignInAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        SignInEvent(
                            SignInEvent.EventType.InitiateSignInWithSRP("username", "password", emptyMap())
                        )
                    )
                }
            )

        val testLatch = CountDownLatch(1)
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            {
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedOut }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.SignInRequested(
                                SignInData.SRPSignInData(
                                    "username",
                                    "password",
                                    emptyMap()
                                )
                            )
                        )
                    )
                }
                val authNState = it.authNState.takeIf { itN ->
                    itN is AuthenticationState.SignedIn && it.authZState is AuthorizationState.SessionEstablished
                }
                authNState?.apply {
                    stateMachine.cancel(token)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        )

        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testSignInWithCustomWithRetry() {
        setupConfigureSignedOut()
        setupSignInActionWithCustomAuth()
        setupCustomAuthActions()

        Mockito.`when`(mockAuthenticationActions.initiateSignInAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        SignInEvent(
                            SignInEvent.EventType.InitiateSignInWithCustom(
                                "username",
                                mapOf()
                            )
                        )
                    )
                }
            )

        val testLatch = CountDownLatch(1)
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            {
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedOut }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.SignInRequested(
                                SignInData.CustomAuthSignInData(
                                    "username",
                                    emptyMap()
                                )
                            )
                        )
                    )
                }

                val signInState = (it.authNState as? AuthenticationState.SigningIn)?.signInState
                val challengeState = signInState?.challengeState.takeIf { signInChallengeState ->
                    signInChallengeState is SignInChallengeState.WaitingForAnswer
                }
                challengeState?.apply {
                    stateMachine.send(
                        SignInChallengeEvent(
                            SignInChallengeEvent.EventType.RetryVerifyChallengeAnswer(
                                "test",
                                mapOf(),
                                AuthChallenge(
                                    ChallengeNameType.CustomChallenge.toString(),
                                    "Test",
                                    "session_mock_value",
                                    mapOf()
                                )
                            )
                        )
                    )
                    stateMachine.send(
                        SignInChallengeEvent(
                            SignInChallengeEvent.EventType.VerifyChallengeAnswer(
                                "test",
                                mapOf()
                            )
                        )
                    )
                }

                val authNState =
                    it.authNState.takeIf { itN ->
                        itN is AuthenticationState.SignedIn && it.authZState is AuthorizationState.SessionEstablished
                    }
                authNState?.apply {
                    stateMachine.cancel(token)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        )

        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testSignInWithCustom() {
        setupConfigureSignedOut()
        setupSignInActionWithCustomAuth()
        setupCustomAuthActions()

        Mockito.`when`(mockAuthenticationActions.initiateSignInAction(MockitoHelper.anyObject()))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        SignInEvent(
                            SignInEvent.EventType.InitiateSignInWithCustom(
                                "username",
                                mapOf()
                            )
                        )
                    )
                }
            )

        val testLatch = CountDownLatch(1)
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            {
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedOut }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.SignInRequested(
                                SignInData.CustomAuthSignInData(
                                    "username",
                                    emptyMap()
                                )
                            )
                        )
                    )
                }

                val signInState = (it.authNState as? AuthenticationState.SigningIn)?.signInState
                val challengeState = signInState?.challengeState.takeIf { signInChallengeState ->
                    signInChallengeState is SignInChallengeState.WaitingForAnswer
                }
                challengeState?.apply {
                    stateMachine.send(
                        SignInChallengeEvent(
                            SignInChallengeEvent.EventType.VerifyChallengeAnswer("test", mapOf())
                        )
                    )
                }

                val authNState =
                    it.authNState.takeIf { itN ->
                        itN is AuthenticationState.SignedIn && it.authZState is AuthorizationState.SessionEstablished
                    }
                authNState?.apply {
                    stateMachine.cancel(token)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        )

        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testLocalSignOut() {
        setupConfigureSignedIn()
        setupLocalSignOut()

        val testLatch = CountDownLatch(1)
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            {
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.SignOutRequested(SignOutData())
                        )
                    )
                }

                val authNState = it.authNState.takeIf { itN ->
                    itN is AuthenticationState.SignedOut && it.authZState is AuthorizationState.Configured
                }
                authNState?.apply {
                    stateMachine.cancel(token)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        )

        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testRevokeTokenSignOut() {
        setupConfigureSignedIn()
        setupRevokeTokenSignOut()

        val testLatch = CountDownLatch(1)
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            {
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.SignOutRequested(SignOutData())
                        )
                    )
                }

                val authNState = it.authNState.takeIf { itN ->
                    itN is AuthenticationState.SignedOut && it.authZState is AuthorizationState.Configured
                }
                authNState?.apply {
                    stateMachine.cancel(token)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        )

        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testGlobalSignOut() {
        setupConfigureSignedIn()

        val testLatch = CountDownLatch(1)
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            {
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.SignOutRequested(SignOutData())
                        )
                    )
                }

                val authNState = it.authNState.takeIf { itN ->
                    itN is AuthenticationState.SignedOut && it.authZState is AuthorizationState.Configured
                }
                authNState?.apply {
                    stateMachine.cancel(token)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        )

        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testFetchAuthSessionSignedIn() {
        setupConfigureSignedIn()
        val configureLatch = CountDownLatch(1)
        val testLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        configureLatch.countDown()
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            { it ->
                val authState = it.takeIf {
                    it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn
                }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(AuthorizationEvent(AuthorizationEvent.EventType.FetchAuthSession))
                }

                val authNState = it.authNState.takeIf { itN ->
                    itN is AuthenticationState.SignedIn && it.authZState is AuthorizationState.SessionEstablished
                }
                authNState?.run {
                    stateMachine.cancel(token)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        )

        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testFetchAuthSessionSignedOut() {
        setupConfigureSignedOut()
        val configureLatch = CountDownLatch(1)
        val testLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        configureLatch.countDown()
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            { it ->
                val authState = it.takeIf {
                    it is AuthState.Configured && it.authNState is AuthenticationState.SignedOut
                }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(AuthorizationEvent(AuthorizationEvent.EventType.FetchUnAuthSession))
                }

                val authNState = it.authNState.takeIf { itN ->
                    itN is AuthenticationState.SignedOut && it.authZState is AuthorizationState.SessionEstablished
                }
                authNState?.run {
                    stateMachine.cancel(token)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        )

        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testRefreshUserPoolTokens() {
        setupConfigureSignedIn()
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val testLatch = CountDownLatch(1)
        val token = StateChangeListenerToken()
        stateMachine.listen(
            token,
            { it ->
                val authState = it.takeIf {
                    it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn
                }
                authState?.run {
                    configureLatch.countDown()
                    token?.let(stateMachine::cancel)

                    stateMachine.send(AuthorizationEvent(AuthorizationEvent.EventType.RefreshSession(credentials)))
                    stateMachine.listen(
                        StateChangeListenerToken(),
                        { it2 ->
                            val authNState = it2.takeIf {
                                it2 is AuthState.Configured &&
                                    it2.authNState is AuthenticationState.SignedIn &&
                                    it2.authZState is AuthorizationState.SessionEstablished
                            }
                            authNState?.run {
                                stateMachine.cancel(token)
                                testLatch.countDown()
                            }
                        },
                        null
                    )
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration))
        )

        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }
}
