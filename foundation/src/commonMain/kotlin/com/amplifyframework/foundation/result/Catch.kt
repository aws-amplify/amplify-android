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

package com.amplifyframework.foundation.result

import com.amplifyframework.annotations.InternalAmplifyApi
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs the supplied block and returns the return value as a Result.Success. If an [Exception] is thrown it returns
 * it as a Result.Failure.
 *
 * [CancellationException] is re-thrown so that coroutine cancellation is not swallowed, and [Error]s are not caught
 * at all as they indicate unrecoverable conditions.
 */
@InternalAmplifyApi
inline fun <T> resultCatching(block: () -> T): Result<T, Exception> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Failure(e)
    }
}
