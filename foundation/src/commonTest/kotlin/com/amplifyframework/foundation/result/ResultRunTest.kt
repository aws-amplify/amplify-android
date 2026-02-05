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

import com.amplifyframework.foundation.result.runCatching as localRunCatching
import com.amplifyframework.testutils.foundation.results.shouldBeFailure
import com.amplifyframework.testutils.foundation.results.shouldBeSuccess
import org.junit.Test

class ResultRunTest {
    @Test
    fun `runCatching returns success result`() {
        val result = localRunCatching {
            10
        }
        result shouldBeSuccess 10
    }

    @Test
    fun `runCatching returns failure result for thrown exception`() {
        val exception = RuntimeException("failed")

        val result = localRunCatching {
            throw exception
        }

        result shouldBeFailure exception
    }
}
