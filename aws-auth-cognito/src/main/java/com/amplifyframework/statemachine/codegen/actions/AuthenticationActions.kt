package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent

interface AuthenticationActions {
    fun configureAuthenticationAction(event: AuthenticationEvent.EventType.Configure): Action
    fun initiateSRPSignInAction(event: AuthenticationEvent.EventType.SignInRequested): Action
    fun initiateSignOutAction(
        event: AuthenticationEvent.EventType.SignOutRequested,
        signedInData: SignedInData
    ): Action
}
