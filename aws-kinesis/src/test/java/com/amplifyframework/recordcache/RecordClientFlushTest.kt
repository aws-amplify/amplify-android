package com.amplifyframework.recordcache

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.testutils.assertions.shouldBeFailure
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordClientFlushTest {

    private lateinit var storage: SQLiteRecordStorage
    private lateinit var mockSender: RecordSender
    private lateinit var recordClient: RecordClient

    @Before
    fun setup() {
        storage = SQLiteRecordStorage(
            maxRecordsByStream = 1000,
            cacheMaxBytes = 1024 * 1024L,
            identifier = "test_flush",
            connectionFactory = { BundledSQLiteDriver().open(":memory:") },
            maxRecordSizeBytes = 10L * 1_024 * 1_024,
            maxBytesPerStream = 10L * 1_024 * 1_024,
            maxPartitionKeyLength = 256,
            dispatcher = Dispatchers.IO
        )
        mockSender = mockk()
        recordClient = RecordClient(mockSender, storage, maxRetries = 3)
    }

    @After
    fun cleanup() {
        runTest {
            storage.clearRecords()
        }
    }

    @Test
    fun `flush should handle mixed record states correctly`() = runTest {
        // Given: Records with different states
        val streamName = "test-stream"

        // Add records to storage
        val record1 = RecordInput(streamName, "key1", byteArrayOf(1))
        val record2 = RecordInput(streamName, "key2", byteArrayOf(2))
        val record3 = RecordInput(streamName, "key3", byteArrayOf(3))

        storage.addRecord(record1).getOrThrow() // Will succeed
        storage.addRecord(record2).getOrThrow() // Will retry
        storage.addRecord(record3).getOrThrow() // Will fail (max retries)

        // Get all records and set retry count for record 3 to max (3)
        val allRecordsByStream = storage.getRecordsByStream().getOrThrow()
        val allRecords = allRecordsByStream.flatten() // Flatten the list of lists
        val record3Id = allRecords[2].id
        storage.incrementRetryCount(listOf(record3Id)).getOrThrow()
        storage.incrementRetryCount(listOf(record3Id)).getOrThrow()
        storage.incrementRetryCount(listOf(record3Id)).getOrThrow() // Now at max retries (3)

        // Configure mock sender response
        coEvery { mockSender.putRecords(streamName, any()) } returns
            Result.Success(
                PutRecordsResponse(
                    successfulIds = listOf(allRecords[0].id),
                    retryableIds = listOf(allRecords[1].id),
                    failedIds = listOf(record3Id)
                )
            )

        // When
        val result = recordClient.flush()

        // Then
        result.shouldBeSuccess()

        // Verify final state
        val remainingRecordsByStream = storage.getRecordsByStream().getOrThrow()
        val remainingRecords = remainingRecordsByStream.flatten()
        remainingRecords.size shouldBe 1
        remainingRecords[0].id shouldBe allRecords[1].id
        remainingRecords[0].retryCount shouldBe 1
    }

    @Test
    fun `flush should increment retry count for all records when request fails`() = runTest {
        // Given: Records with various retry counts
        val streamName = "test-stream"
        storage.addRecord(RecordInput(streamName, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key2", byteArrayOf(2))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key3", byteArrayOf(3))).getOrThrow()

        // Configure mock sender to fail
        coEvery { mockSender.putRecords(streamName, any()) } returns
            Result.Failure(RuntimeException("Network error"))

        // When
        val result = recordClient.flush()

        // Then
        result.shouldBeFailure() // Should return failure

        // Verify all records had retry count incremented
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 3
        remainingRecords.forEach { record ->
            record.retryCount shouldBe 1
        }
    }

    @Test
    fun `flush should delete records at max retries when request fails`() = runTest {
        // Given: Records at different retry counts
        val streamName = "test-stream"
        storage.addRecord(RecordInput(streamName, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key2", byteArrayOf(2))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key3", byteArrayOf(3))).getOrThrow()

        // Set record 2 and 3 to max retries
        val allRecords = storage.getRecordsByStream().getOrThrow().flatten()
        val record2Id = allRecords[1].id
        val record3Id = allRecords[2].id
        
        repeat(3) { storage.incrementRetryCount(listOf(record2Id, record3Id)).getOrThrow() }

        // Configure mock sender to fail
        coEvery { mockSender.putRecords(streamName, any()) } returns
            Result.Failure(RuntimeException("Network error"))

        // When
        val result = recordClient.flush()

        // Then
        result.shouldBeFailure()

        // Verify only record 1 remains (records 2 and 3 were deleted)
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 1
        remainingRecords[0].id shouldBe allRecords[0].id
        remainingRecords[0].retryCount shouldBe 1
    }

    @Test
    fun `flush should attempt all streams even when one fails`() = runTest {
        // Given: Records in two different streams
        val stream1 = "stream-1"
        val stream2 = "stream-2"
        storage.addRecord(RecordInput(stream1, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(stream2, "key2", byteArrayOf(2))).getOrThrow()

        // Configure mock sender: stream1 fails, stream2 succeeds
        coEvery { mockSender.putRecords(stream1, any()) } returns
            Result.Failure(RuntimeException("Stream 1 error"))
        coEvery { mockSender.putRecords(stream2, any()) } returns
            Result.Success(
                PutRecordsResponse(
                    successfulIds = storage.getRecordsByStream().getOrThrow()
                        .flatten()
                        .filter { it.streamName == stream2 }
                        .map { it.id },
                    retryableIds = emptyList(),
                    failedIds = emptyList()
                )
            )

        // When
        val result = recordClient.flush()

        // Then - should return failure (first error) but still process stream2
        result.shouldBeFailure()

        // Verify stream2 record was deleted (sent successfully)
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 1
        remainingRecords[0].streamName shouldBe stream1
        remainingRecords[0].retryCount shouldBe 1
    }

    @Test
    fun `flush should return first error when multiple streams fail`() = runTest {
        // Given: Records in two streams
        val stream1 = "stream-1"
        val stream2 = "stream-2"
        storage.addRecord(RecordInput(stream1, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(stream2, "key2", byteArrayOf(2))).getOrThrow()

        val firstError = RuntimeException("First error")
        val secondError = RuntimeException("Second error")

        // Configure mock sender: both streams fail
        coEvery { mockSender.putRecords(stream1, any()) } returns Result.Failure(firstError)
        coEvery { mockSender.putRecords(stream2, any()) } returns Result.Failure(secondError)

        // When
        val result = recordClient.flush()

        // Then - should return the first error encountered
        result.shouldBeFailure()

        // Both records should have retry count incremented
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 2
        remainingRecords.forEach { it.retryCount shouldBe 1 }
    }
}