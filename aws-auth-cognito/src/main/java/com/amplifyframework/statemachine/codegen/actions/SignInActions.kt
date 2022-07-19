package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.events.SignInEvent

interface SignInActions {
    fun startSRPAuthAction(event: SignInEvent.EventType.InitiateSignInWithSRP): Action
}
