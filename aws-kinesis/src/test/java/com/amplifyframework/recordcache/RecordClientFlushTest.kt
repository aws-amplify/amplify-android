package com.amplifyframework.recordcache

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.foundation.result.isSuccess
import io.kotest.matchers.booleans.shouldBeTrue
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
            maxRecords = 1000,
            maxBytes = 1024 * 1024L,
            identifier = "test_flush",
            connectionFactory = { BundledSQLiteDriver().open(":memory:") },
            dispatcher = Dispatchers.IO
        )
        mockSender = mockk()
        recordClient = RecordClient(mockSender, storage)
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
        coEvery { mockSender.putRecords(streamName, any()) } returns Result.Success(
            PutRecordsResponse(
                successfulIds = listOf(allRecords[0].id),
                retryableIds = listOf(allRecords[1].id),
                failedIds = listOf(record3Id)
            )
        )

        // When
        val result = recordClient.flush()

        // Then
        result.isSuccess().shouldBeTrue()

        // Verify final state
        val remainingRecordsByStream = storage.getRecordsByStream().getOrThrow()
        val remainingRecords = remainingRecordsByStream.flatten()
        remainingRecords.size shouldBe 1
        remainingRecords[0].id shouldBe allRecords[1].id
        remainingRecords[0].retryCount shouldBe 1
    }
}
