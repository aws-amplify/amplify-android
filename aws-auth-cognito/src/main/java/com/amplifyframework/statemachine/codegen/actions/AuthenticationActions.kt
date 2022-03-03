package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.data.SignedInData
import com.amplifyframework.auth.cognito.events.AuthenticationEvent
import com.amplifyframework.statemachine.Action

interface AuthenticationActions {
    fun configureAuthenticationAction(event: AuthenticationEvent.EventType.Configure): Action
    fun initiateSRPSignInAction(event: AuthenticationEvent.EventType.SignInRequested): Action
    fun initiateSignOutAction(
        event: AuthenticationEvent.EventType.SignOutRequested,
        signedInData: SignedInData
    ): Action

    fun initiateSignUpAction(event: AuthenticationEvent.EventType.SignUpRequested): Action
    fun initiateConfirmSignUpAction(event: AuthenticationEvent.EventType.ConfirmSignUpRequested): Action
}


