package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent

interface CustomSignInActions {
    fun initiateCustomSignInAuthAction(event: CustomSignInEvent.EventType.InitiateCustomSignIn): Action
}
