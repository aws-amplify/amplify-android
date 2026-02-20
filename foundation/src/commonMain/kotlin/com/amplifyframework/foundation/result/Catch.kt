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

/**
 * Runs the supplied block and returns the return value as a Result.Success. If an exception is thrown it returns
 * it as a Result.Failure.
 */
@InternalAmplifyApi

inline fun <T> resultCatching(block: () -> T): Result<T, Throwable> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        Result.Success(block())
    } catch (e: Throwable) {
        Result.Failure(e)
    }
}
