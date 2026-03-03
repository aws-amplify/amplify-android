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
    fun `flush should increment retry count for all records when request fails with non-SDK error`() = runTest {
        // Given: Records with various retry counts
        val streamName = "test-stream"
        storage.addRecord(RecordInput(streamName, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key2", byteArrayOf(2))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key3", byteArrayOf(3))).getOrThrow()

        // Configure mock sender to fail with a non-SDK error (e.g., network error)
        coEvery { mockSender.putRecords(streamName, any()) } returns
            Result.Failure(RuntimeException("Network error"))

        // When
        val result = recordClient.flush()

        // Then
        result.shouldBeFailure() // Should return failure for non-SDK errors

        // Verify all records had retry count incremented
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 3
        remainingRecords.forEach { record ->
            record.retryCount shouldBe 1
        }
    }

    @Test
    fun `flush should delete records at max retries when request fails with non-SDK error`() = runTest {
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

        // Configure mock sender to fail with a non-SDK error
        coEvery { mockSender.putRecords(streamName, any()) } returns
            Result.Failure(RuntimeException("Network error"))

        // When
        val result = recordClient.flush()

        // Then
        result.shouldBeFailure() // Non-SDK errors should fail

        // Verify only record 1 remains (records 2 and 3 were deleted)
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 1
        remainingRecords[0].id shouldBe allRecords[0].id
        remainingRecords[0].retryCount shouldBe 1
    }

    @Test
    fun `flush should attempt all streams even when one fails with non-SDK error`() = runTest {
        // Given: Records in two different streams
        val stream1 = "stream-1"
        val stream2 = "stream-2"
        storage.addRecord(RecordInput(stream1, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(stream2, "key2", byteArrayOf(2))).getOrThrow()

        // Configure mock sender: stream1 fails with non-SDK error, stream2 succeeds
        coEvery { mockSender.putRecords(stream1, any()) } returns
            Result.Failure(RuntimeException("Network error"))
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

        // Then - should return failure and stop processing (stream2 not attempted)
        result.shouldBeFailure()

        // Verify stream1 record has retry count incremented, stream2 record still exists
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 2
        remainingRecords.find { it.streamName == stream1 }!!.retryCount shouldBe 1
        remainingRecords.find { it.streamName == stream2 }!!.retryCount shouldBe 0 // Not processed
    }

    @Test
    fun `flush should return first critical error and stop processing remaining streams`() = runTest {
        // Given: Records in two streams
        val stream1 = "stream-1"
        val stream2 = "stream-2"
        storage.addRecord(RecordInput(stream1, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(stream2, "key2", byteArrayOf(2))).getOrThrow()

        val firstError = RuntimeException("First network error")
        val secondError = RuntimeException("Second network error")

        // Configure mock sender: both streams fail with non-SDK errors
        coEvery { mockSender.putRecords(stream1, any()) } returns Result.Failure(firstError)
        coEvery { mockSender.putRecords(stream2, any()) } returns Result.Failure(secondError)

        // When
        val result = recordClient.flush()

        // Then - should return failure and stop after first critical error
        result.shouldBeFailure()

        // Only stream1 should have retry count incremented (stream2 not processed)
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 2
        remainingRecords.find { it.streamName == stream1 }!!.retryCount shouldBe 1
        remainingRecords.find { it.streamName == stream2 }!!.retryCount shouldBe 0 // Not processed
    }

    @Test
    fun `flush should return success when SDK Kinesis exception occurs`() = runTest {
        // Given: Records in a stream
        val streamName = "test-stream"
        storage.addRecord(RecordInput(streamName, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key2", byteArrayOf(2))).getOrThrow()

        // Configure mock sender to fail with SDK Kinesis exception
        val sdkException = aws.sdk.kotlin.services.kinesis.model.ResourceNotFoundException.invoke {
            message = "Stream not found"
        }
        coEvery { mockSender.putRecords(streamName, any()) } returns Result.Failure(sdkException)

        // When
        val result = recordClient.flush()

        // Then - should return success (SDK errors are silently handled)
        result.shouldBeSuccess()
        result.getOrThrow().recordsFlushed shouldBe 0

        // Verify records had retry count incremented (will be retried later)
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 2
        remainingRecords.forEach { record ->
            record.retryCount shouldBe 1
        }
    }

    @Test
    fun `flush should return success when one stream has SDK error and another succeeds`() = runTest {
        // Given: Records in two streams
        val stream1 = "stream-1"
        val stream2 = "stream-2"
        storage.addRecord(RecordInput(stream1, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(stream2, "key2", byteArrayOf(2))).getOrThrow()

        val allRecords = storage.getRecordsByStream().getOrThrow().flatten()
        val stream2RecordId = allRecords.first { it.streamName == stream2 }.id

        // Configure mock sender: stream1 fails with SDK error, stream2 succeeds
        val sdkException = aws.sdk.kotlin.services.kinesis.model.ProvisionedThroughputExceededException.invoke {
            message = "Throttled"
        }
        coEvery { mockSender.putRecords(stream1, any()) } returns Result.Failure(sdkException)
        coEvery { mockSender.putRecords(stream2, any()) } returns
            Result.Success(
                PutRecordsResponse(
                    successfulIds = listOf(stream2RecordId),
                    retryableIds = emptyList(),
                    failedIds = emptyList()
                )
            )

        // When
        val result = recordClient.flush()

        // Then - should return success with count of successfully flushed records
        result.shouldBeSuccess()
        result.getOrThrow().recordsFlushed shouldBe 1

        // Verify stream1 record still exists with incremented retry, stream2 deleted
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 1
        remainingRecords[0].streamName shouldBe stream1
        remainingRecords[0].retryCount shouldBe 1
    }

    @Test
    fun `flush should stop immediately on critical error even if SDK error would occur later`() = runTest {
        // Given: Records in two streams
        val stream1 = "stream-1"
        val stream2 = "stream-2"
        storage.addRecord(RecordInput(stream1, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(stream2, "key2", byteArrayOf(2))).getOrThrow()

        // Configure mock sender: stream1 has critical error, stream2 has SDK error
        val criticalError = RuntimeException("Network error")
        val sdkException = aws.sdk.kotlin.services.kinesis.model.ResourceNotFoundException.invoke {
            message = "Stream not found"
        }
        
        coEvery { mockSender.putRecords(stream1, any()) } returns Result.Failure(criticalError)
        coEvery { mockSender.putRecords(stream2, any()) } returns Result.Failure(sdkException)

        // When
        val result = recordClient.flush()

        // Then - should return failure and stop after critical error
        result.shouldBeFailure()

        // Only stream1 should have retry count incremented (stream2 not processed)
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 2
        remainingRecords.find { it.streamName == stream1 }!!.retryCount shouldBe 1
        remainingRecords.find { it.streamName == stream2 }!!.retryCount shouldBe 0 // Not processed
    }

    @Test
    fun `flush should increment retry and delete records at limit on critical error`() = runTest {
        // Given: Records at various retry counts
        val streamName = "test-stream"
        storage.addRecord(RecordInput(streamName, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key2", byteArrayOf(2))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key3", byteArrayOf(3))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key4", byteArrayOf(4))).getOrThrow()

        // Get initial records and set up retry counts
        val initialRecords = storage.getRecordsByStream().getOrThrow().flatten()
        val record1Id = initialRecords[0].id
        val record2Id = initialRecords[1].id
        val record3Id = initialRecords[2].id
        val record4Id = initialRecords[3].id
        
        // Set record 2 to retry count 1
        storage.incrementRetryCount(listOf(record2Id)).getOrThrow()
        
        // Set record 3 to retry count 2 (will be deleted on next failure since retryCount + 1 >= maxRetries)
        repeat(2) { storage.incrementRetryCount(listOf(record3Id)).getOrThrow() }
        
        // Set record 4 to retry count 2 as well
        repeat(2) { storage.incrementRetryCount(listOf(record4Id)).getOrThrow() }

        // Configure mock sender to fail with critical error
        coEvery { mockSender.putRecords(streamName, any()) } returns
            Result.Failure(RuntimeException("Network error"))

        // When
        val result = recordClient.flush()

        // Then - should return failure
        result.shouldBeFailure()

        // Verify retry counts and deletions
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 2 // Records 3 and 4 should be deleted
        
        // Record 1: 0 -> 1
        remainingRecords.find { it.id == record1Id }!!.retryCount shouldBe 1
        
        // Record 2: 1 -> 2
        remainingRecords.find { it.id == record2Id }!!.retryCount shouldBe 2
        
        // Records 3 and 4: 2 -> deleted (retryCount + 1 >= maxRetries)
        remainingRecords.none { it.id == record3Id } shouldBe true
        remainingRecords.none { it.id == record4Id } shouldBe true
    }

    @Test
    fun `flush should increment retry and delete records at limit on SDK error but return success`() = runTest {
        // Given: Records at various retry counts
        val streamName = "test-stream"
        storage.addRecord(RecordInput(streamName, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key2", byteArrayOf(2))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key3", byteArrayOf(3))).getOrThrow()
        storage.addRecord(RecordInput(streamName, "key4", byteArrayOf(4))).getOrThrow()

        // Get initial records and set up retry counts
        val initialRecords = storage.getRecordsByStream().getOrThrow().flatten()
        val record1Id = initialRecords[0].id
        val record2Id = initialRecords[1].id
        val record3Id = initialRecords[2].id
        val record4Id = initialRecords[3].id
        
        // Set record 2 to retry count 1
        storage.incrementRetryCount(listOf(record2Id)).getOrThrow()
        
        // Set record 3 to retry count 2 (will be deleted on next failure since retryCount + 1 >= maxRetries)
        repeat(2) { storage.incrementRetryCount(listOf(record3Id)).getOrThrow() }
        
        // Set record 4 to retry count 2 as well
        repeat(2) { storage.incrementRetryCount(listOf(record4Id)).getOrThrow() }

        // Configure mock sender to fail with SDK error
        val sdkException = aws.sdk.kotlin.services.kinesis.model.ProvisionedThroughputExceededException.invoke {
            message = "Throttled"
        }
        coEvery { mockSender.putRecords(streamName, any()) } returns Result.Failure(sdkException)

        // When
        val result = recordClient.flush()

        // Then - should return success (SDK errors are silent)
        result.shouldBeSuccess()
        result.getOrThrow().recordsFlushed shouldBe 0

        // Verify retry counts and deletions (same behavior as critical error)
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 2 // Records 3 and 4 should be deleted
        
        // Record 1: 0 -> 1
        remainingRecords.find { it.id == record1Id }!!.retryCount shouldBe 1
        
        // Record 2: 1 -> 2
        remainingRecords.find { it.id == record2Id }!!.retryCount shouldBe 2
        
        // Records 3 and 4: 2 -> deleted (retryCount + 1 >= maxRetries)
        remainingRecords.none { it.id == record3Id } shouldBe true
        remainingRecords.none { it.id == record4Id } shouldBe true
    }

    @Test
    fun `flush should continue processing streams when SDK errors occur`() = runTest {
        // Given: Records in two streams
        val stream1 = "stream-1" // Will have SDK error
        val stream2 = "stream-2" // Will succeed
        
        storage.addRecord(RecordInput(stream1, "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput(stream2, "key2", byteArrayOf(2))).getOrThrow()

        val initialRecords = storage.getRecordsByStream().getOrThrow().flatten()
        val stream2RecordId = initialRecords.find { it.streamName == stream2 }!!.id

        // Configure mock sender
        val sdkException = aws.sdk.kotlin.services.kinesis.model.ResourceNotFoundException.invoke {
            message = "Stream not found"
        }
        coEvery { mockSender.putRecords(stream1, any()) } returns Result.Failure(sdkException)
        coEvery { mockSender.putRecords(stream2, any()) } returns
            Result.Success(
                PutRecordsResponse(
                    successfulIds = listOf(stream2RecordId),
                    retryableIds = emptyList(),
                    failedIds = emptyList()
                )
            )

        // When
        val result = recordClient.flush()

        // Then - should return success (SDK errors are silent)
        result.shouldBeSuccess()
        result.getOrThrow().recordsFlushed shouldBe 1

        // Verify final state
        val remainingRecords = storage.getRecordsByStream().getOrThrow().flatten()
        remainingRecords.size shouldBe 1
        
        // Stream2 should be deleted (successfully flushed)
        remainingRecords.none { it.streamName == stream2 } shouldBe true
        
        // Stream1 should have retry incremented (SDK error)
        remainingRecords.find { it.streamName == stream1 }!!.retryCount shouldBe 1
    }
}