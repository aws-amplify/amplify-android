package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.codegen.events.SignUpEvent
import com.amplifyframework.statemachine.Action

interface SignUpActions {
    fun startSignUpAction(event: SignUpEvent.EventType.InitiateSignUp): Action
    fun confirmSignUpAction(event: SignUpEvent.EventType.ConfirmSignUp): Action
    fun resendConfirmationCodeAction(event: SignUpEvent.EventType.ResendSignUpCode): Action
    fun resetSignUpAction(): Action
}