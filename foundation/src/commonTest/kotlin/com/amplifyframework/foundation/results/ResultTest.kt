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

package com.amplifyframework.foundation.results

import com.amplifyframework.testutils.foundation.results.shouldBeFailure
import com.amplifyframework.testutils.foundation.results.shouldBeSuccess
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.io.IOException
import org.junit.Test

class ResultTest {

    enum class OriginalFailure {
        BadNetwork,
        DnsIssue,
        InvalidInput,
        InputTooLong
    }

    enum class MappedFailure {
        NetworkProblem,
        InputProblem
    }

    @Test
    fun `getOrThrow throws exception for failure`() {
        shouldThrow<IOException> {
            val failure = Result.Failure(IOException("failed"))
            failure.getOrThrow()
        }
    }

    @Test
    fun `getOrThrow returns data for success`() {
        val success = Result.Success("yay")
        success.getOrThrow() shouldBe "yay"
    }

    @Test
    fun `mapFailure maps failure`() {
        val mapped = Result.Failure(OriginalFailure.BadNetwork).mapFailure(::failureMapper)
        mapped shouldBeFailure MappedFailure.NetworkProblem
    }

    @Test
    fun `mapFailure does not map success as failure`() {
        val mapped = Result.Success(5).mapFailure(::failureMapper)
        mapped shouldBeSuccess 5
    }

    @Test
    fun `mapSuccess maps success`() {
        val mapped = Result.Success(3).mapSuccess(::successMapper)
        mapped shouldBeSuccess 30
    }

    @Test
    fun `mapSuccess does not map failure as success`() {
        val mapped = Result.Failure(OriginalFailure.DnsIssue).mapSuccess(::successMapper)
        mapped shouldBeFailure OriginalFailure.DnsIssue
    }

    @Test
    fun `mapBoth maps failure`() {
        val mapped = Result.Failure(OriginalFailure.InputTooLong).mapBoth(::successMapper, ::failureMapper)
        mapped shouldBeFailure MappedFailure.InputProblem
    }

    @Test
    fun `mapBoth maps success`() {
        val mapped = Result.Success(4).mapBoth(::successMapper, ::failureMapper)
        mapped shouldBeSuccess 40
    }

    private fun failureMapper(original: OriginalFailure) = when (original) {
        OriginalFailure.InvalidInput, OriginalFailure.InputTooLong -> MappedFailure.InputProblem
        else -> MappedFailure.NetworkProblem
    }

    private fun successMapper(original: Int) = original * 10
}
