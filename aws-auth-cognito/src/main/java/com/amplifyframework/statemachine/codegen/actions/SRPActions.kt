package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.events.SRPEvent
import com.amplifyframework.statemachine.Action

interface SRPActions {
    fun initiateSRPAuthAction(event: SRPEvent.EventType.InitiateSRP): Action
    fun verifyPasswordSRPAction(event: SRPEvent.EventType.RespondPasswordVerifier): Action
}