package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.Action

interface FetchAuthSessionActions {
    fun configureUserPoolTokensAction(amplifyCredential: AmplifyCredential?): Action
    fun configureIdentityAction(amplifyCredential: AmplifyCredential?): Action
    fun configureAWSCredentialsAction(amplifyCredential: AmplifyCredential?): Action
}