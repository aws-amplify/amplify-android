package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.events.SignOutEvent

interface SignOutActions {
    fun localSignOutAction(event: SignOutEvent.EventType.SignOutLocally): Action
    fun globalSignOutAction(event: SignOutEvent.EventType.SignOutGlobally): Action
    fun revokeTokenAction(event: SignOutEvent.EventType.RevokeToken): Action
}
