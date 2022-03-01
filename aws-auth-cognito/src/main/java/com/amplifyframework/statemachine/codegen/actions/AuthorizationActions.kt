package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher

interface AuthorizationActions {
    fun configureAuthorizationAction() = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }
}