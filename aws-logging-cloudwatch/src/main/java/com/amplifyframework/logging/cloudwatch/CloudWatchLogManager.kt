/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.logging.cloudwatch

import android.content.Context
import android.util.Log
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import aws.sdk.kotlin.services.cloudwatchlogs.model.CreateLogStreamRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.DescribeLogStreamsRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.InputLogEvent
import aws.sdk.kotlin.services.cloudwatchlogs.model.PutLogEventsRequest
import com.amplifyframework.logging.cloudwatch.db.CloudWatchLoggingDatabase
import com.amplifyframework.logging.cloudwatch.models.AWSCloudWatchLoggingPluginConfiguration
import com.amplifyframework.logging.cloudwatch.models.CloudWatchLogEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class CloudWatchLogManager(
    private val context: Context,
    private val pluginConfiguration: AWSCloudWatchLoggingPluginConfiguration,
    private val awsCloudWatchLogsClient: CloudWatchLogsClient,
    private val loggingConstraintsResolver: LoggingConstraintsResolver,
    private val cloudWatchLoggingDatabase: CloudWatchLoggingDatabase = CloudWatchLoggingDatabase(context),
    private val customCognitoCredentialsProvider: CustomCognitoCredentialsProvider = CustomCognitoCredentialsProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val deviceIdKey = "unique_device_id"
    private var stopSync = false
    private var userIdentityId: String? = null
        set(value) {
            field = value
            loggingConstraintsResolver.userId = value
        }
    private val todayDate: String = SimpleDateFormat("MM-dd-yyyy", Locale.US).format(Date())
    private val coroutineScope = CoroutineScope(dispatcher)
    private var isSyncInProgress = AtomicBoolean(false)

    init {
        onSignIn()
    }

    suspend fun saveLogEvent(event: CloudWatchLogEvent) {
        withContext(dispatcher) {
            try {
                cloudWatchLoggingDatabase.saveLogEvent(event)
                if (isCacheFull()) {
                    syncLogEventsWithCloudwatch()
                } else {
                }
            } catch (e: Exception) {
                Log.e(
                    "CloudwatchLogEventRecorder",
                    "failed to save event",
                    e,
                )
            }
        }
    }

    suspend fun startSync() {
        stopSync = false
        withContext(dispatcher) {
            while (!stopSync) {
                try {
                    syncLogEventsWithCloudwatch()
                } catch (e: Exception) {
                    Log.e("CloudwatchLogEventRecorder", "error $e, stacktrace:  ${Log.getStackTraceString(e)}")
                } finally {
                    Log.d("Behavior", "waiting sync")
                    delay(pluginConfiguration.flushIntervalInSeconds * 1000L)
                }
            }
        }
    }

    internal fun stopSync() {
        stopSync = true
        clearCache()
        Log.d("Behavior", "stopping sync")
    }

    internal suspend fun syncLogEventsWithCloudwatch() {
        if (isSyncInProgress.get()) {
            return
        }
        withContext(dispatcher) {
            var inputLogEventsIdToBeDeleted: List<Int> = emptyList()
            try {
                isSyncInProgress.set(true)
                awsCloudWatchLogsClient.let { client ->
                    while (true) {
                        val queriedEvents = cloudWatchLoggingDatabase.queryAllEvents().toMutableList()
                        if (queriedEvents.isEmpty()) break
                        Log.i("CloudwatchLogEventRecorder", "Queried ${queriedEvents.size} events")
                        while (queriedEvents.isNotEmpty()) {
                            val groupName = pluginConfiguration.logGroupName
                            val streamName = "$todayDate.${uniqueDeviceId()}.${userIdentityId ?: "guest"}"
                            val nextBatch = getNextBatch(queriedEvents)
                            val inputLogEvents = nextBatch.first
                            inputLogEventsIdToBeDeleted = nextBatch.second
                            if (inputLogEvents.isEmpty()) {
                                return@withContext
                            }
                            createLogStreamIfNotCreated(streamName, groupName, client)
                            try {
                                client.putLogEvents(
                                    PutLogEventsRequest {
                                        logEvents = inputLogEvents
                                        logGroupName = groupName
                                        logStreamName = streamName
                                    },
                                ).also { response ->
                                    response.rejectedLogEventsInfo?.tooNewLogEventStartIndex?.let {
                                        inputLogEventsIdToBeDeleted = inputLogEventsIdToBeDeleted.slice(
                                            IntRange(0, it - 1),
                                        ).toMutableList()
                                    }
                                    cloudWatchLoggingDatabase.bulkDelete(inputLogEventsIdToBeDeleted)
                                }
                            } catch (networkException: Exception) {
                                Log.e("TAG", networkException.toString())
                            }
                        }
                    }
                }
            } catch (exception: Exception) {
                Log.e("CloudwatchLogEventRecorder", "stacktrace:  ${Log.getStackTraceString(exception)}")
                if (isCacheFull()) {
                    cloudWatchLoggingDatabase.bulkDelete(inputLogEventsIdToBeDeleted)
                }
            } finally {
                isSyncInProgress.set(false)
            }
        }
    }

    private suspend fun createLogStreamIfNotCreated(
        logStream: String,
        groupName: String,
        client: CloudWatchLogsClient,
    ) {
        client.describeLogStreams(
            DescribeLogStreamsRequest {
                logGroupName = groupName
                logStreamNamePrefix = logStream
            },
        ).apply {
            if (this.logStreams == null || this.logStreams?.isEmpty() == true) {
                client.createLogStream(
                    CreateLogStreamRequest {
                        logGroupName = groupName
                        logStreamName = logStream
                    },
                )
            }
        }
    }

    private suspend fun getNextBatch(queriedEvents: MutableList<CloudWatchLogEvent>):
        Pair<List<InputLogEvent>, List<Int>> {
        var totalBatchSize = 0L
        val inputLogEvents = mutableListOf<InputLogEvent>()
        val inputLogEventsIdToBeDeleted = mutableListOf<Int>()
        val firstEvent = queriedEvents[0]
        val iterator = queriedEvents.iterator()
        while (iterator.hasNext()) {
            val cloudWatchEvent = iterator.next()
            totalBatchSize = totalBatchSize.plus(cloudWatchEvent.message.length).plus(26)
            if (
                inputLogEvents.size >= 10000 || // The maximum number of log events in a batch is 10,000.
                totalBatchSize >= 1048576 || // The maximum batch size is 1,048,576 bytes.
                cloudWatchEvent.timestamp - firstEvent.timestamp >= 24 * 60 * 60L // A batch of log events in a single request cannot span more than 24 hours. Otherwise, the operation fails.
            ) {
                break
            }
            inputLogEvents.add(
                InputLogEvent {
                    timestamp = cloudWatchEvent.timestamp
                    message = cloudWatchEvent.message
                },
            )
            inputLogEventsIdToBeDeleted.add(cloudWatchEvent.id)
            iterator.remove()
        }
        return Pair(inputLogEvents, inputLogEventsIdToBeDeleted)
    }

    private fun clearCache() {
        coroutineScope.launch {
            cloudWatchLoggingDatabase.clearDatabase()
            context.getSharedPreferences(
                AWSCloudWatchLoggingPlugin.SHARED_PREFERENCE_FILENAME,
                Context.MODE_PRIVATE,
            ).edit().remove(LoggingConstraintsResolver.REMOTE_LOGGING_CONSTRAINTS_KEY).apply()
        }
    }

    internal fun onSignIn() {
        coroutineScope.launch {
            syncLogEventsWithCloudwatch()
            userIdentityId = try {
                val authUser = customCognitoCredentialsProvider.getCurrentUser()
                authUser.userId
            } catch (exception: Exception) {
                null
            }
        }
    }

    internal fun onSignOut() {
        userIdentityId = null
        clearCache()
    }

    private fun uniqueDeviceId(): String {
        val sharedPreferences =
            context.getSharedPreferences(AWSCloudWatchLoggingPlugin.SHARED_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(deviceIdKey, null) ?: UUID.randomUUID().toString().also { id ->
            sharedPreferences.edit().putString(deviceIdKey, id).apply()
        }
    }

    private fun isCacheFull() = cloudWatchLoggingDatabase.isCacheFull(pluginConfiguration.localStoreMaxSizeInMB)
}
