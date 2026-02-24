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
import kotlin.contracts.contract

/**
 * Returns the Success data or throws the Failure error
 */
@InternalAmplifyApi
fun <T, E : Throwable> Result<T, E>.getOrThrow(): T {
    contract {
        returns() implies (this@getOrThrow is Result.Success)
    }
    return when (this) {
        is Result.Failure -> throw error
        is Result.Success -> data
    }
}

/**
 * Returns the Success data or null in the case of Failure
 */
@InternalAmplifyApi
fun <T> Result<T, *>.getOrNull(): T? {
    contract {
        returnsNotNull() implies (this@getOrNull is Result.Success)
        returns(null) implies (this@getOrNull is Result.Failure)
    }
    return if (this is Result.Success) data else null
}
