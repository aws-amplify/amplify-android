package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.events.SignUpEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher

interface SignUpActions {
    fun startSignUpAction(event: SignUpEvent.EventType.InitiateSignUp) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }

    fun confirmSignUpAction(event: SignUpEvent.EventType.ConfirmSignUp) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }

    fun resendConfirmationCodeAction() = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }

    fun cancelSignUpAction() = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }
}