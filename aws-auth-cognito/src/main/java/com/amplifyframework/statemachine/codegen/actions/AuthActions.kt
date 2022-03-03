package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.events.AuthEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher

interface AuthActions {
    fun initializeAuthConfigurationAction(event: AuthEvent.EventType.ConfigureAuth): Action
    fun initializeAuthenticationConfigurationAction(event: AuthEvent.EventType.ConfigureAuthentication): Action
    fun initializeAuthorizationConfigurationAction(event: AuthEvent.EventType): Action
}