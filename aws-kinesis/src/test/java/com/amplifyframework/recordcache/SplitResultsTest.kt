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

package com.amplifyframework.recordcache

import io.kotest.matchers.shouldBe
import org.junit.Test

class SplitResultsTest {

    private val maxRetries = 3

    @Test
    fun `all records successful`() {
        val records = listOf(
            testRecord(1L, retryCount = 0),
            testRecord(2L, retryCount = 0)
        )
        val result = splitResults(
            errorCodes = listOf(null, null),
            records = records,
            maxRetries = maxRetries
        )
        result.successfulIds shouldBe listOf(1L, 2L)
        result.retryableIds shouldBe emptyList()
        result.failedIds shouldBe emptyList()
    }

    @Test
    fun `retryable record below max retries`() {
        val records = listOf(
            testRecord(1L, retryCount = 0),
            testRecord(2L, retryCount = 1)
        )
        val result = splitResults(
            errorCodes = listOf(null, "ProvisionedThroughputExceededException"),
            records = records,
            maxRetries = maxRetries
        )
        result.successfulIds shouldBe listOf(1L)
        result.retryableIds shouldBe listOf(2L)
        result.failedIds shouldBe emptyList()
    }

    @Test
    fun `failed record at max retries`() {
        val records = listOf(
            testRecord(1L, retryCount = 0),
            testRecord(2L, retryCount = maxRetries)
        )
        val result = splitResults(
            errorCodes = listOf(null, "InternalFailure"),
            records = records,
            maxRetries = maxRetries
        )
        result.successfulIds shouldBe listOf(1L)
        result.retryableIds shouldBe emptyList()
        result.failedIds shouldBe listOf(2L)
    }

    @Test
    fun `mixed success, retry, and fail`() {
        val records = listOf(
            testRecord(1L, retryCount = 0),
            testRecord(2L, retryCount = 1),
            testRecord(3L, retryCount = maxRetries)
        )
        val result = splitResults(
            errorCodes = listOf(null, "ProvisionedThroughputExceededException", "InternalFailure"),
            records = records,
            maxRetries = maxRetries
        )
        result.successfulIds shouldBe listOf(1L)
        result.retryableIds shouldBe listOf(2L)
        result.failedIds shouldBe listOf(3L)
    }

    private fun testRecord(id: Long, retryCount: Int = 0) = Record(
        id = id,
        streamName = "test-stream",
        data = byteArrayOf(1),
        dataSize = 1,
        retryCount = retryCount,
        createdAt = System.currentTimeMillis()
    )
}
