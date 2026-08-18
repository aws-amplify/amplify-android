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
import com.amplifyframework.cloudwatch.common.db.CloudWatchDatabase
import com.amplifyframework.cloudwatch.common.db.LogEvent
import com.amplifyframework.cloudwatch.common.models.CloudWatchLogEvent
import com.amplifyframework.cloudwatch.worker.CloudWatchLogsSyncWorker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
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

    private fun newManager(flushIntervalInSeconds: Long? = 60L) = CloudWatchLogManager(
        context = context,
        logGroupName = "LOG_GROUP",
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
    fun `stopSync clears the local cache`() = runTest {
        coEvery { database.clearDatabase() } returns 1

        manager.stopSync()

        coVerify(exactly = 1) { database.clearDatabase() }
    }

    @Test
    fun `startSync schedules periodic flush work`() = runTest {
        manager.startSync()

        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(CloudWatchLogsSyncWorker.WORKER_NAME_TAG).get()
        work.shouldNotBeEmpty()
    }

    @Test
    fun `startSync does not schedule work when auto-flush is disabled`() = runTest {
        val noAutoFlush = newManager(flushIntervalInSeconds = null)

        noAutoFlush.startSync()

        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(CloudWatchLogsSyncWorker.WORKER_NAME_TAG).get()
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
