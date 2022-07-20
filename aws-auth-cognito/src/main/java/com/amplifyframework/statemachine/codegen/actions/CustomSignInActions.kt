package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent

interface CustomSignInActions {
    fun initiateSRPAuthAction(event: CustomSignInEvent.EventType.InitiateCustomSignIn): Action
    fun respondToChallengeAction(event: CustomSignInEvent.EventType.FinalizeSignIn): Action
}