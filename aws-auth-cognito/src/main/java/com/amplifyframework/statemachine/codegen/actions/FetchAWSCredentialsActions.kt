package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.auth.cognito.data.AmplifyCredential
import com.amplifyframework.statemachine.Action

interface FetchAWSCredentialsActions {
    fun initFetchAWSCredentialsAction(amplifyCredential: AmplifyCredential?): Action
}