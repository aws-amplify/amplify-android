package com.amplifyframework.statemachine.codegen.states

import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.SignInActions
import com.amplifyframework.statemachine.codegen.events.SignInEvent

sealed class SignInState : State {
    data class NotStarted(val id: String = "") : SignInState()
    data class SigningInWithSRP(override var srpSignInState: SRPSignInState?) : SignInState()
    data class SigningInWithHostedUI(val id: String = "") : SignInState()
    data class SigningInWithCustom(val id: String = "") : SignInState()
    data class SigningInWithSRPCustom(val id: String = "") : SignInState()
    data class ResolvingSMSChallenge(val id: String = "") : SignInState()
    data class ResolvingCustomChallenge(val id: String = "") : SignInState()
    data class Done(val id: String = "") : SignInState()
    data class Error(val exception: Exception) : SignInState()

    open var srpSignInState: SRPSignInState? = SRPSignInState.NotStarted()

    class Resolver(
        private val srpSignInResolver: StateMachineResolver<SRPSignInState>,
        private val signInActions: SignInActions
    ) :
        StateMachineResolver<SignInState> {
        override val defaultState = NotStarted()

        private fun asSignInEvent(event: StateMachineEvent): SignInEvent.EventType? {
            return (event as? SignInEvent)?.eventType
        }

        override fun resolve(oldState: SignInState, event: StateMachineEvent): StateResolution<SignInState> {
            val resolution = resolveSignInEvent(oldState, event)
            val actions = resolution.actions.toMutableList()
            val builder = Builder(resolution.newState)

            oldState.srpSignInState?.let { srpSignInResolver.resolve(it, event) }?.let {
                builder.srpSignInState = it.newState
                actions += it.actions
            }

            return StateResolution(builder.build(), actions)
        }

        private fun resolveSignInEvent(
            oldState: SignInState,
            event: StateMachineEvent
        ): StateResolution<SignInState> {
            val signInEvent = asSignInEvent(event)
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotStarted -> when (signInEvent) {
                    is SignInEvent.EventType.InitiateSignInWithSRP -> StateResolution(
                        SigningInWithSRP(oldState.srpSignInState),
                        listOf(signInActions.startSRPAuthAction(signInEvent))
                    )
                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception), listOf())
                    else -> defaultResolution
                }
                is SigningInWithSRP -> when (signInEvent) {
                    is SignInEvent.EventType.ReceivedSMSChallenge -> StateResolution(ResolvingSMSChallenge())
                    is SignInEvent.EventType.SignedIn -> StateResolution(Done())
                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception), listOf())
                    else -> defaultResolution
                }
                is ResolvingSMSChallenge -> when (signInEvent) {
                    is SignInEvent.EventType.SignedIn -> StateResolution(Done())
                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception), listOf())
                    else -> defaultResolution
                }
                is Error -> defaultResolution
                else -> defaultResolution
            }
        }
    }

    class Builder(private val signInState: SignInState) :
        com.amplifyframework.statemachine.Builder<SignInState> {
        var srpSignInState: SRPSignInState? = null

        override fun build(): SignInState = when (signInState) {
            is SigningInWithSRP -> SigningInWithSRP(srpSignInState)
            else -> signInState
        }
    }
}
