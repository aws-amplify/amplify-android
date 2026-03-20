package com.amplifyframework.kinesis

import androidx.annotation.VisibleForTesting
import aws.sdk.kotlin.services.kinesis.KinesisClient
import aws.sdk.kotlin.services.kinesis.model.KinesisException as SdkKinesisException
import aws.sdk.kotlin.services.kinesis.model.PutRecordsRequest
import aws.sdk.kotlin.services.kinesis.model.PutRecordsRequestEntry
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.recordcache.PutRecordsResponse
import com.amplifyframework.recordcache.Record
import com.amplifyframework.recordcache.RecordSender
import com.amplifyframework.recordcache.resultCatchingSkippable
import com.amplifyframework.recordcache.splitResults

@OptIn(InternalAmplifyApi::class)
internal class KinesisRecordSender(
    private val kinesisClient: KinesisClient,
    private val maxRetries: Int
) : RecordSender {

    override suspend fun putRecords(streamName: String, records: List<Record>): Result<PutRecordsResponse, Throwable> =
        resultCatchingSkippable<SdkKinesisException> {
            val request = createRequest(streamName, records)
            val sdkResponse = kinesisClient.putRecords(request)
            splitResults(
                errorCodes = sdkResponse.records.map { it.errorCode },
                records = records,
                maxRetries = maxRetries
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
}
