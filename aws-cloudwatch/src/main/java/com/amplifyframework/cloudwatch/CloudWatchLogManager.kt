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
@file:OptIn(InternalAmplifyApi::class)

package com.amplifyframework.cloudwatch

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import aws.sdk.kotlin.services.cloudwatchlogs.model.CreateLogStreamRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.DescribeLogStreamsRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.InputLogEvent
import aws.sdk.kotlin.services.cloudwatchlogs.model.PutLogEventsRequest
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.cloudwatch.common.db.CloudWatchLoggingDatabase
import com.amplifyframework.cloudwatch.common.db.LogEvent
import com.amplifyframework.cloudwatch.common.models.CloudWatchLogEvent
import com.amplifyframework.cloudwatch.worker.CloudWatchLogsSyncWorker
import com.amplifyframework.cloudwatch.worker.CloudWatchRouterWorker
import com.amplifyframework.cloudwatch.worker.CloudWatchWorkerFactory
import com.amplifyframework.foundation.logging.AmplifyLogging
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Engine for buffering and delivering CloudWatch log events for [AmplifyCloudWatchClient]. Ported
 * from the v2 `AWSCloudWatchLoggingPlugin`, decoupled from `:core`: failures are reported through
 * the [onWriteLogFailure] / [onFlushLogFailure] callbacks (instead of Hub), user identity is set
 * explicitly via [setUserIdentifier] (instead of resolved from Auth), and diagnostics use the
 * foundation logger. Log persistence is delegated to the shared `:aws-cloudwatch-common` store.
 *
 * @param flushIntervalInSeconds interval between automatic flushes; `null` disables automatic
 *   flushing (used for `FlushStrategy.None`).
 */
internal class CloudWatchLogManager(
    private val context: Context,
    private val logGroupName: String,
    private val localStoreMaxSizeInMB: Int,
    private val flushIntervalInSeconds: Long?,
    private val awsCloudWatchLogsClient: CloudWatchLogsClient,
    databaseName: String,
    passphrasePreferencesName: String,
    private val onWriteLogFailure: (context: String?, cause: Throwable?) -> Unit,
    private val onFlushLogFailure: (context: String?, cause: Throwable?) -> Unit,
    private val cloudWatchLoggingDatabase: CloudWatchLoggingDatabase = CloudWatchLoggingDatabase(
        context,
        databaseName = databaseName,
        passphrasePreferencesName = passphrasePreferencesName
    ),
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val deviceIdKey = "unique_device_id"
    private var stopSync = false

    @Volatile
    private var userIdentityId: String? = null
    private val todayDate: String = SimpleDateFormat("MM-dd-yyyy", Locale.US).format(Date())
    private val coroutineScope = CoroutineScope(coroutineDispatcher)
    private var isSyncInProgress = AtomicBoolean(false)
    private val logger = AmplifyLogging.logger(CloudWatchLogManager::class.java.simpleName)

    init {
        // Register the delegate factory so the WorkManager-instantiated CloudWatchRouterWorker can
        // create a CloudWatchLogsSyncWorker wired to this manager.
        CloudWatchRouterWorker.workerFactories[CloudWatchRouterWorker.WORKER_FACTORY_KEY] =
            CloudWatchWorkerFactory(this)
    }

    suspend fun saveLogEvent(event: CloudWatchLogEvent) = withContext(coroutineDispatcher) {
        try {
            cloudWatchLoggingDatabase.saveLogEvent(event)
            if (isCacheFull()) {
                syncLogEventsWithCloudwatch()
            }
        } catch (e: Exception) {
            logger.error("failed to save event", e)
            onWriteLogFailure(event.message, e)
        }
    }

    /**
     * Update the current user identifier. Pending events are flushed to the previous user's stream
     * before the identifier changes, mirroring the v2 plugin's sign-in behavior.
     */
    fun setUserIdentifier(identifier: String?) {
        coroutineScope.launch {
            syncLogEventsWithCloudwatch()
            userIdentityId = identifier
        }
    }

    suspend fun startSync() {
        stopSync = false
        enqueueSync()
    }

    fun stopSync() {
        stopSync = true
        cancelSync()
        clearCache()
        logger.debug("stopping sync")
    }

    suspend fun syncLogEventsWithCloudwatch() {
        if (isSyncInProgress.get()) {
            return
        }
        withContext(coroutineDispatcher) {
            var inputLogEventsIdToBeDeleted: List<Long> = emptyList()
            try {
                isSyncInProgress.set(true)
                awsCloudWatchLogsClient.let { client ->
                    while (true) {
                        val queriedEvents = cloudWatchLoggingDatabase.queryAllEvents().toMutableList()
                        if (queriedEvents.isEmpty()) break
                        while (queriedEvents.isNotEmpty()) {
                            val groupName = logGroupName
                            val deviceId = uniqueDeviceId()

                            // Default format: MM-dd-yyyy.deviceId.userId
                            val streamName = "$todayDate.$deviceId.${userIdentityId ?: "guest"}"

                            val nextBatch = getNextBatch(queriedEvents)
                            val inputLogEvents = nextBatch.first
                            inputLogEventsIdToBeDeleted = nextBatch.second
                            if (inputLogEvents.isEmpty()) {
                                return@withContext
                            }
                            createLogStreamIfNotCreated(streamName, groupName, client)
                            client.putLogEvents(
                                PutLogEventsRequest {
                                    logEvents = inputLogEvents
                                    logGroupName = groupName
                                    logStreamName = streamName
                                }
                            ).also { response ->
                                response.rejectedLogEventsInfo?.tooNewLogEventStartIndex?.let {
                                    inputLogEventsIdToBeDeleted = inputLogEventsIdToBeDeleted.slice(
                                        IntRange(0, it - 1)
                                    ).toMutableList()
                                }
                                cloudWatchLoggingDatabase.bulkDelete(inputLogEventsIdToBeDeleted)
                            }
                        }
                    }
                }
            } catch (exception: Exception) {
                onFlushLogFailure(null, exception)
                if (isCacheFull()) {
                    cloudWatchLoggingDatabase.bulkDelete(inputLogEventsIdToBeDeleted)
                }
                throw exception
            } finally {
                isSyncInProgress.set(false)
            }
        }
    }

    private suspend fun createLogStreamIfNotCreated(
        logStream: String,
        groupName: String,
        client: CloudWatchLogsClient
    ) {
        client.describeLogStreams(
            DescribeLogStreamsRequest {
                logGroupName = groupName
                logStreamNamePrefix = logStream
            }
        ).apply {
            if (this.logStreams == null || this.logStreams?.isEmpty() == true) {
                client.createLogStream(
                    CreateLogStreamRequest {
                        logGroupName = groupName
                        logStreamName = logStream
                    }
                )
            }
        }
    }

    private fun getNextBatch(queriedEvents: MutableList<LogEvent>): Pair<List<InputLogEvent>, List<Long>> {
        var totalBatchSize = 0L
        val inputLogEvents = mutableListOf<InputLogEvent>()
        val inputLogEventsIdToBeDeleted = mutableListOf<Long>()
        val firstEvent = queriedEvents[0]
        val iterator = queriedEvents.iterator()
        while (iterator.hasNext()) {
            val cloudWatchEvent = iterator.next()
            totalBatchSize = totalBatchSize.plus(cloudWatchEvent.message.length).plus(26)
            if (
                // The maximum number of log events in a batch is 10,000.
                inputLogEvents.size >= 10000 ||
                // The maximum batch size is 1,048,576 bytes.
                totalBatchSize >= 1048576 ||
                // A batch of log events in a single request cannot span more than 24 hours.
                // Otherwise, the operation fails.
                cloudWatchEvent.timestamp - firstEvent.timestamp >= 24 * 60 * 60L
            ) {
                break
            }
            inputLogEvents.add(
                InputLogEvent {
                    timestamp = cloudWatchEvent.timestamp
                    message = cloudWatchEvent.message
                }
            )
            inputLogEventsIdToBeDeleted.add(cloudWatchEvent.id)
            iterator.remove()
        }
        return Pair(inputLogEvents, inputLogEventsIdToBeDeleted)
    }

    private fun clearCache() {
        coroutineScope.launch {
            cloudWatchLoggingDatabase.clearDatabase()
        }
    }

    internal fun enqueueSync() {
        val interval = flushIntervalInSeconds ?: return
        if (!stopSync) {
            val syncRequest = OneTimeWorkRequest.Builder(CloudWatchRouterWorker::class.java)
                .setInitialDelay(interval, TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(
                    workDataOf(
                        CloudWatchRouterWorker.WORKER_CLASS_NAME to CloudWatchLogsSyncWorker::class.java.simpleName,
                        CloudWatchRouterWorker.WORKER_ID to CloudWatchRouterWorker.WORKER_FACTORY_KEY
                    )
                )
                .addTag(CloudWatchLogsSyncWorker.WORKER_NAME_TAG)
                .build()
            WorkManager.getInstance(context).beginUniqueWork(
                CloudWatchLogsSyncWorker.WORKER_NAME_TAG,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            ).enqueue()
        }
    }

    private fun cancelSync() {
        WorkManager.getInstance(context).cancelUniqueWork(CloudWatchLogsSyncWorker.WORKER_NAME_TAG)
    }

    private fun uniqueDeviceId(): String {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(deviceIdKey, null) ?: UUID.randomUUID().toString().also { id ->
            sharedPreferences.edit().putString(deviceIdKey, id).apply()
        }
    }

    private fun isCacheFull() = cloudWatchLoggingDatabase.isCacheFull(localStoreMaxSizeInMB)

    internal companion object {
        // Reused from the v2 plugin so the persisted device id carries across a v2 -> v3 migration.
        internal const val SHARED_PREFERENCE_FILENAME = "com.amplify.logging.a3fa4188-0ac5-11ee-be56-0242ac120002"
    }
}
