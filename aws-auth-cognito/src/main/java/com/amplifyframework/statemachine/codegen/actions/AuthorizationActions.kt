package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.Action

interface AuthorizationActions {
    fun configureAuthorizationAction(): Action
    fun initializeFetchAuthSession(amplifyCredential: AmplifyCredential?): Action
}