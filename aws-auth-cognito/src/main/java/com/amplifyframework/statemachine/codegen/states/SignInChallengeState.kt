package com.amplifyframework.statemachine.codegen.states

import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.SignInChallengeActions
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent

sealed class SignInChallengeState : State {
    data class NotStarted(val id: String = "") : SignInChallengeState()
    data class WaitingForAnswer(val id: String = "") : SignInChallengeState()
    data class Verifying(val id: String = "") : SignInChallengeState()
    data class Verified(val id: String = "") : SignInChallengeState()
    data class Error(val exception: Exception) : SignInChallengeState()

    class Resolver(private val challengeActions: SignInChallengeActions) : StateMachineResolver<SignInChallengeState> {
        override val defaultState: SignInChallengeState = NotStarted()

        private fun asSignInChallengeEvent(event: StateMachineEvent): SignInChallengeEvent.EventType? {
            return (event as? SignInChallengeEvent)?.eventType
        }

        override fun resolve(
            oldState: SignInChallengeState,
            event: StateMachineEvent
        ): StateResolution<SignInChallengeState> {
            val defaultResolution = StateResolution(oldState)
            val challengeEvent = asSignInChallengeEvent(event)
            return when (oldState) {
                is NotStarted -> when(challengeEvent) {
                    is SignInChallengeEvent.EventType.WaitForAnswer -> StateResolution(WaitingForAnswer())
                    else -> defaultResolution
                }
                is WaitingForAnswer -> when(challengeEvent) {
                    is SignInChallengeEvent.EventType.VerifyChallengeAnswer -> {
                        val action = challengeActions.verifySignInChallenge(challengeEvent)
                        StateResolution(Verifying(), listOf(action))
                    }
                    else -> defaultResolution
                }
                is Verifying -> when(challengeEvent) {
                    // finalize sign in
                    is SignInChallengeEvent.EventType.Verified -> StateResolution(Verified())
                    else -> defaultResolution
                }
                else -> defaultResolution
            }
        }
    }
}
