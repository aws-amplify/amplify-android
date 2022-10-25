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
 * Auth exception caused by invalid input.
 * @param message Explains the reason for the exception
 * @param recoverySuggestion Text suggesting a way to recover from the error being described
 * @param cause The original error.
 */
open class ValidationException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AuthException(message, recoverySuggestion, cause)
