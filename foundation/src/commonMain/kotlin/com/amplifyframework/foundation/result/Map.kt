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

@InternalAmplifyApi
inline infix fun <T, E, E2> Result<T, E>.mapFailure(mapper: (E) -> E2): Result<T, E2> {
    contract {
        callsInPlace(mapper, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Result.Success -> this
        is Result.Failure -> Result.Failure(mapper(this.error))
    }
}

@InternalAmplifyApi
inline infix fun <T, E, T2> Result<T, E>.mapSuccess(mapper: (T) -> T2): Result<T2, E> {
    contract {
        callsInPlace(mapper, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Result.Failure -> this
        is Result.Success -> Result.Success(mapper(this.data))
    }
}

@InternalAmplifyApi
inline fun <T, E, T2, E2> Result<T, E>.mapBoth(mapSuccess: (T) -> T2, mapFailure: (E) -> E2): Result<T2, E2> {
    contract {
        callsInPlace(mapSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(mapFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Result.Failure -> Result.Failure(mapFailure(this.error))
        is Result.Success -> Result.Success(mapSuccess(this.data))
    }
}
