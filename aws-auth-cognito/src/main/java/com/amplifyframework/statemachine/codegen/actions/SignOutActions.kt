package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.events.SignOutEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher

interface SignOutActions {
    fun localSignOutAction(event: SignOutEvent.EventType.SignOutLocally) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }

    fun globalSignOutAction(event: SignOutEvent.EventType.SignOutGlobally) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }

    fun revokeTokenAction(event: SignOutEvent.EventType.RevokeToken) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }
}