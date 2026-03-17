package com.amplifyframework.recordcache

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.amplifyframework.foundation.result.getOrThrow
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SQLiteRecordStorageCacheAccuracyTest {

    private fun createTestStorage(): SQLiteRecordStorage = SQLiteRecordStorage(
        maxRecordsByStream = 1000,
        cacheMaxBytes = 1024 * 1024L,
        identifier = "test",
        connectionFactory = { BundledSQLiteDriver().open(":memory:") },
        maxRecordSizeBytes = 10L * 1024 * 1024,
        maxBytesPerStream = 10L * 1024 * 1024,
        maxPartitionKeyLength = 256,
        dispatcher = Dispatchers.IO
    )

    @Test
    fun `cached size matches database after add operations`() = runTest {
        val storage = createTestStorage()

        val record1 = RecordInput("stream1", "a", byteArrayOf(1, 2, 3))
        val record2 = RecordInput("stream1", "b", byteArrayOf(4, 5, 6, 7))

        storage.addRecord(record1).getOrThrow()
        storage.addRecord(record2).getOrThrow()

        val cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 9
    }

    @Test
    fun `cached size matches database after delete operations`() = runTest {
        val storage = createTestStorage()

        // Add records
        val record1 = RecordInput("stream1", "a", byteArrayOf(1, 2, 3))
        val record2 = RecordInput("stream1", "b", byteArrayOf(4, 5, 6, 7))
        val record3 = RecordInput("stream2", "c", byteArrayOf(8, 9))

        storage.addRecord(record1).getOrThrow()
        storage.addRecord(record2).getOrThrow()
        storage.addRecord(record3).getOrThrow()

        // Get record IDs for deletion
        val records = storage.getRecordsByStream().getOrThrow().flatten()
        val idsToDelete = records.take(2).map { it.id }

        storage.deleteRecords(idsToDelete).getOrThrow()

        val cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 3
    }

    @Test
    fun `cached size matches database after clear operations`() = runTest {
        val storage = createTestStorage()

        // Add records
        storage.addRecord(RecordInput("stream1", "a", byteArrayOf(1, 2, 3))).getOrThrow()
        storage.addRecord(RecordInput("stream2", "b", byteArrayOf(4, 5))).getOrThrow()

        storage.clearRecords().getOrThrow()

        val cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 0
    }

    @Test
    fun `cached size remains accurate through mixed operations`() = runTest {
        val storage = createTestStorage()

        // Complex sequence of operations
        storage.addRecord(RecordInput("stream1", "a", byteArrayOf(1, 2, 3, 4, 5))).getOrThrow()
        storage.addRecord(RecordInput("stream2", "b", byteArrayOf(6, 7, 8))).getOrThrow()

        var cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 10

        // Delete one record
        val records = storage.getRecordsByStream().getOrThrow().flatten()
        storage.deleteRecords(listOf(records.first().id)).getOrThrow()

        cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 4

        // Add another record
        storage.addRecord(RecordInput("stream3", "c", byteArrayOf(9, 10))).getOrThrow()

        cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 7
    }

    @Test
    fun `concurrent producer consumer operations are thread safe`(): Unit = runBlocking {
        val storage = createTestStorage()
        val recordSize = 10

        val createdRecords = mutableMapOf<String, MutableSet<String>>()
        val deletedRecords = mutableSetOf<String>()

        val producers = (0..3).map { producerIndex ->
            async(Dispatchers.Default) {
                // Use real thread pool
                val threadRecords = mutableSetOf<String>()
                synchronized(createdRecords) {
                    createdRecords["producer$producerIndex"] = threadRecords
                }

                repeat(400) { recordCounter ->
                    val recordKey = "producer${producerIndex}_record$recordCounter"
                    val record = RecordInput(
                        streamName = "stream$producerIndex",
                        partitionKey = recordKey,
                        data = ByteArray(recordSize) { (it + producerIndex).toByte() }
                    )

                    storage.addRecord(record).getOrThrow()
                    synchronized(threadRecords) {
                        threadRecords.add(recordKey)
                    }

                    delay(1)
                }
            }
        }

        // Create consumers
        val consumers = (0..1).map { consumerIndex ->
            async(Dispatchers.Default) {
                println("Consumer $consumerIndex running on thread: ${Thread.currentThread().name}")
                repeat(100) { _ ->
                    delay(1)
                    val records = storage.getRecordsByStream().getOrThrow().flatten()
                    if (records.isNotEmpty()) {
                        val recordsToDelete = records.take(1)
                        val idsToDelete = recordsToDelete.map { it.id }
                        val keysToDelete = recordsToDelete.map { it.partitionKey }

                        // Note, other consumer might already have deleted what we're trying to delete
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

        // Log final counts
        val totalCreated = createdRecords.values.sumOf { it.size }
        val totalDeleted = deletedRecords.size

        // Verify data integrity
        val finalRecords = storage.getRecordsByStream().getOrThrow().flatten()
        println("Created $totalCreated records, deleted $totalDeleted records, found in DB ${finalRecords.size}")

        val finalCacheSize = storage.getCurrentCacheSize().getOrThrow()
        // Reset and get cache size
        storage.resetCacheSizeFromDb()
        val actualCacheSize = storage.getCurrentCacheSize().getOrThrow()
        // Size of what we should have written
        val expectedCacheSize = finalRecords.sumOf { it.dataSize }

        finalCacheSize shouldBe actualCacheSize
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

    @Test
    fun `getRecordsByStream respects per-stream byte limit across multiple streams`() = runTest {
        // Storage with a large cache but a tight 200-byte per-stream limit
        val storage = SQLiteRecordStorage(
            maxRecordsByStream = 100,
            cacheMaxBytes = 10_000L,
            identifier = "test_per_stream",
            connectionFactory = { BundledSQLiteDriver().open(":memory:") },
            maxRecordSizeBytes = 1024,
            maxBytesPerStream = 200L,
            maxPartitionKeyLength = 256,
            dispatcher = Dispatchers.IO
        )

        // Each record: "a" (1 byte key) + 50 bytes data = 51 bytes per record
        // 200 / 51 = 3.9 → at most 3 records per stream fit under 200 bytes (3 × 51 = 153)
        // The 4th record would push running_size to 204, exceeding 200
        repeat(6) { i ->
            storage.addRecord(RecordInput("stream-A", "a", ByteArray(50) { i.toByte() })).getOrThrow()
        }
        repeat(6) { i ->
            storage.addRecord(RecordInput("stream-B", "b", ByteArray(50) { i.toByte() })).getOrThrow()
        }

        val recordsByStream = storage.getRecordsByStream().getOrThrow()
        recordsByStream.size shouldBe 2

        for (records in recordsByStream) {
            val streamName = records.first().streamName
            val totalSize = records.sumOf { it.dataSize }

            // Each stream should return at most 3 records (153 bytes ≤ 200)
            records.size shouldBe 3
            totalSize shouldBe 153

            // Verify all records belong to the same stream
            records.forEach { it.streamName shouldBe streamName }
        }
    }

    @Test
    fun `getRecordsByStream with empty afterIdByStream returns all records`() = runTest {
        val storage = createTestStorage()

        storage.addRecord(RecordInput("stream1", "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput("stream1", "key2", byteArrayOf(2))).getOrThrow()
        storage.addRecord(RecordInput("stream2", "key3", byteArrayOf(3))).getOrThrow()

        val result = storage.getRecordsByStream().getOrThrow()
        val allRecords = result.flatten()

        allRecords.size shouldBe 3
    }

    @Test
    fun `getRecordsByStream skips records at or before afterId per stream`() = runTest {
        val storage = createTestStorage()

        storage.addRecord(RecordInput("stream1", "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput("stream1", "key2", byteArrayOf(2))).getOrThrow()
        storage.addRecord(RecordInput("stream1", "key3", byteArrayOf(3))).getOrThrow()

        val allRecords = storage.getRecordsByStream().getOrThrow().flatten()
        allRecords.size shouldBe 3

        // Skip past the second record for stream1
        val afterId = mapOf("stream1" to allRecords[1].id)
        val filtered = storage.getRecordsByStream(afterId).getOrThrow().flatten()

        filtered.size shouldBe 1
        filtered[0].id shouldBe allRecords[2].id
    }

    @Test
    fun `getRecordsByStream with afterId past last record returns empty`() = runTest {
        val storage = createTestStorage()

        storage.addRecord(RecordInput("stream1", "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput("stream1", "key2", byteArrayOf(2))).getOrThrow()

        val allRecords = storage.getRecordsByStream().getOrThrow().flatten()
        val lastId = allRecords.maxOf { it.id }

        val result = storage.getRecordsByStream(mapOf("stream1" to lastId)).getOrThrow()
        result shouldBe emptyList()
    }

    @Test
    fun `getRecordsByStream applies afterId independently per stream`() = runTest {
        val storage = createTestStorage()

        storage.addRecord(RecordInput("stream1", "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput("stream1", "key2", byteArrayOf(2))).getOrThrow()
        storage.addRecord(RecordInput("stream2", "key3", byteArrayOf(3))).getOrThrow()
        storage.addRecord(RecordInput("stream2", "key4", byteArrayOf(4))).getOrThrow()

        val allRecords = storage.getRecordsByStream().getOrThrow().flatten()
        val stream1First = allRecords.first { it.streamName == "stream1" }
        val stream2First = allRecords.first { it.streamName == "stream2" }

        // Skip past the first record of each stream
        val afterIds = mapOf("stream1" to stream1First.id, "stream2" to stream2First.id)
        val filtered = storage.getRecordsByStream(afterIds).getOrThrow()
        val remaining = filtered.flatten()

        remaining.size shouldBe 2
        remaining.none { it.id == stream1First.id } shouldBe true
        remaining.none { it.id == stream2First.id } shouldBe true
    }

    @Test
    fun `getRecordsByStream respects batch limit with afterId pagination`() = runTest {
        val storage = SQLiteRecordStorage(
            maxRecordsByStream = 2,
            cacheMaxBytes = 1024 * 1024L,
            identifier = "test_batch_afterid",
            connectionFactory = { BundledSQLiteDriver().open(":memory:") },
            maxRecordSizeBytes = 10L * 1024 * 1024,
            maxBytesPerStream = 10L * 1024 * 1024,
            maxPartitionKeyLength = 256,
            dispatcher = Dispatchers.IO
        )

        // Add 4 records to one stream
        storage.addRecord(RecordInput("stream1", "key1", byteArrayOf(1))).getOrThrow()
        storage.addRecord(RecordInput("stream1", "key2", byteArrayOf(2))).getOrThrow()
        storage.addRecord(RecordInput("stream1", "key3", byteArrayOf(3))).getOrThrow()
        storage.addRecord(RecordInput("stream1", "key4", byteArrayOf(4))).getOrThrow()

        // First batch: 2 records (batch limit)
        val batch1 = storage.getRecordsByStream().getOrThrow().flatten()
        batch1.size shouldBe 2

        // Second batch: start after last id of batch1
        val lastId1 = batch1.maxOf { it.id }
        val batch2 = storage.getRecordsByStream(mapOf("stream1" to lastId1)).getOrThrow().flatten()
        batch2.size shouldBe 2
        batch2.all { it.id > lastId1 } shouldBe true

        // Third batch: start after last id of batch2 — nothing left
        val lastId2 = batch2.maxOf { it.id }
        val batch3 = storage.getRecordsByStream(mapOf("stream1" to lastId2)).getOrThrow()
        batch3 shouldBe emptyList()
    }

    @Test
    fun `getRecordsByStream handles large number of records efficiently with afterId`() = runTest {
        val storage = SQLiteRecordStorage(
            maxRecordsByStream = 100,
            cacheMaxBytes = 50 * 1024 * 1024L,
            identifier = "test_stress",
            connectionFactory = { BundledSQLiteDriver().open(":memory:") },
            maxRecordSizeBytes = 10L * 1024 * 1024,
            maxBytesPerStream = 10L * 1024 * 1024,
            maxPartitionKeyLength = 256,
            dispatcher = Dispatchers.IO
        )

        val totalRecords = 5000
        val streams = listOf("stream-A", "stream-B", "stream-C")

        // Insert many records across multiple streams
        repeat(totalRecords) { i ->
            val stream = streams[i % streams.size]
            storage.addRecord(RecordInput(stream, "key$i", ByteArray(32) { i.toByte() })).getOrThrow()
        }

        // Drain all records using afterId pagination (simulating flush loop)
        val lastIdByStream = mutableMapOf<String, Long>()
        var totalDrained = 0

        var batches = storage.getRecordsByStream(lastIdByStream).getOrThrow()
        while (batches.isNotEmpty()) {
            for (records in batches) {
                val streamName = records.first().streamName
                val maxId = records.maxOf { it.id }
                lastIdByStream[streamName] = maxId
                totalDrained += records.size
            }
            batches = storage.getRecordsByStream(lastIdByStream).getOrThrow()
        }

        totalDrained shouldBe totalRecords
    }
}
