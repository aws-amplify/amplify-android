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

package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthenticationResultType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.WebAuthnHelper
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.ChallengeParameter
import com.amplifyframework.statemachine.codegen.data.WebAuthnSignInContext
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import com.amplifyframework.statemachine.codegen.events.WebAuthnEvent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.MockKAssertScope
import io.mockk.MockKVerificationScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import java.lang.ref.WeakReference
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WebAuthnSignInCognitoActionsTest {

    private val signInContext = WebAuthnSignInContext(
        username = "username",
        callingActivity = WeakReference(null),
        session = "session",
        requestJson = null,
        responseJson = null
    )

    private val identityProviderClient = mockk<CognitoIdentityProviderClient>()

    private val dispatcher = mockk<EventDispatcher>(relaxed = true)
    private val authEnvironment = mockk<AuthEnvironment> {
        every { context } returns mockk()
        every { cognitoAuthService.cognitoIdentityProviderClient } returns identityProviderClient
        coEvery { getUserContextData(any()) } returns null
        every { getPinpointEndpointId() } returns null
        every { configuration.userPool?.appClient } returns "app-client"
    }

    @Test
    fun `fetchCredentialOptions dispatches assert credentials event`() = runTest {
        val expectedSignInContext = WebAuthnSignInContext(
            username = signInContext.username,
            callingActivity = signInContext.callingActivity,
            session = "new-session",
            requestJson = "request-json",
            responseJson = null
        )

        coEvery { identityProviderClient.respondToAuthChallenge(any()) } returns RespondToAuthChallengeResponse {
            session = "new-session"
            challengeName = ChallengeNameType.WebAuthn
            challengeParameters = mapOf(
                ChallengeParameter.CredentialRequestOptions.key to "request-json"
            )
        }

        val event = WebAuthnEvent.EventType.FetchCredentialOptions(signInContext)
        val action = WebAuthnSignInCognitoActions.fetchCredentialOptions(event)
        action.execute(dispatcher, authEnvironment)

        verify {
            dispatcher.send(
                withSignInEvent<SignInEvent.EventType.InitiateWebAuthnSignIn> {
                    it.signInContext shouldBe expectedSignInContext
                }
            )
        }
    }

    @Test
    fun `fetchCredentialsOptions results in InvalidStateException if there is no identity provider client`() = runTest {
        every { authEnvironment.cognitoAuthService.cognitoIdentityProviderClient } returns null

        val event = WebAuthnEvent.EventType.FetchCredentialOptions(signInContext)
        val action = WebAuthnSignInCognitoActions.fetchCredentialOptions(event)
        action.execute(dispatcher, authEnvironment)

        verify {
            dispatcher.send(
                withWebAuthnEvent<WebAuthnEvent.EventType.ThrowError> {
                    it.exception.shouldBeInstanceOf<InvalidStateException>()
                }
            )
        }
    }

    @Test
    fun `assertCredentials dispatches verify credentials event`() = runTest {
        val requestContext = signInContext.copy(requestJson = "request-json")
        val expectedSignInContext = requestContext.copy(responseJson = "response-json")

        val event = WebAuthnEvent.EventType.AssertCredentialOptions(requestContext)
        val action = WebAuthnSignInCognitoActions.assertCredentials(event)

        mockkConstructor(WebAuthnHelper::class) {
            coEvery { anyConstructed<WebAuthnHelper>().getCredential("request-json", any()) } returns "response-json"
            action.execute(dispatcher, authEnvironment)
        }

        verify {
            dispatcher.send(
                withWebAuthnEvent<WebAuthnEvent.EventType.VerifyCredentialsAndSignIn> {
                    it.signInContext shouldBe expectedSignInContext
                }
            )
        }
    }

    @Test
    fun `assertCredentials results in InvalidStateException if request JSON is missing`() = runTest {
        val requestContext = signInContext.copy(requestJson = null)
        val event = WebAuthnEvent.EventType.AssertCredentialOptions(requestContext)
        val action = WebAuthnSignInCognitoActions.assertCredentials(event)

        mockkConstructor(WebAuthnHelper::class) {
            coEvery { anyConstructed<WebAuthnHelper>().getCredential("request-json", any()) } returns "response-json"
            action.execute(dispatcher, authEnvironment)
        }

        verify {
            dispatcher.send(
                withWebAuthnEvent<WebAuthnEvent.EventType.ThrowError> {
                    it.exception.shouldBeInstanceOf<InvalidStateException>()
                }
            )
        }
    }

    @Test
    fun `verifyCredentials dispatches signedInCompleted event`() = runTest {
        val requestContext = signInContext.copy(responseJson = "response-json")

        coEvery { identityProviderClient.respondToAuthChallenge(any()) } returns RespondToAuthChallengeResponse {
            authenticationResult = AuthenticationResultType {
            }
        }

        val event = WebAuthnEvent.EventType.VerifyCredentialsAndSignIn(requestContext)
        val action = WebAuthnSignInCognitoActions.verifyCredentialAndSignIn(event)
        action.execute(dispatcher, authEnvironment)

        verify {
            dispatcher.send(withAuthEvent<AuthenticationEvent.EventType.SignInCompleted>())
        }
    }

    @Test
    fun `verifyCredentials results in InvalidStateException if missing response json`() = runTest {
        val requestContext = signInContext.copy(responseJson = null)

        coEvery { identityProviderClient.respondToAuthChallenge(any()) } returns RespondToAuthChallengeResponse {
            authenticationResult = AuthenticationResultType {
            }
        }

        val event = WebAuthnEvent.EventType.VerifyCredentialsAndSignIn(requestContext)
        val action = WebAuthnSignInCognitoActions.verifyCredentialAndSignIn(event)
        action.execute(dispatcher, authEnvironment)

        verify {
            dispatcher.send(
                withWebAuthnEvent<WebAuthnEvent.EventType.ThrowError> {
                    it.exception.shouldBeInstanceOf<InvalidStateException>()
                }
            )
        }
    }

    private inline fun <reified T : WebAuthnEvent.EventType> MockKVerificationScope.withWebAuthnEvent(
        noinline assertions: MockKAssertScope.(T) -> Unit = { }
    ) = withArg<StateMachineEvent> {
        val event = it.shouldBeInstanceOf<WebAuthnEvent>()
        val type = event.eventType.shouldBeInstanceOf<T>()
        assertions(type)
    }

    private inline fun <reified T : SignInEvent.EventType> MockKVerificationScope.withSignInEvent(
        noinline assertions: MockKAssertScope.(T) -> Unit = { }
    ) = withArg<StateMachineEvent> {
        val event = it.shouldBeInstanceOf<SignInEvent>()
        val type = event.eventType.shouldBeInstanceOf<T>()
        assertions(type)
    }

    private inline fun <reified T : AuthenticationEvent.EventType> MockKVerificationScope.withAuthEvent(
        noinline assertions: MockKAssertScope.(T) -> Unit = { }
    ) = withArg<StateMachineEvent> {
        val event = it.shouldBeInstanceOf<AuthenticationEvent>()
        val type = event.eventType.shouldBeInstanceOf<T>()
        assertions(type)
    }
}
