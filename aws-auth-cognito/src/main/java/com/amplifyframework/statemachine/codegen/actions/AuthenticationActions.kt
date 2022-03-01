package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.data.SignedInData
import com.amplifyframework.auth.cognito.events.AuthenticationEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher

interface AuthenticationActions {
    fun configureAuthenticationAction(event: AuthenticationEvent.EventType.Configure) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                throw Error("Not yet implemented")
            }
        }

    fun initiateSRPSignInAction(event: AuthenticationEvent.EventType.SignInRequested) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            throw Error("Not yet implemented")
        }
    }

    fun initiateSignOutAction(event: AuthenticationEvent.EventType.SignOutRequested, signedInData: SignedInData) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                throw Error("Not yet implemented")
            }
        }

    fun initiateSignUpAction(event: AuthenticationEvent.EventType.SignUpRequested) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                throw Error("Not yet implemented")
            }
        }
}


