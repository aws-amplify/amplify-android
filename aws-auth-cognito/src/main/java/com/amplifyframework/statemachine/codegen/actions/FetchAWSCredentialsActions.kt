package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential

interface FetchAWSCredentialsActions {
    fun initFetchAWSCredentialsAction(amplifyCredential: AmplifyCredential?): Action
}
