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

package com.amplifyframework.statemachine.codegen.states

import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.MigrateAuthActions
import com.amplifyframework.statemachine.codegen.events.SignInEvent

internal sealed class MigrateSignInState : State {
    data class NotStarted(val id: String = "") : MigrateSignInState()
    data class SigningIn(val id: String = "") : MigrateSignInState()
    data class SignedIn(val id: String = "") : MigrateSignInState()

    class Resolver(private val migrateAuthActions: MigrateAuthActions) : StateMachineResolver<MigrateSignInState> {
        override val defaultState = NotStarted()

        private fun asSignInEvent(event: StateMachineEvent): SignInEvent.EventType? {
            return (event as? SignInEvent)?.eventType
        }

        override fun resolve(
            oldState: MigrateSignInState,
            event: StateMachineEvent
        ): StateResolution<MigrateSignInState> {
            val defaultResolution = StateResolution(oldState)
            val signInEvent = asSignInEvent(event)
            return when (oldState) {
                is NotStarted -> when (signInEvent) {
                    is SignInEvent.EventType.InitiateMigrateAuth -> StateResolution(
                        SigningIn(),
                        listOf(migrateAuthActions.initiateMigrateAuthAction(signInEvent))
                    )
                    else -> defaultResolution
                }
                is SigningIn -> when (signInEvent) {
                    is SignInEvent.EventType.FinalizeSignIn -> StateResolution(SignedIn())
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }
}
