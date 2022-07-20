package com.amplifyframework.statemachine.codegen.states

import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution

sealed class HostedUISignInState : State {
    data class NotStarted(val id: String = "") : HostedUISignInState()
    data class ShowingUI(val id: String = "") : HostedUISignInState()
    data class FetchingToken(val id: String = "") : HostedUISignInState()
    data class Done(val id: String = "") : HostedUISignInState()
    data class Error(val exception: Exception) : HostedUISignInState()

    class Resolver() : StateMachineResolver<HostedUISignInState> {
        override val defaultState = NotStarted()

        // TODO Implement Resolver
        override fun resolve(
            oldState: HostedUISignInState,
            event: StateMachineEvent
        ): StateResolution<HostedUISignInState> {
            return StateResolution(oldState)
        }
    }
}
