package com.amplifyframework.kinesis

import androidx.annotation.VisibleForTesting
import aws.sdk.kotlin.services.kinesis.KinesisClient
import aws.sdk.kotlin.services.kinesis.model.PutRecordsRequest
import aws.sdk.kotlin.services.kinesis.model.PutRecordsRequestEntry
import com.amplifyframework.recordcache.PutRecordsResponse
import com.amplifyframework.recordcache.Record
import com.amplifyframework.recordcache.RecordCacheNetworkException
import com.amplifyframework.recordcache.RecordSender

typealias PutRecordsResponseSdk = aws.sdk.kotlin.services.kinesis.model.PutRecordsResponse

internal class KinesisRecordSender(
    private val kinesisClient: KinesisClient,
    private val maxRetries: Int,
) : RecordSender {

    override suspend fun putRecords(streamName: String, records: List<Record>): Result<PutRecordsResponse> =
        runCatching {
            // Create request
            val request = createRequest(streamName, records)

            val sdkResponse = kinesisClient.putRecords(request)

            val response = splitResponse(sdkResponse, records)
            return@runCatching response
        }.recoverCatching { cause ->
            throw RecordCacheNetworkException(
                "Failed to send records to Kinesis",
                "Check network connectivity and AWS credentials",
                cause
            )
        }

    @VisibleForTesting
    internal fun createRequest(streamName: String, records: List<Record>) = PutRecordsRequest {
        this.streamName = streamName
        this.records = records.map { record ->
            PutRecordsRequestEntry {
                this.data = record.data
                this.partitionKey = record.partitionKey
            }
        }
    }

    @VisibleForTesting
    internal fun splitResponse(response: PutRecordsResponseSdk, records: List<Record>): PutRecordsResponse {
        val successfulIds = mutableListOf<Long>()
        val retryableIds = mutableListOf<Long>()
        val failedIds = mutableListOf<Long>()
        response.records.forEachIndexed { index, result ->
            val recordId = records[index].id
            val retryCount = records[index].retryCount
            if (result.errorCode == null) {
                successfulIds.add(recordId)
            } else if (retryCount >= maxRetries) {
                failedIds.add(recordId)
            } else {
                // Error codes can be: ProvisionedThroughputExceededException or InternalFailure
                retryableIds.add(recordId)
            }
        }

        return PutRecordsResponse(successfulIds, retryableIds, failedIds)
    }
}
