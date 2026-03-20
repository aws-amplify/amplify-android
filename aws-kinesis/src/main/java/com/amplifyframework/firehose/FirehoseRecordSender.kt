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
package com.amplifyframework.firehose

import androidx.annotation.VisibleForTesting
import aws.sdk.kotlin.services.firehose.FirehoseClient
import aws.sdk.kotlin.services.firehose.model.FirehoseException as SdkFirehoseException
import aws.sdk.kotlin.services.firehose.model.PutRecordBatchRequest
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.recordcache.PutRecordsResponse
import com.amplifyframework.recordcache.Record
import com.amplifyframework.recordcache.RecordSender
import com.amplifyframework.recordcache.resultCatchingSkippable
import com.amplifyframework.recordcache.splitResults

typealias FirehoseRecord = aws.sdk.kotlin.services.firehose.model.Record

@OptIn(InternalAmplifyApi::class)
internal class FirehoseRecordSender(
    private val firehoseClient: FirehoseClient,
    private val maxRetries: Int
) : RecordSender {

    override suspend fun putRecords(streamName: String, records: List<Record>): Result<PutRecordsResponse, Throwable> =
        resultCatchingSkippable<SdkFirehoseException> {
            val request = createRequest(streamName, records)
            val sdkResponse = firehoseClient.putRecordBatch(request)
            splitResults(
                errorCodes = sdkResponse.requestResponses.map { it.errorCode },
                records = records,
                maxRetries = maxRetries
            )
        }

    @VisibleForTesting
    internal fun createRequest(streamName: String, records: List<Record>) = PutRecordBatchRequest {
        this.deliveryStreamName = streamName
        this.records = records.map { record ->
            FirehoseRecord { this.data = record.data }
        }
    }
}
