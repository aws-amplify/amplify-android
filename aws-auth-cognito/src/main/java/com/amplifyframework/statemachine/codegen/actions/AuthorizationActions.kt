package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.statemachine.Action

interface AuthorizationActions {
    fun configureAuthorizationAction(): Action
    fun initializeFetchAuthSession(amplifyCredential: AmplifyCredential?): Action
}