package com.amplifyframework.auth.cognito.actions

import com.amplifyframework.auth.cognito.events.AuthEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.codegen.actions.AuthorizationActions

object AuthorizationCognitoActions : AuthorizationActions {
    override fun configureAuthorizationAction() = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            dispatcher.send(AuthEvent(AuthEvent.EventType.ConfiguredAuthorization))
        }
    }
}