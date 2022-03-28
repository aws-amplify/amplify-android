package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential

interface FetchIdentityActions {
    fun initFetchIdentityAction(amplifyCredential: AmplifyCredential?): Action
}
