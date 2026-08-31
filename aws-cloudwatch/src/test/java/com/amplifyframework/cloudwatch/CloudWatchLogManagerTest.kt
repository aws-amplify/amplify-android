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
package com.amplifyframework.cloudwatch

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import aws.sdk.kotlin.services.cloudwatchlogs.model.CreateLogStreamResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.DescribeLogStreamsResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.InputLogEvent
import aws.sdk.kotlin.services.cloudwatchlogs.model.LogStream
import aws.sdk.kotlin.services.cloudwatchlogs.model.PutLogEventsRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.PutLogEventsResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.RejectedLogEventsInfo
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.cloudwatch.db.CloudWatchDatabase
import com.amplifyframework.cloudwatch.db.LogEvent
import com.amplifyframework.cloudwatch.models.CloudWatchLogEvent
import com.amplifyframework.cloudwatch.worker.CloudWatchLogsSyncWorker
import com.amplifyframework.cloudwatch.worker.CloudWatchRouterWorker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, InternalAmplifyApi::class)
@RunWith(RobolectricTestRunner::class)
internal class CloudWatchLogManagerTest {

    private val cloudWatchLogsClient = mockk<CloudWatchLogsClient>()
    private val database = mockk<CloudWatchDatabase>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val writeFailures = mutableListOf<Throwable?>()
    private val flushFailures = mutableListOf<Throwable?>()
    private val putRequestSlot = slot<PutLogEventsRequest>()
    private val putRequests = mutableListOf<PutLogEventsRequest>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var manager: CloudWatchLogManager

    @Before
    fun setup() = runTest {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        every { database.isCacheFull(any()) } returns false
        coEvery { database.queryAllEvents() } returns emptyList()

        manager = newManager()
    }

    @After
    fun cleanup() {
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    private fun newManager(flushIntervalInSeconds: Long? = 60L, logGroupName: String = "LOG_GROUP") =
        CloudWatchLogManager(
            context = context,
            logGroupName = logGroupName,
            localStoreMaxSizeInMB = 5,
            flushIntervalInSeconds = flushIntervalInSeconds,
            awsCloudWatchLogsClient = cloudWatchLogsClient,
            databaseName = "test.cloudwatch.db",
            passphrasePreferencesName = "test.prefs",
            onWriteLogFailure = { _, cause -> writeFailures.add(cause) },
            onFlushLogFailure = { _, cause -> flushFailures.add(cause) },
            cloudWatchLoggingDatabase = database,
            coroutineDispatcher = testDispatcher
        )

    /** Stubs a successful describe/create/put cycle, capturing the put request. */
    private fun stubSuccessfulUpload(tooNewLogEventStartIndex: Int? = null, existingStream: Boolean = false) {
        coEvery { cloudWatchLogsClient.describeLogStreams(any()) } returns DescribeLogStreamsResponse.invoke {
            logStreams = if (existingStream) listOf(LogStream.invoke { logStreamName = "existing" }) else null
        }
        coEvery { cloudWatchLogsClient.createLogStream(any()) } returns CreateLogStreamResponse.invoke { }
        coEvery { cloudWatchLogsClient.putLogEvents(capture(putRequestSlot)) } returns PutLogEventsResponse.invoke {
            rejectedLogEventsInfo = RejectedLogEventsInfo.invoke {
                this.tooNewLogEventStartIndex = tooNewLogEventStartIndex
            }
        }
        coJustRun { database.bulkDelete(any()) }
    }

    /** Stubs a successful describe/put cycle, capturing every batch's put request into [putRequests]. */
    private fun stubBatchCapture() {
        coEvery { cloudWatchLogsClient.describeLogStreams(any()) } returns DescribeLogStreamsResponse.invoke {
            logStreams = listOf(LogStream.invoke { logStreamName = "existing" })
        }
        coEvery { cloudWatchLogsClient.createLogStream(any()) } returns CreateLogStreamResponse.invoke { }
        coEvery { cloudWatchLogsClient.putLogEvents(capture(putRequests)) } returns PutLogEventsResponse.invoke { }
        coJustRun { database.bulkDelete(any()) }
    }

    @Test
    fun `batches split at the 10,000 event per-request limit`() = runTest {
        val events = (1..10_001L).map { LogEvent(timestamp = it, message = "m", id = it) }
        coEvery { database.queryAllEvents() } returns events andThen emptyList()
        stubBatchCapture()

        manager.syncLogEventsWithCloudwatch()

        putRequests.map { it.logEvents!!.size } shouldBe listOf(10_000, 1)
    }

    @Test
    fun `batches split at the 1 MB per-request size limit`() = runTest {
        // Each event is 100_000 bytes + 26 overhead = 100_026; ten fit under 1_048_576, the eleventh spills over.
        val big = "a".repeat(100_000)
        val events = (1..11L).map { LogEvent(timestamp = it, message = big, id = it) }
        coEvery { database.queryAllEvents() } returns events andThen emptyList()
        stubBatchCapture()

        manager.syncLogEventsWithCloudwatch()

        putRequests.map { it.logEvents!!.size } shouldBe listOf(10, 1)
    }

    @Test
    fun `batches split when events span more than 24 hours`() = runTest {
        val hour = 3_600_000L
        // 0h and 1h share a batch; 25h is >= 24h from the batch start, so it opens a new batch.
        val events = listOf(
            LogEvent(timestamp = 0L, message = "m", id = 1L),
            LogEvent(timestamp = hour, message = "m", id = 2L),
            LogEvent(timestamp = 25 * hour, message = "m", id = 3L)
        )
        coEvery { database.queryAllEvents() } returns events andThen emptyList()
        stubBatchCapture()

        manager.syncLogEventsWithCloudwatch()

        putRequests.map { it.logEvents!!.size } shouldBe listOf(2, 1)
    }

    @Test
    fun `events larger than the per-event limit are dropped, not sent`() = runTest {
        val oversized = "a".repeat(262_145) // exceeds the 262_144-byte per-event limit on its own
        val events = listOf(
            LogEvent(timestamp = 1L, message = oversized, id = 1L),
            LogEvent(timestamp = 2L, message = "ok", id = 2L)
        )
        coEvery { database.queryAllEvents() } returns events andThen emptyList()
        stubBatchCapture()

        manager.syncLogEventsWithCloudwatch()

        // Only the sendable event is uploaded...
        putRequests.flatMap { it.logEvents!! }.map { it.message } shouldBe listOf("ok")
        // ...and the oversized event is deleted so it can't permanently block the buffer.
        coVerify { database.bulkDelete(listOf(1L)) }
    }

    @Test
    fun `saveLogEvent persists the event when the cache is not full`() = runTest {
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "Sample log")
        coEvery { database.saveLogEvent(event) } returns 1L

        manager.saveLogEvent(event)

        coVerify(exactly = 1) { database.saveLogEvent(event) }
        coVerify(exactly = 0) { cloudWatchLogsClient.putLogEvents(any()) }
    }

    @Test
    fun `saveLogEvent flushes to CloudWatch when the cache is full`() = runTest {
        val event = CloudWatchLogEvent(1_000L, "Sample log")
        every { database.isCacheFull(any()) } returns true
        coEvery { database.saveLogEvent(event) } returns 1L
        coEvery { database.queryAllEvents() } returns
            listOf(LogEvent(event.timestamp, event.message, 1L)) andThen emptyList()
        stubSuccessfulUpload()

        manager.saveLogEvent(event)

        putRequestSlot.captured.logGroupName shouldBe "LOG_GROUP"
        putRequestSlot.captured.logEvents shouldBe listOf(
            InputLogEvent.invoke {
                message = event.message
                timestamp = event.timestamp
            }
        )
    }

    @Test
    fun `saveLogEvent reports a write failure through the callback`() = runTest {
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "Sample log")
        val error = RuntimeException("db write failed")
        coEvery { database.saveLogEvent(event) } throws error

        manager.saveLogEvent(event)

        writeFailures shouldBe listOf(error)
    }

    @Test
    fun `sync creates the log stream when it does not exist`() = runTest {
        coEvery { database.queryAllEvents() } returns listOf(LogEvent(1L, "hi", 1L)) andThen emptyList()
        stubSuccessfulUpload(existingStream = false)

        manager.syncLogEventsWithCloudwatch()

        coVerify(exactly = 1) { cloudWatchLogsClient.createLogStream(any()) }
    }

    @Test
    fun `sync reuses an existing log stream`() = runTest {
        coEvery { database.queryAllEvents() } returns listOf(LogEvent(1L, "hi", 1L)) andThen emptyList()
        stubSuccessfulUpload(existingStream = true)

        manager.syncLogEventsWithCloudwatch()

        coVerify(exactly = 0) { cloudWatchLogsClient.createLogStream(any()) }
    }

    @Test
    fun `sync deletes only accepted events keeping too-new events for retry`() = runTest {
        coEvery { database.queryAllEvents() } returns
            listOf(LogEvent(1L, "first", 1L), LogEvent(2L, "second", 2L)) andThen emptyList()
        // Index 1 (the second event) is too new, so only the first id should be deleted.
        stubSuccessfulUpload(tooNewLogEventStartIndex = 1)

        manager.syncLogEventsWithCloudwatch()

        coVerify(exactly = 1) { database.bulkDelete(listOf(1L)) }
    }

    @Test
    fun `sync reports a flush failure through the callback and rethrows`() = runTest {
        val error = RuntimeException("put failed")
        coEvery { database.queryAllEvents() } returns listOf(LogEvent(1L, "hi", 1L))
        coEvery { cloudWatchLogsClient.describeLogStreams(any()) } returns
            DescribeLogStreamsResponse.invoke { logStreams = null }
        coEvery { cloudWatchLogsClient.createLogStream(any()) } returns CreateLogStreamResponse.invoke { }
        coEvery { cloudWatchLogsClient.putLogEvents(any()) } throws error

        shouldThrow<RuntimeException> { manager.syncLogEventsWithCloudwatch() }

        flushFailures shouldBe listOf(error)
    }

    @Test
    fun `flush failure while cache is full drops the failed batch so the buffer can drain`() = runTest {
        // The cache is full and CloudWatch rejects the batch. The recovery valve must drop the batch
        // that failed; otherwise getNextBatch keeps re-picking the same rows and the buffer never drains.
        every { database.isCacheFull(any()) } returns true
        coEvery { database.queryAllEvents() } returns listOf(LogEvent(1L, "a", 1L), LogEvent(2L, "b", 2L))
        coEvery { cloudWatchLogsClient.describeLogStreams(any()) } returns
            DescribeLogStreamsResponse.invoke { logStreams = listOf(LogStream.invoke { logStreamName = "existing" }) }
        coEvery { cloudWatchLogsClient.putLogEvents(any()) } throws RuntimeException("put failed")
        coJustRun { database.bulkDelete(any()) }

        shouldThrow<RuntimeException> { manager.syncLogEventsWithCloudwatch() }

        coVerify(exactly = 1) { database.bulkDelete(listOf(1L, 2L)) }
    }

    @Test
    fun `flush failure while cache is not full keeps the batch for retry`() = runTest {
        every { database.isCacheFull(any()) } returns false
        coEvery { database.queryAllEvents() } returns listOf(LogEvent(1L, "a", 1L))
        coEvery { cloudWatchLogsClient.describeLogStreams(any()) } returns
            DescribeLogStreamsResponse.invoke { logStreams = listOf(LogStream.invoke { logStreamName = "existing" }) }
        coEvery { cloudWatchLogsClient.putLogEvents(any()) } throws RuntimeException("put failed")
        coJustRun { database.bulkDelete(any()) }

        shouldThrow<RuntimeException> { manager.syncLogEventsWithCloudwatch() }

        // Nothing dropped: the entries stay buffered for the next flush.
        coVerify(exactly = 0) { database.bulkDelete(any()) }
    }

    @Test
    fun `a cache-full flush failure is reported only as a flush failure, not a write failure`() = runTest {
        val event = CloudWatchLogEvent(1_000L, "Sample log")
        every { database.isCacheFull(any()) } returns true
        coEvery { database.saveLogEvent(event) } returns 1L
        coEvery { database.queryAllEvents() } returns listOf(LogEvent(event.timestamp, event.message, 1L))
        coEvery { cloudWatchLogsClient.describeLogStreams(any()) } returns
            DescribeLogStreamsResponse.invoke { logStreams = listOf(LogStream.invoke { logStreamName = "existing" }) }
        val error = RuntimeException("put failed")
        coEvery { cloudWatchLogsClient.putLogEvents(any()) } throws error
        coJustRun { database.bulkDelete(any()) }

        manager.saveLogEvent(event)

        // The event was persisted successfully, so the upload failure must not surface as a write failure.
        writeFailures.shouldBeEmpty()
        flushFailures shouldBe listOf(error)
    }

    @Test
    fun `a multi-batch flush ensures each log stream only once`() = runTest {
        val events = (1..10_001L).map { LogEvent(timestamp = it, message = "m", id = it) }
        coEvery { database.queryAllEvents() } returns events andThen emptyList()
        stubBatchCapture()

        manager.syncLogEventsWithCloudwatch()

        // Two batches share one stream name, so describeLogStreams is issued once, not per batch.
        coVerify(exactly = 1) { cloudWatchLogsClient.describeLogStreams(any()) }
        putRequests shouldHaveSize 2
    }

    @Test
    fun `stream name uses guest when no user identifier is set`() = runTest {
        coEvery { database.queryAllEvents() } returns listOf(LogEvent(1L, "hi", 1L)) andThen emptyList()
        stubSuccessfulUpload()

        manager.syncLogEventsWithCloudwatch()

        putRequestSlot.captured.logStreamName!! shouldEndWith ".guest"
    }

    @Test
    fun `stream name uses the user identifier when set`() = runTest {
        coEvery { database.queryAllEvents() } returns emptyList()
        manager.setUserIdentifier("USER_ID")

        coEvery { database.queryAllEvents() } returns listOf(LogEvent(1L, "hi", 1L)) andThen emptyList()
        stubSuccessfulUpload()

        manager.syncLogEventsWithCloudwatch()

        putRequestSlot.captured.logStreamName!! shouldEndWith ".USER_ID"
    }

    @Test
    fun `stopSync preserves the local cache and cancels scheduled work`() = runTest {
        manager.startSync()

        manager.stopSync()

        // Disabling preserves buffered entries; the local cache is never cleared.
        coVerify(exactly = 0) { database.clearDatabase() }
        // The scheduled auto-flush is canceled.
        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("${CloudWatchLogsSyncWorker.WORKER_NAME_TAG}.LOG_GROUP").get()
        work.all { it.state == WorkInfo.State.CANCELLED } shouldBe true
    }

    @Test
    fun `startSync schedules periodic flush work`() = runTest {
        manager.startSync()

        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("${CloudWatchLogsSyncWorker.WORKER_NAME_TAG}.LOG_GROUP").get()
        work.shouldNotBeEmpty()
    }

    @Test
    fun `distinct log groups get isolated worker factories and scheduled work`() = runTest {
        newManager(logGroupName = "group-A").startSync()
        newManager(logGroupName = "group-B").startSync()

        val wm = WorkManager.getInstance(context)
        wm.getWorkInfosForUniqueWork("${CloudWatchLogsSyncWorker.WORKER_NAME_TAG}.group-A").get().shouldNotBeEmpty()
        wm.getWorkInfosForUniqueWork("${CloudWatchLogsSyncWorker.WORKER_NAME_TAG}.group-B").get().shouldNotBeEmpty()
        // Each manager registers under its own factory key, so a second client can't hijack the first's routing.
        CloudWatchRouterWorker.workerFactories.keys shouldContainAll listOf(
            "${CloudWatchRouterWorker.WORKER_FACTORY_KEY}.group-A",
            "${CloudWatchRouterWorker.WORKER_FACTORY_KEY}.group-B"
        )
    }

    @Test
    fun `startSync does not schedule work when auto-flush is disabled`() = runTest {
        val noAutoFlush = newManager(flushIntervalInSeconds = null)

        noAutoFlush.startSync()

        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("${CloudWatchLogsSyncWorker.WORKER_NAME_TAG}.LOG_GROUP").get()
        work.shouldBeEmpty()
    }

    @Test
    fun `sync does nothing when there are no cached events`() = runTest {
        coEvery { database.queryAllEvents() } returns emptyList()

        manager.syncLogEventsWithCloudwatch()

        coVerify(exactly = 0) { cloudWatchLogsClient.putLogEvents(any()) }
        writeFailures.shouldBeEmpty()
        flushFailures.shouldBeEmpty()
    }

    @Test
    fun `too-new event ids are retained across a single flush`() = runTest {
        // Sanity check that a fully-accepted batch deletes every id.
        coEvery { database.queryAllEvents() } returns
            listOf(LogEvent(1L, "a", 1L), LogEvent(2L, "b", 2L)) andThen emptyList()
        stubSuccessfulUpload(tooNewLogEventStartIndex = null)

        manager.syncLogEventsWithCloudwatch()

        coVerify(exactly = 1) { database.bulkDelete(listOf(1L, 2L)) }
    }

    @Test
    fun `write failures list stays empty on a successful save`() = runTest {
        val event = CloudWatchLogEvent(System.currentTimeMillis(), "ok")
        coEvery { database.saveLogEvent(event) } returns 1L

        manager.saveLogEvent(event)

        writeFailures shouldHaveSize 0
    }
}
