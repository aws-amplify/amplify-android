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

import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.logging.cloudwatch.models.CloudWatchLogEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CloudWatchLoggerTest {
    private val awsCloudWatchLoggingPluginBehavior = mockk<AWSCloudWatchLoggingPluginBehavior>()
    private val loggingConstraintsResolver = mockk<LoggingConstraintsResolver>()
    private val logsEventsQueue = ConcurrentLinkedQueue<CloudWatchLogEvent>()
    private val namespace = "NAMESPACE"
    private val categoryType = CategoryType.LOGGING
    private lateinit var cloudWatchLogger: CloudWatchLogger

    @Before
    fun setup() {
        cloudWatchLogger = CloudWatchLogger(
            namespace,
            categoryType,
            loggingConstraintsResolver,
            awsCloudWatchLoggingPluginBehavior,
            logsEventsQueue,
            Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
        )
    }

    @Test
    fun `test getThresholdLevel`() {
        every { loggingConstraintsResolver.resolveLogLevel(namespace, categoryType) }.answers { LogLevel.INFO }
        assertEquals(LogLevel.INFO, cloudWatchLogger.thresholdLevel)
    }

    @Test
    fun `test getThresholdLevel without category`() {
        cloudWatchLogger = CloudWatchLogger(
            namespace,
            null,
            loggingConstraintsResolver,
            awsCloudWatchLoggingPluginBehavior,
        )
        every { loggingConstraintsResolver.resolveLogLevel(namespace, null) }.answers { LogLevel.WARN }
        assertEquals(LogLevel.WARN, cloudWatchLogger.thresholdLevel)
    }

    @Test
    fun `persist logs in local queue when cloudwatch is not configured`() = runTest {
        val cloudWatchLogManager = mockk<CloudWatchLogManager>()
        val slot = mutableListOf<CloudWatchLogEvent>()
        every { awsCloudWatchLoggingPluginBehavior.isPluginEnabled }.answers { true }
        every { loggingConstraintsResolver.resolveLogLevel(namespace, categoryType) }.answers { LogLevel.ERROR }
        every { awsCloudWatchLoggingPluginBehavior.cloudWatchLogManager }.answers { null }
        coEvery { cloudWatchLogManager.saveLogEvent(capture(slot)) }.answers { }
        cloudWatchLogger.error("Test Message")
        assertEquals(1, logsEventsQueue.size)
        every { awsCloudWatchLoggingPluginBehavior.cloudWatchLogManager }.answers { cloudWatchLogManager }
        cloudWatchLogger.error("Test Message2")
        coVerify(exactly = 2) { cloudWatchLogManager.saveLogEvent(any()) }
        assertEquals(slot[0].message, "error/NAMESPACE: Test Message2")
        assertEquals(slot[1].message, "error/NAMESPACE: Test Message")
    }

    @Test
    fun `persist logs after cloudwatch is configured`() = runTest {
        val cloudWatchLogManager = mockk<CloudWatchLogManager>()
        val slot = mutableListOf<CloudWatchLogEvent>()
        every { awsCloudWatchLoggingPluginBehavior.isPluginEnabled }.answers { true }
        every { loggingConstraintsResolver.resolveLogLevel(namespace, categoryType) }.answers { LogLevel.ERROR }
        every { awsCloudWatchLoggingPluginBehavior.cloudWatchLogManager }.answers { cloudWatchLogManager }
        coEvery { cloudWatchLogManager.saveLogEvent(capture(slot)) }.answers { }
        cloudWatchLogger.error("Test Message")
        assertEquals(0, logsEventsQueue.size)
        coVerify(exactly = 1) { cloudWatchLogManager.saveLogEvent(any()) }
        assertEquals(slot[0].message, "error/NAMESPACE: Test Message")
    }
}
