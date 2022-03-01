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

import com.amplifyframework.auth.cognito.events.*
import com.amplifyframework.auth.cognito.states.*
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        setupSignUpActions()
        setupStateMachine()
        Dispatchers.setMain(mainThreadSurrogate)
    }

    private fun setupStateMachine() {
        stateMachine = AuthStateMachine(
            AuthState.Resolver(
                AuthenticationState.Resolver(
                    CredentialStoreState.Resolver(),
                    SignUpState.Resolver(mockSignUpActions),
                    SRPSignInState.Resolver(mockSRPActions),
                    SignOutState.Resolver(mockSignOutActions),
                    mockAuthenticationActions
                ),
                AuthorizationState.Resolver(mockAuthorizationActions),
                mockAuthActions
            ), AuthEnvironment.empty
        )
    }

    private fun setupConfigureSignedIn() {
        Mockito.`when`(mockAuthenticationActions.configureAuthenticationAction(MockitoHelper.anyObject()))
            .thenReturn(
                object : Action {
                    override suspend fun execute(
                        dispatcher: EventDispatcher,
                        environment: Environment
                    ) {
                        stateMachine.send(
                            AuthenticationEvent(
                                AuthenticationEvent.EventType.InitializedSignedIn(signedInData)
                            )
                        )
                        stateMachine.send(
                            AuthEvent(
                                AuthEvent.EventType.ConfiguredAuthentication(configuration)
                            )
                        )
                    }
                })
    }

    private fun setupLocalSignOut() {
        Mockito.`when`(
            mockAuthenticationActions.initiateSignOutAction(
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject()
            )
        ).thenReturn(
            object : Action {
                override suspend fun execute(
                    dispatcher: EventDispatcher,
                    environment: Environment
                ) {
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
            })
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
        val testLatch = CountDownLatch(1)
        val listenLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        stateMachine.listen({
            if (it is AuthState.Configured && it.authNState is AuthenticationState.SignedOut) {
                listenLatch.countDown()
            }
        }, {
            subscribeLatch.countDown()
        })
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        val configure =
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, null))
        stateMachine.send(configure)
        stateMachine.getCurrentState {
            assertTrue(it is AuthState.ConfiguringAuth)
            testLatch.countDown()
        }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testConfigureSignedIn() {
        setupConfigureSignedIn()

        val testLatch = CountDownLatch(1)
        val listenLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        stateMachine.listen({
            if (it is AuthState.Configured && it.authNState is AuthenticationState.SignedIn) {
                listenLatch.countDown()
            }
        }, {
            subscribeLatch.countDown()
        })
        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        val configure =
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
        stateMachine.send(configure)
        stateMachine.getCurrentState {
            assertTrue(it is AuthState.ConfiguringAuth)
            testLatch.countDown()
        }
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { listenLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testSignIn() {
        val testLatch = CountDownLatch(1)
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        stateMachine.listen({
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
                testLatch.countDown()
            }
        }, {
            subscribeLatch.countDown()
        })

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
        stateMachine.listen({
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
                testLatch.countDown()
            }
        }, {
            subscribeLatch.countDown()
        })

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
        stateMachine.listen({
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
                testLatch.countDown()
            }
        }, {
            subscribeLatch.countDown()
        })

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
        )

        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    fun testSignUp() {
        val testLatch = CountDownLatch(1)
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        stateMachine.listen({
            val authState =
                it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedOut }
            authState?.run {
                configureLatch.countDown()
                stateMachine.send(
                    AuthenticationEvent(
                        AuthenticationEvent.EventType.SignUpRequested(
                            "username",
                            "password",
                            AuthSignUpOptions.builder().build()
                        )
                    )
                )
            }

            val authNState =
                it.authNState.takeIf { itN -> itN is AuthenticationState.SigningUp && itN.signUpState is SignUpState.SigningUpInitiated }
            authNState?.apply {
                testLatch.countDown()
            }
        }, {
            subscribeLatch.countDown()
        })

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
        )

        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
    }

    @Test
    @Ignore("WIP")
    fun testConfirmSignUp() {
        val testLatch = CountDownLatch(1)
        val configureLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)
        stateMachine.listen({
            val authState =
                it.takeIf { it is AuthState.Configured && it.authNState is AuthenticationState.SignedOut }
            authState?.run {
                configureLatch.countDown()
                stateMachine.send(
                    SignUpEvent(SignUpEvent.EventType.ConfirmSignUp("username", "code"))
                )
            }

            val authNState =
                it.authNState.takeIf { itN -> itN is AuthenticationState.SigningUp && itN.signUpState is SignUpState.SignedUp }
            authNState?.apply {
                testLatch.countDown()
            }
        }, {
            subscribeLatch.countDown()
        })

        assertTrue { subscribeLatch.await(5, TimeUnit.SECONDS) }

        stateMachine.send(
            AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration, credentials))
        )

        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
        assertTrue { configureLatch.await(5, TimeUnit.SECONDS) }
    }
}