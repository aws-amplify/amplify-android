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

import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.StateChangeListenerToken
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.DeleteUserEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import com.amplifyframework.statemachine.codegen.events.FetchUserPoolTokensEvent
import com.amplifyframework.statemachine.codegen.events.SignOutEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.DeleteUserState
import com.amplifyframework.statemachine.codegen.states.FetchAuthSessionState
import com.amplifyframework.statemachine.codegen.states.FetchAwsCredentialsState
import com.amplifyframework.statemachine.codegen.states.FetchIdentityState
import com.amplifyframework.statemachine.codegen.states.FetchUserPoolTokensState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StateTransitionTests : StateTransitionTestBase() {

    private val mainThreadSurrogate = newSingleThreadContext("Main thread")

    internal lateinit var stateMachine: AuthStateMachine

    @Before
    fun setUp() {
        setupAuthActions()
        setupAuthNActions()
        setupAuthZActions()
        setupSRPActions()
        setupSignOutActions()
        setupFetchAuthActions()
        setupDeleteAction()
        setupStateMachine()
        Dispatchers.setMain(mainThreadSurrogate)
    }

    private fun setupStateMachine() {
        stateMachine = AuthStateMachine(
            AuthState.Resolver(
                AuthenticationState.Resolver(
                    SRPSignInState.Resolver(mockSRPActions),
                    SignOutState.Resolver(mockSignOutActions),
                    mockAuthenticationActions
                ),
                AuthorizationState.Resolver(
                    FetchAuthSessionState.Resolver(
                        FetchAwsCredentialsState.Resolver(mockFetchAwsCredentialsActions),
                        FetchIdentityState.Resolver(mockFetchIdentityActions),
                        FetchUserPoolTokensState.Resolver(mockFetchUserPoolTokensActions),
                        mockFetchAuthSessionActions
                    ),
                    DeleteUserState.Resolver(mockDeleteUserActions),
                    mockAuthorizationActions
                ),
                mockAuthActions
            ),
            AuthEnvironment(configuration, cognitoAuthService)
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
                            AuthenticationEvent.EventType.InitializedSignedIn(signedInData)
                        )
                    )
                    dispatcher.send(
                        AuthEvent(
                            AuthEvent.EventType.ConfiguredAuthentication(configuration)
                        )
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
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.InitializedSignedOut(SignedOutData())
                        )
                    )
                    dispatcher.send(
                        AuthEvent(AuthEvent.EventType.ConfiguredAuthentication(configuration))
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
                        SignOutEvent.EventType.SignOutLocally(
                            signedInData,
                            isGlobalSignOut = false,
                            invalidateTokens = false
                        )
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
        var token: StateChangeListenerToken? = null
        token = stateMachine.listen(
            {
                if (it is AuthState.Configured && it.authNState is AuthenticationState.SignedOut) {
                    token?.let(stateMachine::cancel)
                    listenLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        val configure =
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, null))
        stateMachine.send(configure)

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testConfigureSignedIn() {
        setupConfigureSignedIn()

        val listenLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        var token: StateChangeListenerToken? = null
        token = stateMachine.listen(
            {
                if (it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn) {
                    token?.let(stateMachine::cancel)
                    listenLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        val configure =
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
        stateMachine.send(configure)

        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testSignIn() {
        setupConfigureSignedOut()
        val testLatch = CountDownLatch(1)
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        var token: StateChangeListenerToken? = null
        token = stateMachine.listen(
            {
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedOut }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.SignInRequested(
                                "username",
                                "password",
                                AuthSignInOptions.defaults()
                            )
                        )
                    )
                }

                val authNState =
                    it.authNState.takeIf { itN -> itN is AuthenticationState.SignedIn }
                authNState?.apply {
                    token?.let(stateMachine::cancel)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, null))
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
        var token: StateChangeListenerToken? = null
        token = stateMachine.listen(
            {
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.SignOutRequested()
                        )
                    )
                }

                val authNState =
                    it.authNState.takeIf { itN -> itN is AuthenticationState.SignedOut }
                authNState?.apply {
                    token?.let(stateMachine::cancel)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
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
        var token: StateChangeListenerToken? = null
        token = stateMachine.listen(
            {
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.SignOutRequested()
                        )
                    )
                }

                val authNState =
                    it.authNState.takeIf { itN -> itN is AuthenticationState.SignedOut }
                authNState?.apply {
                    token?.let(stateMachine::cancel)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
        )

        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testSignUp() {

    }

    @Test
    fun testConfirmSignUp() {

    }

    @Test
    fun testFetchAuthSessionSignedIn() {
        setupConfigureSignedIn()
        val configureLatch = CountDownLatch(1)
        val testLatch = CountDownLatch(2)
        val subscribeLatch = CountDownLatch(1)
        configureLatch.countDown()
        var token: StateChangeListenerToken? = null
        token = stateMachine.listen(
            { it ->
                val authState =
                    it.takeIf {
                        it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn &&
                            it.authZState is AuthorizationState.Configured
                    }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthorizationEvent(
                            AuthorizationEvent.EventType.FetchAuthSession(credentials)
                        )
                    )
                }

                val authZFetchTokensState =
                    it.authZState.takeIf { itZ ->
                        itZ is AuthorizationState.FetchingAuthSession &&
                            itZ.fetchAuthSessionState is FetchAuthSessionState.FetchingUserPoolTokens
                    }
                authZFetchTokensState?.run {
                    testLatch.countDown()
                }

                val authZState =
                    it.authZState.takeIf { itZ -> itZ is AuthorizationState.SessionEstablished }
                authZState?.run {
                    token?.let(stateMachine::cancel)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
        )

        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    private fun setupAuthZNoTokens() {
        Mockito.`when`(
            mockAuthorizationActions.initializeFetchAuthSession(MockitoHelper.anyObject())
        )
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        FetchAuthSessionEvent(FetchAuthSessionEvent.EventType.FetchIdentity(credentials))
                    )
                }
            )
    }

    @Test
    fun testFetchAuthSessionSignedOut() {
        setupConfigureSignedOut()
        setupAuthZNoTokens()
        val configureLatch = CountDownLatch(1)
        val testLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        configureLatch.countDown()
        var token: StateChangeListenerToken? = null
        token = stateMachine.listen(
            { it ->
                val authState =
                    it.takeIf {
                        it is AuthState.Configured && it.authNState is AuthenticationState.SignedOut &&
                            it.authZState is AuthorizationState.Configured
                    }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthorizationEvent(
                            AuthorizationEvent.EventType.FetchAuthSession(null)
                        )
                    )
                }

                val authZFetchTokensState =
                    it.authZState.takeIf { itZ -> itZ is AuthorizationState.FetchingAuthSession }
                assertFalse(
                    authZFetchTokensState?.fetchAuthSessionState is FetchAuthSessionState.FetchingUserPoolTokens
                )

                val authZState =
                    it.authZState.takeIf { itZ -> itZ is AuthorizationState.SessionEstablished }
                authZState?.run {
                    token?.let(stateMachine::cancel)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
        )

        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    private fun setupRefreshTokens() {
        Mockito.`when`(mockFetchAuthSessionActions.configureUserPoolTokensAction(credentials))
            .thenReturn(
                Action { dispatcher, _ ->
                    dispatcher.send(
                        FetchUserPoolTokensEvent(
                            FetchUserPoolTokensEvent.EventType.Refresh(credentials)
                        )
                    )
                }
            )
    }

    @Test
    fun testRefreshUserPoolTokens() {
        setupConfigureSignedIn()
        setupRefreshTokens()
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val testLatch = CountDownLatch(2)
        var token: StateChangeListenerToken? = null
        token = stateMachine.listen(
            { it ->
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        AuthorizationEvent(
                            AuthorizationEvent.EventType.FetchAuthSession(credentials)
                        )
                    )
                }

                val authZRefreshTokensState =
                    it.authZState.takeIf { itZ ->
                        itZ is AuthorizationState.FetchingAuthSession &&
                            itZ.fetchAuthSessionState is FetchAuthSessionState.FetchingUserPoolTokens &&
                            itZ.fetchAuthSessionState?.fetchUserPoolTokensState is FetchUserPoolTokensState.Refreshing
                    }
                authZRefreshTokensState?.run {
                    testLatch.countDown()
                }

                val authZState =
                    it.authZState.takeIf { itZ -> itZ is AuthorizationState.SessionEstablished }
                authZState?.run {
                    token?.let(stateMachine::cancel)
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
        )

        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testDeleteUser() {
        setupConfigureSignedIn()
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        val testLatch = CountDownLatch(1)
        var token: StateChangeListenerToken? = null
        token = stateMachine.listen(
            { it ->
                println("Auth State :" + it)
                val authState =
                    it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn }
                authState?.run {
                    configureLatch.countDown()
                    stateMachine.send(
                        DeleteUserEvent(
                            DeleteUserEvent.EventType.DeleteUser("TOKEN-123")
                        )
                    )
                }
                val signoutState = it.takeIf { it.authNState is AuthenticationState.SignedOut }
                signoutState?.run {
                    stateMachine.send(
                        DeleteUserEvent(
                            DeleteUserEvent.EventType.SignOutDeletedUser()
                        )
                    )
                }
                val deleteUserState = it.authZState?.deleteUserState
                val userDeletedSuccess = deleteUserState?.takeIf {
                    println("Delete User State: " + it)
                    it is DeleteUserState.UserDeleted
                }

                userDeletedSuccess?.run {
                    testLatch.countDown()
                }
            },
            {
                subscribeLatch.countDown()
            }
        )

        assertTrue { subscribeLatch.await(5, TimeUnit.MINUTES) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
        )
        assertTrue { configureLatch.await(5, TimeUnit.MINUTES) }
        assertTrue { testLatch.await(10, TimeUnit.MINUTES) }
    }
}
