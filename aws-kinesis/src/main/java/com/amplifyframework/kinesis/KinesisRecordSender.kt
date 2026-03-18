/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amplifyframework.kinesis

import androidx.annotation.VisibleForTesting
import aws.sdk.kotlin.services.kinesis.KinesisClient
import aws.sdk.kotlin.services.kinesis.model.PutRecordsRequest
import aws.sdk.kotlin.services.kinesis.model.PutRecordsRequestEntry
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.resultCatching
import com.amplifyframework.recordcache.PutRecordsResponse
import com.amplifyframework.recordcache.Record
import com.amplifyframework.recordcache.RecordSender

typealias PutRecordsResponseSdk = aws.sdk.kotlin.services.kinesis.model.PutRecordsResponse

@OptIn(InternalAmplifyApi::class)
internal class KinesisRecordSender(
    private val kinesisClient: KinesisClient,
    private val maxRetries: Int
) : RecordSender {

    override suspend fun putRecords(streamName: String, records: List<Record>): Result<PutRecordsResponse, Throwable> =
        resultCatching {
            val request = createRequest(streamName, records)
            val sdkResponse = kinesisClient.putRecords(request)
            splitResponse(sdkResponse, records)
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
