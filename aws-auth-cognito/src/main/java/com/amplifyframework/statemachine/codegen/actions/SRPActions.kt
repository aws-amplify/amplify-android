package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.events.SRPEvent

interface SRPActions {
    fun initiateSRPAuthAction(event: SRPEvent.EventType.InitiateSRP): Action
    fun verifyPasswordSRPAction(event: SRPEvent.EventType.RespondPasswordVerifier): Action
}
