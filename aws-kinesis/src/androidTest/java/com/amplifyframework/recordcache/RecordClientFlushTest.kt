package com.amplifyframework.recordcache

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordClientFlushTest {

    private lateinit var storage: SQLiteRecordStorage
    private lateinit var mockSender: TestRecordSender
    private lateinit var recordClient: RecordClient<Exception>

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        storage = SQLiteRecordStorage.forTesting(
            maxRecords = 1000,
            maxBytes = 1024 * 1024L,
            identifier = "test_flush",
            connectionFactory = { BundledSQLiteDriver().open(context.getDatabasePath("test_flush.db").absolutePath) },
            dispatcher = Dispatchers.IO
        )
        mockSender = TestRecordSender()
        recordClient = RecordClient(mockSender, storage) { it }
    }

    @After
    fun cleanup() {
        runTest {
            storage.clearRecords()
        }
    }

    @Test
    fun flushShouldHandleMixedRecordStates() = runTest {
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
        mockSender.mockResponse = PutRecordsResponse(
            successfulIds = listOf(allRecords[0].id),
            retryableIds = listOf(allRecords[1].id),
            failedIds = listOf(record3Id)
        )

        // When
        val result = recordClient.flush()

        // Then
        assertTrue(result.isSuccess)

        // Verify final state
        val remainingRecordsByStream = storage.getRecordsByStream().getOrThrow()
        val remainingRecords = remainingRecordsByStream.flatten()
        assertEquals(1, remainingRecords.size)
        assertEquals(allRecords[1].id, remainingRecords[0].id)
        assertEquals(1, remainingRecords[0].retryCount)
    }

    private class TestRecordSender : RecordSender {
        var mockResponse: PutRecordsResponse? = null

        override suspend fun putRecords(streamName: String, records: List<Record>): Result<PutRecordsResponse> =
            Result.success(mockResponse ?: PutRecordsResponse(emptyList(), emptyList(), emptyList()))
    }
}
