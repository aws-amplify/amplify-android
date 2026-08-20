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

/**
 * The result of a single operation.
 */
sealed interface Result<out T, out E> {
    /**
     * Result type that indicates the operation was successful
     */
    data class Success<out T>(val data: T) : Result<T, Nothing>

    /**
     * Result type that indicates the operation was not successful
     */
    data class Failure<out E>(val error: E) : Result<Nothing, E>
}
