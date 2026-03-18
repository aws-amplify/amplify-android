/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.recordcache

import com.amplifyframework.foundation.result.Result
import kotlin.system.measureTimeMillis

/**
 * Executes [operation] and logs success/failure with elapsed time.
 */
internal suspend inline fun <T, E : Throwable> logOp(
    operation: suspend () -> Result<T, E>,
    logSuccess: (T, Long) -> Unit,
    logFailure: (Throwable?, Long) -> Unit
): Result<T, E> {
    val result: Result<T, E>
    val timeMs = measureTimeMillis {
        result = operation()
    }
    when (result) {
        is Result.Failure -> logFailure(result.error, timeMs)
        is Result.Success -> logSuccess(result.data, timeMs)
    }
    return result
}
