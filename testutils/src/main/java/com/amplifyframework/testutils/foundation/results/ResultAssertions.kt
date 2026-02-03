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

package com.amplifyframework.testutils.foundation.results

import com.amplifyframework.foundation.results.Result
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

private fun <T, E> beFailure() = Matcher<Result<T, E>> { value ->
    MatcherResult(
        value is Result.Failure,
        { "result expected to be failure but was success" },
        { "result expected to not be failure but was failure" }
    )
}

private fun <T, E> beSuccess() = Matcher<Result<T, E>> { value ->
    MatcherResult(
        value is Result.Success,
        { "result expected to be success but was failure" },
        { "result expected to not be success but was success" }
    )
}

fun <T, E> Result<T, E>.shouldBeFailure(): Result.Failure<E> {
    this should beFailure()
    return this as Result.Failure
}

fun <T, E> Result<T, E>.shouldBeSuccess(): Result.Success<T> {
    this should beSuccess()
    return this as Result.Success
}

infix fun <T, E> Result<T, E>.shouldBeFailure(expected: E): Result.Failure<E> {
    this should beFailure()
    val failure = this as Result.Failure
    failure.error shouldBe expected
    return failure
}

infix fun <T, E> Result<T, E>.shouldBeSuccess(expected: T): Result.Success<T> {
    this should beSuccess()
    val success = this as Result.Success
    success.data shouldBe expected
    return success
}
