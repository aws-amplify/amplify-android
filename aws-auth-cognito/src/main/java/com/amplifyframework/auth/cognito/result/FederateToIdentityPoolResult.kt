package com.amplifyframework.auth.cognito.result

import com.amplifyframework.statemachine.codegen.data.AWSCredentials

data class FederateToIdentityPoolResult(val credentials: AWSCredentials, val identityId: String)
