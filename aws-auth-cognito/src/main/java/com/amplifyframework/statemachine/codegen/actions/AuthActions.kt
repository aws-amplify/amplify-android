package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.events.AuthEvent

interface AuthActions {
    fun initializeAuthConfigurationAction(event: AuthEvent.EventType.ConfigureAuth): Action
    fun initializeAuthenticationConfigurationAction(
        event: AuthEvent.EventType.ConfigureAuthentication
    ): Action
    fun initializeAuthorizationConfigurationAction(event: AuthEvent.EventType): Action
}
