package com.amplifyframework.logging.cloudwatch

import android.content.Context
import android.util.Log
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import aws.sdk.kotlin.services.cloudwatchlogs.model.DescribeLogStreamsRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.InputLogEvent
import aws.sdk.kotlin.services.cloudwatchlogs.model.PutLogEventsRequest
import com.amplifyframework.logging.cloudwatch.db.CloudWatchLoggingDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
class CloudwatchLogEventRecorder(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private var stopSync = false
    private var previousSequence: String? = null

    var awsCloudWatchLogsClient: CloudWatchLogsClient? = null

    private val cloudWatchLoggingDatabase = CloudWatchLoggingDatabase(context)

    suspend fun saveLogEvent(event: CloudWatchLogEvent) {
        if (!stopSync) {
            try {
                cloudWatchLoggingDatabase.saveLogEvent(event)
            } catch (e: Exception) {
                Log.e(
                    "CloudwatchLogEventRecorder",
                    "failed to save event, error $e, stacktrace:  ${Log.getStackTraceString(e)}",
                ) // ktlint-disable max-line-length
            }
        }
    }

    suspend fun startSync() {
        withContext(dispatcher) {
            while (!stopSync) {
                try {
                    syncLogEventsWithCloudwatch()
                } catch (e: Exception) {
                    Log.e("CloudwatchLogEventRecorder", "error $e, stacktrace:  ${Log.getStackTraceString(e)}")
                } finally {
                    delay(15 * 1000)
                }
            }
        }
    }

    private suspend fun syncLogEventsWithCloudwatch() {
        withContext(dispatcher) {
            awsCloudWatchLogsClient?.let { client ->
                val queriedEvents = cloudWatchLoggingDatabase.queryAllEvents()
                Log.i("CloudwatchLogEventRecorder", "Queried ${queriedEvents.size} events")
                val groupName = "central-logging-poc/test"
                val streamName = "2022/11/23/test"
                val inputLogEvents = mutableListOf<InputLogEvent>()
                val inputLogEventsId = mutableListOf<Int>()
                queriedEvents.forEach { event ->
                    inputLogEvents.add(
                        InputLogEvent {
                            timestamp = event.timestamp
                            message = event.message
                        },
                    )
                    inputLogEventsId.add(event.id)
                }
                inputLogEvents.sortBy { it.timestamp }
                if (inputLogEvents.size < 1) {
                    return@withContext
                }
                val response = client.describeLogStreams(
                    DescribeLogStreamsRequest {
                        logGroupName = groupName
                        logStreamNamePrefix = streamName
                    },
                )
                val nexttoken = response.logStreams?.get(0)?.uploadSequenceToken
                val putLogEventResponse = client.putLogEvents(
                    PutLogEventsRequest {
                        logEvents = inputLogEvents
                        logGroupName = groupName
                        logStreamName = streamName
                        sequenceToken = nexttoken
                    },
                )
                cloudWatchLoggingDatabase.bulkDelete(inputLogEventsId)
            }
        }
    }
}
