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
package com.amplifyframework.kinesis

import aws.sdk.kotlin.services.kinesis.KinesisClient
import com.amplifyframework.recordcache.Record
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KinesisRecordSenderTest {

    private val testStreamName = "test-stream"
    private val maxRetries = 3
    private val mockClient = mockk<KinesisClient>()

    @Test
    fun `createRequest should construct correct PutRecordsRequest`() = runTest {
        // Given
        val recordSender = KinesisRecordSender(mockClient, maxRetries)

        val records = listOf(
            createTestRecord(1L, "key1", byteArrayOf(1, 2, 3)),
            createTestRecord(2L, "key2", byteArrayOf(4, 5, 6))
        )

        // When
        val request = recordSender.createRequest(testStreamName, records)

        // Then
        request.streamName shouldBe testStreamName
        request.records?.size shouldBe 2
        request.records?.get(0)?.partitionKey shouldBe "key1"
        request.records?.get(0)?.data shouldBe byteArrayOf(1, 2, 3)
        request.records?.get(1)?.partitionKey shouldBe "key2"
        request.records?.get(1)?.data shouldBe byteArrayOf(4, 5, 6)
    }

    private fun createTestRecord(id: Long, partitionKey: String, data: ByteArray, retryCount: Int = 0): Record = Record(
        id = id,
        streamName = testStreamName,
        partitionKey = partitionKey,
        data = data,
        dataSize = data.size,
        retryCount = retryCount,
        createdAt = System.currentTimeMillis()
    )
}
