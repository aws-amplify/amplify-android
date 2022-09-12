/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.auth.cognito.result

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AWSCognitoAuthExceptions
import com.amplifyframework.auth.result.AuthSignOutResult
import com.amplifyframework.statemachine.codegen.data.GlobalSignOutErrorData
import com.amplifyframework.statemachine.codegen.data.HostedUIErrorData
import com.amplifyframework.statemachine.codegen.data.RevokeTokenErrorData

sealed class AWSCognitoAuthSignOutResult : AuthSignOutResult() {

    abstract val signedOutLocally: Boolean

    object CompleteSignOut : AWSCognitoAuthSignOutResult() {
        override val signedOutLocally = true
    }
    data class FailedSignOut(val error: AuthException) : AWSCognitoAuthSignOutResult() {
        override val signedOutLocally = false
    }

    data class PartialSignOut(
        val hostedUIError: HostedUIError? = null,
        val globalSignOutError: GlobalSignOutError? = null,
        val revokeTokenError: RevokeTokenError? = null,
    ) : AWSCognitoAuthSignOutResult() {
        override val signedOutLocally = true
    }
}

class HostedUIError internal constructor(hostedUIErrorData: HostedUIErrorData) {
    val error = hostedUIErrorData.error
}

class GlobalSignOutError internal constructor(globalSignOutErrorData: GlobalSignOutErrorData) {
    val accessToken = globalSignOutErrorData.accessToken
    val error = AWSCognitoAuthExceptions.GlobalSignOutException(globalSignOutErrorData.error)
}

class RevokeTokenError internal constructor(revokeTokenErrorData: RevokeTokenErrorData) {
    val refreshToken = revokeTokenErrorData.refreshToken
    val error = AWSCognitoAuthExceptions.RevokeTokenException(revokeTokenErrorData.error)
}
