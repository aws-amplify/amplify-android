package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.statemachine.Action

interface FetchIdentityActions {
    fun initFetchIdentityAction(amplifyCredential: AmplifyCredential?): Action
}