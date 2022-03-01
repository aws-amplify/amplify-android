package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.events.AuthEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher

interface AuthActions {
    fun initializeAuthConfigurationAction(event: AuthEvent.EventType.ConfigureAuth) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                throw Error("Not yet implemented")
            }
        }

    fun initializeAuthenticationConfigurationAction(event: AuthEvent.EventType.ConfigureAuthentication) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                throw Error("Not yet implemented")
            }
        }

    fun initializeAuthorizationConfigurationAction(event: AuthEvent.EventType) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }
}