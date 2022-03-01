package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.events.SRPEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher

interface SRPActions {
    fun initiateSRPAuthAction(event: SRPEvent.EventType.InitiateSRP) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }

    fun verifyPasswordSRPAction(event: SRPEvent.EventType.RespondPasswordVerifier) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                throw Error("Not yet implemented")
            }
        }
}