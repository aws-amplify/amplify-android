package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.Action

interface FetchIdentityActions {
    fun initFetchIdentityAction(amplifyCredential: AmplifyCredential?): Action
}