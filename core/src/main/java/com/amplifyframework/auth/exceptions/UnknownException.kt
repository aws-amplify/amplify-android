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
 * Auth exception caused by unknown reason
 * @param message Explains the reason for the exception
 * @param cause The original error.
 */
open class UnknownException(
    message: String = "An unclassified error prevented this operation.",
    cause: Throwable? = null
) : AuthException(
    message,
    if (cause == null) RECOVERY_SUGGESTION_WITH_THROWABLE else RECOVERY_SUGGESTION_WITHOUT_THROWABLE,
    cause
) {
    companion object {
        const val RECOVERY_SUGGESTION_WITH_THROWABLE = "See the attached exception for more details"
        const val RECOVERY_SUGGESTION_WITHOUT_THROWABLE = "Sorry, we don't have a suggested fix for this error yet."
    }
}
