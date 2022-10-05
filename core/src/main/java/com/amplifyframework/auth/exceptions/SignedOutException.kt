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
package com.amplifyframework.auth.exceptions

import com.amplifyframework.auth.AuthException

/**
 * Auth exception caused by the user being signed out.
 * @param message Explains the reason for the exception
 * @param recoverySuggestion Text suggesting a way to recover from the error being described
 * @param cause The original error.
 */
open class SignedOutException(
    message: String = "Your session has expired.",
    recoverySuggestion: String = RECOVERY_SUGGESTION_GUEST_ACCESS_DISABLED,
    cause: Throwable? = null
) : AuthException(message, recoverySuggestion, cause) {
    companion object {
        const val RECOVERY_SUGGESTION_GUEST_ACCESS_DISABLED = "Please sign in and reattempt the operation."
        const val RECOVERY_SUGGESTION_GUEST_ACCESS_POSSIBLE = "If you have guest access enabled, please check " +
            "that your device is online and try again. Otherwise if guest access is not enabled, you'll " +
            "need to sign in and try again."
        const val RECOVERY_SUGGESTION_GUEST_ACCESS_ENABLED = "For guest access, please check that your device " +
            "is online and try again. For normal user access, please sign in."
    }
}
