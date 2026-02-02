package com.amplifyframework.recordcache

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SQLiteRecordStorageCacheAccuracyTest {

    private fun createTestStorage(): SQLiteRecordStorage {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return SQLiteRecordStorage(
            maxRecords = 1000,
            maxBytes = 1024 * 1024L,
            identifier = "test",
            connectionFactory = { BundledSQLiteDriver().open(context.getDatabasePath("test.db").absolutePath) },
            dispatcher = Dispatchers.IO
        )
    }

    @Test
    fun cachedSizeMatchesDatabaseAfterAddOperations() = runTest {
        val storage = createTestStorage()

        val record1 = RecordInput("stream1", "key1", byteArrayOf(1, 2, 3), 3)
        val record2 = RecordInput("stream1", "key2", byteArrayOf(4, 5, 6, 7), 4)

        storage.addRecord(record1).getOrThrow()
        storage.addRecord(record2).getOrThrow()

        val cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 7
    }

    @Test
    fun cachedSizeMatchesDatabaseAfterDeleteOperations() = runTest {
        val storage = createTestStorage()

        // Add records
        val record1 = RecordInput("stream1", "key1", byteArrayOf(1, 2, 3), 3)
        val record2 = RecordInput("stream1", "key2", byteArrayOf(4, 5, 6, 7), 4)
        val record3 = RecordInput("stream2", "key3", byteArrayOf(8, 9), 2)

        storage.addRecord(record1).getOrThrow()
        storage.addRecord(record2).getOrThrow()
        storage.addRecord(record3).getOrThrow()

        // Get record IDs for deletion
        val records = storage.getRecordsByStream().getOrThrow().flatten()
        val idsToDelete = records.take(2).map { it.id }

        storage.deleteRecords(idsToDelete).getOrThrow()

        val cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 2
    }

    @Test
    fun cachedSizeMatchesDatabaseAfterClearOperations() = runTest {
        val storage = createTestStorage()

        // Add records
        storage.addRecord(RecordInput("stream1", "key1", byteArrayOf(1, 2, 3), 3)).getOrThrow()
        storage.addRecord(RecordInput("stream2", "key2", byteArrayOf(4, 5), 2)).getOrThrow()

        storage.clearRecords().getOrThrow()

        val cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 0
    }

    @Test
    fun cachedSizeRemainsAccurateThroughMixedOperations() = runTest {
        val storage = createTestStorage()

        // Complex sequence of operations
        storage.addRecord(RecordInput("stream1", "key1", byteArrayOf(1, 2, 3, 4, 5), 5)).getOrThrow()
        storage.addRecord(RecordInput("stream2", "key2", byteArrayOf(6, 7, 8), 3)).getOrThrow()

        var cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 8

        // Delete one record
        val records = storage.getRecordsByStream().getOrThrow().flatten()
        storage.deleteRecords(listOf(records.first().id)).getOrThrow()

        cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 3

        // Add another record
        storage.addRecord(RecordInput("stream3", "key3", byteArrayOf(9, 10), 2)).getOrThrow()

        cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 5
    }

    @Test
    fun concurrentProducerConsumerOperationsAreThreadSafe() = runTest {
        val storage = createTestStorage()
        val testDurationMs = 2000L
        val recordSize = 10

        val createdRecords = mutableMapOf<String, MutableSet<String>>()
        val deletedRecords = mutableSetOf<String>()

        // Create producer coroutines
        val producers = (0..1).map { producerIndex ->
            async {
                var recordCounter = 0
                val threadRecords = mutableSetOf<String>()
                synchronized(createdRecords) {
                    createdRecords["producer$producerIndex"] = threadRecords
                }

                val endTime = System.currentTimeMillis() + testDurationMs
                while (System.currentTimeMillis() < endTime) {
                    val recordKey = "producer${producerIndex}_record${recordCounter++}"
                    val record = RecordInput(
                        streamName = "stream$producerIndex",
                        partitionKey = recordKey,
                        data = ByteArray(recordSize) { (it + producerIndex).toByte() },
                        dataSize = recordSize
                    )

                    storage.addRecord(record).getOrThrow()
                    synchronized(threadRecords) {
                        threadRecords.add(recordKey)
                    }

                    delay(10)
                }
            }
        }

        // Create consumer coroutines
        val consumers = (0..1).map { consumerIndex ->
            async {
                val endTime = System.currentTimeMillis() + testDurationMs
                while (System.currentTimeMillis() < endTime) {
                    delay(40)

                    val records = storage.getRecordsByStream().getOrThrow().flatten()
                    if (records.isNotEmpty()) {
                        val recordsToDelete = records.take(2)
                        val idsToDelete = recordsToDelete.map { it.id }
                        val keysToDelete = recordsToDelete.map { it.partitionKey }

                        storage.deleteRecords(idsToDelete).getOrThrow()

                        synchronized(deletedRecords) {
                            deletedRecords.addAll(keysToDelete)
                        }
                    }
                }
            }
        }

        // Wait for all coroutines to complete
        (producers + consumers).awaitAll()

        // Verify data integrity
        val finalRecords = storage.getRecordsByStream().getOrThrow().flatten()
        val finalCacheSize = storage.getCurrentCacheSize().getOrThrow()

        val expectedCacheSize = finalRecords.sumOf { it.dataSize.toInt() }
        finalCacheSize shouldBe expectedCacheSize

        val remainingKeys = finalRecords.map { it.partitionKey }.toSet()
        val allCreatedKeys = createdRecords.values.flatten().toSet()

        for (createdKey in allCreatedKeys) {
            val isInDb = remainingKeys.contains(createdKey)
            val wasDeleted = deletedRecords.contains(createdKey)

            (isInDb || wasDeleted) shouldBe true

            (isInDb && wasDeleted) shouldBe false
        }

        for (remainingKey in remainingKeys) {
            allCreatedKeys.contains(remainingKey) shouldBe true
        }

        allCreatedKeys.shouldNotBeEmpty()
        deletedRecords.shouldNotBeEmpty()
        remainingKeys.shouldNotBeEmpty()
    }
}
