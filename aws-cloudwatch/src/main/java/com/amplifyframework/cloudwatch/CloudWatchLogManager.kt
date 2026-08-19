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
import com.amplifyframework.cloudwatch.db.CloudWatchDatabase
import com.amplifyframework.cloudwatch.db.LogEvent
import com.amplifyframework.cloudwatch.models.CloudWatchLogEvent
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
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// AWS PutLogEvents limits (https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutLogEvents.html)
private const val MAX_LOG_EVENTS_PER_BATCH = 10_000
private const val MAX_BATCH_SIZE_BYTES = 1_048_576L
private const val MAX_EVENT_SIZE_BYTES = 262_144L
private const val PER_EVENT_OVERHEAD_BYTES = 26
private val MAX_BATCH_SPAN = 24.hours
private const val STREAM_DATE_FORMAT = "MM-dd-yyyy"

/**
 * Engine for buffering and delivering CloudWatch log events for [AmplifyCloudWatchClient]. Failures
 * are reported through the [onWriteLogFailure] / [onFlushLogFailure] callbacks, user identity is set
 * explicitly via [setUserIdentifier], and log persistence is delegated to [CloudWatchDatabase].
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
    private val cloudWatchLoggingDatabase: CloudWatchDatabase = CloudWatchDatabase(
        context,
        databaseName = databaseName,
        passphrasePreferencesName = passphrasePreferencesName
    ),
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    @Volatile
    private var stopSync = false

    @Volatile
    private var userIdentityId: String? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineDispatcher)
    private val isSyncInProgress = AtomicBoolean(false)
    private val logger = AmplifyLogging.logger<CloudWatchLogManager>()

    // Key the worker-factory registration and the WorkManager unique work by log group so that two clients
    // targeting different log groups don't hijack each other's delegate factory or scheduled auto-flush (both
    // the factory map and unique-work names are app-global).
    private val workerFactoryKey = "${CloudWatchRouterWorker.WORKER_FACTORY_KEY}.$logGroupName"
    private val uniqueWorkName = "${CloudWatchLogsSyncWorker.WORKER_NAME_TAG}.$logGroupName"

    init {
        // Register the delegate factory so the WorkManager-instantiated CloudWatchRouterWorker can
        // create a CloudWatchLogsSyncWorker wired to this manager.
        CloudWatchRouterWorker.workerFactories[workerFactoryKey] = CloudWatchWorkerFactory(this)
    }

    suspend fun saveLogEvent(event: CloudWatchLogEvent) = withContext(coroutineDispatcher) {
        try {
            cloudWatchLoggingDatabase.saveLogEvent(event)
            if (isCacheFull()) {
                syncLogEventsWithCloudwatch()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Failed to save event" }
            onWriteLogFailure(event.message, e)
        }
    }

    /**
     * Update the current user identifier. Pending events are flushed to the previous user's stream
     * before the identifier changes.
     */
    fun setUserIdentifier(identifier: String?) {
        coroutineScope.launch {
            try {
                syncLogEventsWithCloudwatch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(e) { "Failed to flush pending events before switching user" }
            } finally {
                userIdentityId = identifier
            }
        }
    }

    suspend fun startSync() {
        stopSync = false
        enqueueSync()
    }

    fun stopSync() {
        stopSync = true
        cancelSync()
        logger.debug { "Stopping sync" }
    }

    suspend fun syncLogEventsWithCloudwatch() {
        // Atomic guard so overlapping triggers (manual flush, cache-full write, WorkManager) can't
        // run concurrently and delete each other's rows.
        if (!isSyncInProgress.compareAndSet(false, true)) {
            return
        }
        withContext(coroutineDispatcher) {
            var lastAttemptedIds: List<Long> = emptyList()
            try {
                val streamDate = SimpleDateFormat(STREAM_DATE_FORMAT, Locale.US).format(Date())
                val client = awsCloudWatchLogsClient
                while (true) {
                    val queriedEvents = cloudWatchLoggingDatabase.queryAllEvents().toMutableList()
                    if (queriedEvents.isEmpty()) break
                    while (queriedEvents.isNotEmpty()) {
                        val deviceId = uniqueDeviceId()
                        // Default format: MM-dd-yyyy.deviceId.userId
                        val streamName = "$streamDate.$deviceId.${userIdentityId ?: "guest"}"

                        val batch = getNextBatch(queriedEvents)
                        // Events too large to ever be accepted are dropped so they can't block the buffer.
                        if (batch.oversizedIds.isNotEmpty()) {
                            cloudWatchLoggingDatabase.bulkDelete(batch.oversizedIds)
                        }
                        if (batch.events.isEmpty()) continue

                        createLogStreamIfNotCreated(streamName, logGroupName, client)
                        val response = client.putLogEvents(
                            PutLogEventsRequest {
                                logEvents = batch.events
                                logGroupName = this@CloudWatchLogManager.logGroupName
                                logStreamName = streamName
                            }
                        )
                        // Everything up to tooNewLogEventStartIndex was accepted; the rest stays for retry.
                        var acceptedIds = batch.sendableIds
                        response.rejectedLogEventsInfo?.tooNewLogEventStartIndex?.let {
                            acceptedIds = acceptedIds.slice(IntRange(0, it - 1))
                        }
                        lastAttemptedIds = acceptedIds
                        // Nothing accepted (all too new): re-querying would return the same rows forever.
                        if (acceptedIds.isEmpty()) return@withContext
                        cloudWatchLoggingDatabase.bulkDelete(acceptedIds)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                onFlushLogFailure(null, exception)
                if (isCacheFull()) {
                    cloudWatchLoggingDatabase.bulkDelete(lastAttemptedIds)
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

    /**
     * Pulls the next PutLogEvents batch off [queriedEvents], removing whatever it consumes. Enforces
     * the AWS per-batch limits, always includes at least one sendable event (so the buffer can't
     * stall), and diverts events too large to ever be sent into [LogBatch.oversizedIds] for dropping.
     */
    private fun getNextBatch(queriedEvents: MutableList<LogEvent>): LogBatch {
        var totalBatchSize = 0L
        val inputLogEvents = mutableListOf<InputLogEvent>()
        val sendableIds = mutableListOf<Long>()
        val oversizedIds = mutableListOf<Long>()
        var batchStartTimestamp = 0L
        val iterator = queriedEvents.iterator()
        while (iterator.hasNext()) {
            val cloudWatchEvent = iterator.next()
            val eventSize = cloudWatchEvent.message.toByteArray(Charsets.UTF_8).size + PER_EVENT_OVERHEAD_BYTES
            // A single event over the AWS per-event limit can never be sent; drop it instead of stalling.
            if (eventSize > MAX_EVENT_SIZE_BYTES) {
                oversizedIds.add(cloudWatchEvent.id)
                iterator.remove()
                continue
            }
            if (inputLogEvents.isNotEmpty() &&
                (
                    // The maximum number of log events in a batch is 10,000.
                    inputLogEvents.size >= MAX_LOG_EVENTS_PER_BATCH ||
                        // The maximum batch size is 1,048,576 bytes.
                        totalBatchSize + eventSize > MAX_BATCH_SIZE_BYTES ||
                        // A batch of log events in a single request cannot span more than 24 hours.
                        cloudWatchEvent.timestamp - batchStartTimestamp >= MAX_BATCH_SPAN.inWholeMilliseconds
                    )
            ) {
                break
            }
            if (inputLogEvents.isEmpty()) {
                batchStartTimestamp = cloudWatchEvent.timestamp
            }
            totalBatchSize += eventSize
            inputLogEvents.add(
                InputLogEvent {
                    timestamp = cloudWatchEvent.timestamp
                    message = cloudWatchEvent.message
                }
            )
            sendableIds.add(cloudWatchEvent.id)
            iterator.remove()
        }
        return LogBatch(inputLogEvents, sendableIds, oversizedIds)
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
                        CloudWatchRouterWorker.WORKER_ID to workerFactoryKey
                    )
                )
                .addTag(uniqueWorkName)
                .build()
            WorkManager.getInstance(context).beginUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            ).enqueue()
        }
    }

    private fun cancelSync() {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
    }

    private fun uniqueDeviceId(): String {
        val deviceIdKey = CloudWatchPreferences.DEVICE_ID_KEY
        val sharedPreferences =
            context.getSharedPreferences(CloudWatchPreferences.SHARED_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(deviceIdKey, null) ?: UUID.randomUUID().toString().also { id ->
            sharedPreferences.edit().putString(deviceIdKey, id).apply()
        }
    }

    private fun isCacheFull() = cloudWatchLoggingDatabase.isCacheFull(localStoreMaxSizeInMB)

    /** A single PutLogEvents batch plus the ids of events to drop as unsendable. */
    private data class LogBatch(
        val events: List<InputLogEvent>,
        val sendableIds: List<Long>,
        val oversizedIds: List<Long>
    )
}
