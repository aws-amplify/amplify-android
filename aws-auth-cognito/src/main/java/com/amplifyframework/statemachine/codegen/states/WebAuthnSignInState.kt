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

package com.amplifyframework.statemachine.codegen.states

import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.SignInActions
import com.amplifyframework.statemachine.codegen.actions.WebAuthnSignInActions
import com.amplifyframework.statemachine.codegen.data.WebAuthnSignInContext
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import com.amplifyframework.statemachine.codegen.events.WebAuthnEvent

internal sealed class WebAuthnSignInState : State {
    data class NotStarted(val id: String = "") : WebAuthnSignInState()
    data class FetchingCredentialOptions(val id: String = "") : WebAuthnSignInState()
    data class AssertingCredentials(val id: String = "") : WebAuthnSignInState()
    data class VerifyingCredentialsAndSigningIn(val id: String = "") : WebAuthnSignInState()
    data class SignedIn(val id: String = "") : WebAuthnSignInState()
    data class Error(val exception: Exception, val context: WebAuthnSignInContext) : WebAuthnSignInState()

    class Resolver(private val actions: WebAuthnSignInActions, private val signInActions: SignInActions) :
        StateMachineResolver<WebAuthnSignInState> {
        override val defaultState = NotStarted()

        override fun resolve(
            oldState: WebAuthnSignInState,
            event: StateMachineEvent
        ): StateResolution<WebAuthnSignInState> {
            val defaultResolution = StateResolution(oldState)
            val webAuthnEvent = event.asWebAuthnSignInEvent()

            // Thrown errors always result in the error state
            if (webAuthnEvent is WebAuthnEvent.EventType.ThrowError) {
                return StateResolution(
                    Error(webAuthnEvent.exception, webAuthnEvent.signInContext)
                )
            }

            return when (oldState) {
                is NotStarted -> when (webAuthnEvent) {
                    is WebAuthnEvent.EventType.AssertCredentialOptions -> StateResolution(
                        newState = AssertingCredentials(),
                        actions = listOf(actions.assertCredentials(webAuthnEvent))
                    )
                    is WebAuthnEvent.EventType.FetchCredentialOptions -> StateResolution(
                        newState = FetchingCredentialOptions(),
                        actions = listOf(actions.fetchCredentialOptions(webAuthnEvent))
                    )
                    else -> defaultResolution
                }
                is FetchingCredentialOptions -> when (webAuthnEvent) {
                    is WebAuthnEvent.EventType.AssertCredentialOptions -> StateResolution(
                        newState = AssertingCredentials(),
                        actions = listOf(actions.assertCredentials(webAuthnEvent))
                    )
                    else -> defaultResolution
                }
                is AssertingCredentials -> when (webAuthnEvent) {
                    is WebAuthnEvent.EventType.VerifyCredentialsAndSignIn -> StateResolution(
                        newState = VerifyingCredentialsAndSigningIn(),
                        actions = listOf(actions.verifyCredentialAndSignIn(webAuthnEvent))
                    )
                    else -> defaultResolution
                }
                is VerifyingCredentialsAndSigningIn -> defaultResolution
                is SignedIn -> defaultResolution
                is Error -> when {
                    event is SignInEvent && event.eventType is SignInEvent.EventType.InitiateWebAuthnSignIn ->
                        StateResolution(
                            newState = NotStarted(),
                            actions = listOf(signInActions.initiateWebAuthnSignInAction(event.eventType))
                        )
                    else -> defaultResolution
                }
            }
        }

        private fun StateMachineEvent.asWebAuthnSignInEvent() = (this as? WebAuthnEvent)?.eventType
    }
}
