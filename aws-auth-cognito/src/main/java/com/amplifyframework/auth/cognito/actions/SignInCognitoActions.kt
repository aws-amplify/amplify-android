package com.amplifyframework.auth.cognito.actions

import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignInActions
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

object SignInCognitoActions : SignInActions {
    override fun startSRPAuthAction(event: SignInEvent.EventType.InitiateSignInWithSRP) =
        Action<AuthEnvironment>("StartSRPAuth") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = SRPEvent(SRPEvent.EventType.InitiateSRP(event.username, event.password))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
