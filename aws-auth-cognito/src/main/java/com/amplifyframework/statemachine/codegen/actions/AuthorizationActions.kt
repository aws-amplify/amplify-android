package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential

interface AuthorizationActions {
    fun configureAuthorizationAction(): Action
    fun initializeFetchAuthSession(amplifyCredential: AmplifyCredential?): Action
}
