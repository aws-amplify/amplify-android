package com.amplifyframework.kinesis

import aws.sdk.kotlin.services.kinesis.KinesisClient
import aws.sdk.kotlin.services.kinesis.model.PutRecordsResultEntry
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

    @Test
    fun `splitResponse should correctly categorize records`() = runTest {
        // Given
        val recordSender = KinesisRecordSender(mockClient, maxRetries)

        val records = listOf(
            createTestRecord(1L, "key1", byteArrayOf(1), retryCount = 0), // Success
            createTestRecord(2L, "key2", byteArrayOf(2), retryCount = 1), // Retryable
            createTestRecord(3L, "key3", byteArrayOf(3), retryCount = maxRetries) // Failed
        )

        val sdkResponse = PutRecordsResponseSdk {
            this.records = listOf(
                PutRecordsResultEntry {
                    errorCode = null
                    sequenceNumber = "seq1"
                },
                PutRecordsResultEntry { errorCode = "ProvisionedThroughputExceededException" },
                PutRecordsResultEntry { errorCode = "InternalFailure" }
            )
        }

        // When
        val response = recordSender.splitResponse(sdkResponse, records)

        // Then
        response.successfulIds shouldBe listOf(1L)
        response.retryableIds shouldBe listOf(2L)
        response.failedIds shouldBe listOf(3L)
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
