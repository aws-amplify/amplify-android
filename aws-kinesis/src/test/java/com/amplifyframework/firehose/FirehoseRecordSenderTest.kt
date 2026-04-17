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
package com.amplifyframework.firehose

import aws.sdk.kotlin.services.firehose.FirehoseClient
import com.amplifyframework.recordcache.Record
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirehoseRecordSenderTest {

    private val testStreamName = "test-delivery-stream"
    private val maxRetries = 3
    private val mockClient = mockk<FirehoseClient>()

    @Test
    fun `createRequest should construct correct PutRecordBatchRequest`() = runTest {
        val recordSender = FirehoseRecordSender(mockClient, maxRetries)

        val records = listOf(
            createTestRecord(1L, byteArrayOf(1, 2, 3)),
            createTestRecord(2L, byteArrayOf(4, 5, 6))
        )

        val request = recordSender.createRequest(testStreamName, records)

        request.deliveryStreamName shouldBe testStreamName
        request.records?.size shouldBe 2
        request.records?.get(0)?.data shouldBe byteArrayOf(1, 2, 3)
        request.records?.get(1)?.data shouldBe byteArrayOf(4, 5, 6)
    }

    @Test
    fun `createRequest should not include partition keys`() = runTest {
        val recordSender = FirehoseRecordSender(mockClient, maxRetries)

        val records = listOf(
            createTestRecord(1L, byteArrayOf(1, 2, 3))
        )

        val request = recordSender.createRequest(testStreamName, records)

        // Firehose records don't have partition keys (unlike Kinesis)
        request.deliveryStreamName shouldBe testStreamName
        request.records?.size shouldBe 1
    }

    private fun createTestRecord(id: Long, data: ByteArray, retryCount: Int = 0): Record = Record(
        id = id,
        streamName = testStreamName,
        partitionKey = null,
        data = data,
        dataSize = data.size,
        retryCount = retryCount,
        createdAt = System.currentTimeMillis()
    )
}
