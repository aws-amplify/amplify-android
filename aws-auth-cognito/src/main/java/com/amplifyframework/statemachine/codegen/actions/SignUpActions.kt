package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.events.SignUpEvent
import com.amplifyframework.statemachine.Action

interface SignUpActions {
    fun startSignUpAction(event: SignUpEvent.EventType.InitiateSignUp): Action
    fun confirmSignUpAction(event: SignUpEvent.EventType.ConfirmSignUp): Action
    fun resendConfirmationCodeAction(): Action
    fun resetSignUpAction(): Action
}