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

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordClientConcurrentFlushTest {

    private lateinit var storage: SQLiteRecordStorage
    private lateinit var mockSender: RecordSender
    private lateinit var recordClient: RecordClient

    @Before
    fun setup() {
        storage = SQLiteRecordStorage(
            maxRecordsByStream = 1000,
            cacheMaxBytes = 1024 * 1024L,
            identifier = "test_concurrent",
            connectionFactory = { BundledSQLiteDriver().open(":memory:") },
            maxRecordSizeBytes = 10L * 1_024 * 1_024,
            maxBytesPerStream = 10L * 1_024 * 1_024,
            maxPartitionKeyLength = 256,
            dispatcher = Dispatchers.IO
        )
        mockSender = mockk()
        recordClient = RecordClient(mockSender, storage, maxRetries = 3)
    }

    @Test
    fun `concurrent flush should return flushInProgress for second caller`() = runTest {
        val streamName = "test-stream"
        repeat(5) { i ->
            storage.addRecord(RecordInput(streamName, "key$i", byteArrayOf(i.toByte()))).getOrThrow()
        }

        val allRecords = storage.getRecordsByStream().getOrThrow().flatten()

        // Make the sender slow so the first flush holds the lock
        coEvery { mockSender.putRecords(streamName, any()) } coAnswers {
            delay(500)
            Result.Success(
                PutRecordsResponse(
                    successfulIds = allRecords.map { it.id },
                    retryableIds = emptyList(),
                    failedIds = emptyList()
                )
            )
        }

        val flush1 = async(Dispatchers.Default) { recordClient.flush() }
        delay(50) // Give flush1 time to acquire the lock
        val flush2 = async(Dispatchers.Default) { recordClient.flush() }

        val result1 = flush1.await()
        val result2 = flush2.await()

        // One should have done work, the other should report flushInProgress
        val results = listOf(result1, result2)
        val successResults = results.map { it.shouldBeSuccess() }

        val anyFlushed = successResults.any { it.data.recordsFlushed > 0 }
        val anyInProgress = successResults.any { it.data.flushInProgress }

        anyFlushed shouldBe true
        anyInProgress shouldBe true
    }
}
