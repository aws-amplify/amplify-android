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

import com.amplifyframework.testutils.assertions.shouldBeFailure
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import org.junit.Test

class ResultCatchingTest {
    @Test
    fun `resultCatching returns success result`() {
        val result = resultCatching {
            10
        }
        result shouldBeSuccess 10
    }

    @Test
    fun `resultCatching returns failure result for thrown exception`() {
        val exception = RuntimeException("failed")

        val result = resultCatching {
            throw exception
        }

        result shouldBeFailure exception
    }
}
