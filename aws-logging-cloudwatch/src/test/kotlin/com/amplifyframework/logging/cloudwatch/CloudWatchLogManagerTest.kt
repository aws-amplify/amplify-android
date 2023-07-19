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
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import aws.sdk.kotlin.services.cloudwatchlogs.model.CreateLogStreamResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.DescribeLogStreamsResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.InputLogEvent
import aws.sdk.kotlin.services.cloudwatchlogs.model.PutLogEventsRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.PutLogEventsResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.RejectedLogEventsInfo
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.logging.cloudwatch.db.CloudWatchLoggingDatabase
import com.amplifyframework.logging.cloudwatch.db.LogEvent
import com.amplifyframework.logging.cloudwatch.models.AWSCloudWatchLoggingPluginConfiguration
import com.amplifyframework.logging.cloudwatch.models.CloudWatchLogEvent
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class CloudWatchLogManagerTest {
    private val pluginConfiguration = mockk<AWSCloudWatchLoggingPluginConfiguration>()
    private val loggingConstraintsResolver = mockk<LoggingConstraintsResolver>()
    private val customCognitoCredentialsProvider = mockk<CustomCognitoCredentialsProvider>()
    private val cloudWatchLogsClient = mockk<CloudWatchLogsClient>()
    private val cloudWatchLoggingDatabase = mockk<CloudWatchLoggingDatabase>()
    private lateinit var cloudWatchLogManager: CloudWatchLogManager
    private var context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var userIdSlot: CapturingSlot<String>

    @Before
    fun setup() = runTest {
        val config: Configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            config
        )
        userIdSlot = slot()
        every { cloudWatchLoggingDatabase.isCacheFull(any()) }.answers { false }
        every { loggingConstraintsResolver::userId.set(any()) }.answers { }
        every { pluginConfiguration.localStoreMaxSizeInMB }.answers { 1 }
        every { pluginConfiguration.logGroupName }.answers { "LOG_GROUP" }
        coEvery { customCognitoCredentialsProvider.getCurrentUser() }.answers { AuthUser("USER_ID", "USERNAME") }
        coEvery {
            cloudWatchLoggingDatabase.queryAllEvents()
        } returns emptyList()
        every { loggingConstraintsResolver::userId.set(capture(userIdSlot)) }.answers { }
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        cloudWatchLogManager = CloudWatchLogManager(
            context,
            pluginConfiguration,
            cloudWatchLogsClient,
            loggingConstraintsResolver,
            cloudWatchLoggingDatabase,
            customCognitoCredentialsProvider,
            testDispatcher
        )
    }

    @Test
    fun `on saveLogEvents and cache not full`() = runTest {
        val cloudwatchEvent = CloudWatchLogEvent(System.currentTimeMillis(), "Sample log")
        cloudWatchLogManager.saveLogEvent(cloudwatchEvent)
        assertEquals("USER_ID", userIdSlot.captured)
        coVerify(exactly = 1) { cloudWatchLoggingDatabase.saveLogEvent(cloudwatchEvent) }
    }

    @Test
    fun `on saveLogEvents and cache full`() = runTest {
        val cloudwatchEvent = CloudWatchLogEvent(System.currentTimeMillis(), "Sample log")
        val logEvent = LogEvent(cloudwatchEvent.timestamp, cloudwatchEvent.message, 1L)
        every { cloudWatchLoggingDatabase.isCacheFull(any()) }.answers { true }
        coEvery { cloudWatchLoggingDatabase.saveLogEvent(any()) }.answers { Uri.parse("something/1") }
        coEvery {
            cloudWatchLoggingDatabase.queryAllEvents()
        } returns listOf(logEvent) andThen emptyList()
        coEvery {
            cloudWatchLogsClient.describeLogStreams(any())
        }.answers { DescribeLogStreamsResponse.invoke { logStreams = null } }
        coEvery {
            cloudWatchLogsClient.createLogStream(any())
        }.answers { CreateLogStreamResponse.invoke { } }
        val putRequestSlot = slot<PutLogEventsRequest>()
        coEvery { cloudWatchLogsClient.putLogEvents(capture(putRequestSlot)) }.answers {
            PutLogEventsResponse.invoke {
                rejectedLogEventsInfo = RejectedLogEventsInfo.invoke { tooNewLogEventStartIndex = null }
            }
        }
        cloudWatchLogManager.saveLogEvent(cloudwatchEvent)
        assertEquals("USER_ID", userIdSlot.captured)
        coVerify(exactly = 1) { cloudWatchLoggingDatabase.saveLogEvent(cloudwatchEvent) }
        assertEquals("LOG_GROUP", putRequestSlot.captured.logGroupName)
        assertEquals(
            listOf(
                InputLogEvent {
                    message = cloudwatchEvent.message
                    timestamp = cloudwatchEvent.timestamp
                }
            ),
            putRequestSlot.captured.logEvents
        )
    }

    @Test
    fun `test onStopSync`() = runTest {
        context.getSharedPreferences(AWSCloudWatchLoggingPlugin.SHARED_PREFERENCE_FILENAME, Context.MODE_PRIVATE).edit()
            .putString(LoggingConstraintsResolver.REMOTE_LOGGING_CONSTRAINTS_KEY, "{}").apply()
        coEvery { cloudWatchLoggingDatabase.clearDatabase() }.answers { 1 }
        cloudWatchLogManager.stopSync()
        coVerify(exactly = 1) { cloudWatchLoggingDatabase.clearDatabase() }
        assertFalse(
            context.getSharedPreferences(
                AWSCloudWatchLoggingPlugin.SHARED_PREFERENCE_FILENAME,
                Context.MODE_PRIVATE
            ).contains(LoggingConstraintsResolver.REMOTE_LOGGING_CONSTRAINTS_KEY)
        )
    }
}
