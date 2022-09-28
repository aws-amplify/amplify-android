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

/**
 * Wraps the result of a Sign Out operation
 */
sealed class AWSCognitoAuthSignOutResult : AuthSignOutResult() {

    /**
     * Indicates if credentials have been cleared from local device
     */
    abstract val signedOutLocally: Boolean

    /**
     * Indicates a successful sign out with no errors
     */
    object CompleteSignOut : AWSCognitoAuthSignOutResult() {

        /**
         * Indicates if credentials have been cleared from local device
         */
        override val signedOutLocally = true
    }

    /**
     * Indicates a failed sign out that did not complete. The user will remain signed in
     * @param error that occurred during sign out
     */
    data class FailedSignOut internal constructor(val error: AuthException) : AWSCognitoAuthSignOutResult() {

        /**
         * Indicates if credentials have been cleared from local device
         */
        override val signedOutLocally = false
    }

    /**
     * Indicates a partially successful sign out where local credentials have been cleared from the device.
     * @param hostedUIError An error occurred during hosted ui sign out
     * @param globalSignOutError Global sign out failed. Use escape hatch with returned credentials to retry.
     * @param revokeTokenError Revoking token failed. Use escape hatch with returned token to retry.
     */
    data class PartialSignOut internal constructor(
        val hostedUIError: HostedUIError? = null,
        val globalSignOutError: GlobalSignOutError? = null,
        val revokeTokenError: RevokeTokenError? = null,
    ) : AWSCognitoAuthSignOutResult() {

        /**
         * Indicates if credentials have been cleared from local device
         */
        override val signedOutLocally = true
    }
}

/**
 * HostedUI Sign Out Error
 * @param hostedUIErrorData Information about failed hosted ui sign out.
 */
class HostedUIError internal constructor(hostedUIErrorData: HostedUIErrorData) {

    /**
     * Error containing information about hosted ui sign out failure
     */
    val error = hostedUIErrorData.error
}

/**
 * Global Sign Out Error
 * @param globalSignOutErrorData Information about failed global sign out.
 */
class GlobalSignOutError internal constructor(globalSignOutErrorData: GlobalSignOutErrorData) {
    /**
     * accessToken that failed global sign out. Escape hatch can be used to retry global sign out.
     */
    val accessToken = globalSignOutErrorData.accessToken

    /**
     * Error containing information about global sign out failure
     */
    val error = AWSCognitoAuthExceptions.GlobalSignOutException(globalSignOutErrorData.error)
}

/**
 * Revoke Token Error
 * @param revokeTokenErrorData Information about failed global sign out.
 */
class RevokeTokenError internal constructor(revokeTokenErrorData: RevokeTokenErrorData) {

    /**
     * refreshToken that failed to be revoked. Escape hatch can be used to retry revoke.
     */
    val refreshToken = revokeTokenErrorData.refreshToken

    /**
     * Error containing information about revoke token failure
     */
    val error = AWSCognitoAuthExceptions.RevokeTokenException(revokeTokenErrorData.error)
}
