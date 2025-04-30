/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.exceptions.webauthn

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.exceptions.UnknownException

/**
 * A non-specific exception that indicates a failure interacting with Android's CredentialManager APIs
 */
open class WebAuthnFailedException internal constructor(
    message: String,
    recoverySuggestion: String? = null,
    cause: Throwable? = null
) : AuthException(
    message = message,
    recoverySuggestion = recoverySuggestion
        ?: if (cause == null) {
            UnknownException.RECOVERY_SUGGESTION_WITHOUT_THROWABLE
        } else {
            UnknownException.RECOVERY_SUGGESTION_WITH_THROWABLE
        },
    cause = cause
)
