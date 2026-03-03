package com.amplifyframework.recordcache

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.testutils.assertions.shouldBeFailure
import com.amplifyframework.testutils.assertions.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for PutRecords record-level validation.
 *
 * Uses a small maxRecordSizeBytes (1000 bytes) to keep allocations tiny while
 * exercising the same boundary logic that applies to the real 10 MiB limit.
 *
 * Per the Kinesis PutRecords API spec:
 * - Each record's total size (partition key bytes + data blob bytes) must not exceed 10 MiB
 * - Partition key: 1–256 Unicode code points
 * - dataSize should account for both partition key bytes (UTF-8 encoded) and data blob bytes
 *
 * See: https://docs.aws.amazon.com/kinesis/latest/APIReference/API_PutRecordsRequestEntry.html
 */
@RunWith(RobolectricTestRunner::class)
class RecordValidationTest {

    companion object {
        private const val MAX_RECORD_SIZE = 1000L
    }

    private lateinit var storage: SQLiteRecordStorage

    @Before
    fun setup() {
        storage = SQLiteRecordStorage(
            maxRecordsByStream = 500,
            cacheMaxBytes = 10_000L,
            identifier = "test_validation",
            connectionFactory = { BundledSQLiteDriver().open(":memory:") },
            maxRecordSizeBytes = MAX_RECORD_SIZE,
            maxBytesPerStream = 10_000L,
            maxPartitionKeyLength = 256,
            dispatcher = Dispatchers.IO
        )
    }

    // ---------------------------------------------------------------
    // Per-record size limit (partition key + data blob)
    // ---------------------------------------------------------------

    @Test
    fun `record exactly at max size is accepted`() = runTest {
        // "k" = 1 byte, data = 999 bytes → total 1000 = MAX_RECORD_SIZE
        val result = storage.addRecord(
            RecordInput("stream", "k", ByteArray(999) { 0x41 })
        )
        result.shouldBeSuccess()
    }

    @Test
    fun `record exceeding max size by one byte is rejected`() = runTest {
        // "k" = 1 byte, data = 1000 bytes → total 1001 > MAX_RECORD_SIZE
        val result = storage.addRecord(
            RecordInput("stream", "k", ByteArray(1000) { 0x41 })
        )
        result.shouldBeFailure().error
            .shouldBeInstanceOf<RecordCacheValidationException>()
    }

    // ---------------------------------------------------------------
    // dataSize includes partition key
    // ---------------------------------------------------------------

    @Test
    fun `dataSize accounts for partition key bytes`() = runTest {
        val partitionKey = "k".repeat(10) // 10 bytes UTF-8
        val data = ByteArray(50) { 0x41 }

        storage.addRecord(RecordInput("stream", partitionKey, data)).shouldBeSuccess()

        val cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 60 // 50 + 10
    }

    @Test
    fun `dataSize with multi-byte unicode partition key`() = runTest {
        // Each emoji is 4 bytes in UTF-8, 2 emojis = 8 bytes
        val partitionKey = "\uD83D\uDE00".repeat(2)
        val data = ByteArray(10) { 0x41 }

        storage.addRecord(RecordInput("stream", partitionKey, data)).shouldBeSuccess()

        val cachedSize = storage.getCurrentCacheSize().getOrThrow()
        cachedSize shouldBe 18 // 10 + 8
    }

    // ---------------------------------------------------------------
    // Cache size limit respects full record size
    // ---------------------------------------------------------------

    @Test
    fun `cache limit accounts for partition key in cumulative size`() = runTest {
        val tightStorage = SQLiteRecordStorage(
            maxRecordsByStream = 500,
            cacheMaxBytes = 80L,
            identifier = "test_tight",
            connectionFactory = { BundledSQLiteDriver().open(":memory:") },
            maxRecordSizeBytes = MAX_RECORD_SIZE,
            maxBytesPerStream = 10_000L,
            maxPartitionKeyLength = 256,
            dispatcher = Dispatchers.IO
        )

        // "k" * 10 (10 bytes) + data 30 bytes = 40 bytes per record
        val partitionKey = "k".repeat(10)
        val data = ByteArray(30) { 0x41 }

        // First record: 40 bytes — fits in 80-byte cache
        tightStorage.addRecord(RecordInput("stream", partitionKey, data))
            .shouldBeSuccess()

        // Second record: 40 more → total 80 — still fits
        tightStorage.addRecord(RecordInput("stream", partitionKey, data))
            .shouldBeSuccess()

        // Third record: 40 more → total 120 > 80 limit
        tightStorage.addRecord(RecordInput("stream", partitionKey, data))
            .shouldBeFailure().error
            .shouldBeInstanceOf<RecordCacheLimitExceededException>()
    }

    // ---------------------------------------------------------------
    // Partition key validation (1–256 Unicode code points)
    // ---------------------------------------------------------------

    @Test
    fun `empty partition key is rejected`() = runTest {
        val result = storage.addRecord(
            RecordInput("stream", "", byteArrayOf(1, 2, 3))
        )
        result.shouldBeFailure().error
            .shouldBeInstanceOf<RecordCacheValidationException>()
    }

    @Test
    fun `partition key at max length 256 code points is accepted`() = runTest {
        val result = storage.addRecord(
            RecordInput("stream", "k".repeat(256), byteArrayOf(1))
        )
        result.shouldBeSuccess()
    }

    @Test
    fun `partition key exceeding 256 code points is rejected`() = runTest {
        val result = storage.addRecord(
            RecordInput("stream", "k".repeat(257), byteArrayOf(1))
        )
        result.shouldBeFailure().error
            .shouldBeInstanceOf<RecordCacheValidationException>()
    }

    @Test
    fun `partition key with emoji counts code points correctly`() = runTest {
        // Each emoji (U+1F600) is 1 code point but 2 UTF-16 chars (surrogate pair) and 4 UTF-8 bytes
        // 10 emoji = 10 code points (within 256 limit), 20 chars, 40 bytes
        val partitionKey = "\uD83D\uDE00".repeat(10)
        val result = storage.addRecord(
            RecordInput("stream", partitionKey, byteArrayOf(1))
        )
        result.shouldBeSuccess()
    }

    @Test
    fun `partition key exceeding 256 code points with emoji is rejected`() = runTest {
        // Each emoji (U+1F600) is 1 code point
        // 257 emoji = 257 code points > 256 limit
        val partitionKey = "\uD83D\uDE00".repeat(257)
        val result = storage.addRecord(
            RecordInput("stream", partitionKey, byteArrayOf(1))
        )
        result.shouldBeFailure().error
            .shouldBeInstanceOf<RecordCacheValidationException>()
    }

    // ---------------------------------------------------------------
    // Recovery after rejection
    // ---------------------------------------------------------------

    @Test
    fun `storage accepts valid records after rejecting oversized one`() = runTest {
        // 20 bytes key + 990 bytes data = 1010 > 1000 limit
        val oversizedResult = storage.addRecord(
            RecordInput("stream", "k".repeat(20), ByteArray(990) { 0x42 })
        )
        oversizedResult.shouldBeFailure()

        // Valid record should still work
        val validResult = storage.addRecord(
            RecordInput("stream", "a", byteArrayOf(1, 2, 3))
        )
        validResult.shouldBeSuccess()

        val cachedSize = storage.getCurrentCacheSize().getOrThrow()
        // "a" (1) + data (3) = 4
        cachedSize shouldBe 4
    }
}
