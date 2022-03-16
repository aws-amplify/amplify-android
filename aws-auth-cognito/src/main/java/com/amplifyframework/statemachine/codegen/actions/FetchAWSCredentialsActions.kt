package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.Action

interface FetchAWSCredentialsActions {
    fun initFetchAWSCredentialsAction(amplifyCredential: AmplifyCredential?): Action
}