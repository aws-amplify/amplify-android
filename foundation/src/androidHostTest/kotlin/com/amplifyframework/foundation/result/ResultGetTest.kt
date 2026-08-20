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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.IOException
import org.junit.Test

class ResultGetTest {
    @Test
    fun `getOrNull returns data for success`() {
        val success: Result<Int, String> = Result.Success(15)
        success.getOrNull() shouldBe 15
    }

    @Test
    fun `getOrNull returns null for failure`() {
        val failure: Result<Int, String> = Result.Failure("failed")
        failure.getOrNull().shouldBeNull()
    }

    @Test
    fun `getOrThrow returns data for success`() {
        val success = Result.Success("yay")
        success.getOrThrow() shouldBe "yay"
    }

    @Test
    fun `getOrThrow throws exception for failure`() {
        shouldThrow<IOException> {
            val failure = Result.Failure(IOException("failed"))
            failure.getOrThrow()
        }
    }
}
