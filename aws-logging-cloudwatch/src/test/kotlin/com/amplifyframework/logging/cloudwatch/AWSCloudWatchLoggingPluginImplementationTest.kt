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

import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.logging.cloudwatch.models.AWSCloudWatchLoggingPluginConfiguration
import com.amplifyframework.logging.cloudwatch.models.LoggingConstraints
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AWSCloudWatchLoggingPluginImplementationTest {

    private val cloudWatchLogManager = mockk<CloudWatchLogManager>()
    private val loggingConstraintsResolver = mockk<LoggingConstraintsResolver>(relaxed = true)
    private lateinit var awsCloudWatchLoggingPluginImplementation: AWSCloudWatchLoggingPluginImplementation

    @Before
    fun setup() = runTest {
        awsCloudWatchLoggingPluginImplementation =
            AWSCloudWatchLoggingPluginImplementation(
                loggingConstraintsResolver,
                null,
                cloudWatchLogManager,
                dispatcher = UnconfinedTestDispatcher(this.testScheduler)
            )
    }

    @Test
    fun `test configure with plugin enabled`() = runTest {
        val loggingConstraintsSlot = slot<LoggingConstraints>()
        val awsLoggingConfig = AWSCloudWatchLoggingPluginConfiguration(
            enable = true,
            region = "REGION",
            logGroupName = "LOG_GROUP"
        )
        every { loggingConstraintsResolver::localLoggingConstraint.set(capture(loggingConstraintsSlot)) }.answers { }
        coEvery { cloudWatchLogManager.startSync() }.answers { }
        awsCloudWatchLoggingPluginImplementation.configure(
            awsLoggingConfig
        )
        verify { loggingConstraintsResolver::localLoggingConstraint.set(awsLoggingConfig.loggingConstraints) }
        assertEquals(awsLoggingConfig.loggingConstraints, loggingConstraintsSlot.captured)
        coVerify(exactly = 1) { cloudWatchLogManager.startSync() }
    }

    @Test
    fun `test configure with plugin disabled`() = runTest {
        val awsLoggingConfig = AWSCloudWatchLoggingPluginConfiguration(
            enable = false,
            region = "REGION",
            logGroupName = "LOG_GROUP"
        )
        every { loggingConstraintsResolver::localLoggingConstraint.set(any()) }.answers { }
        coEvery { cloudWatchLogManager.startSync() }.answers { }
        awsCloudWatchLoggingPluginImplementation.configure(
            awsLoggingConfig
        )
        verify { loggingConstraintsResolver::localLoggingConstraint.set(awsLoggingConfig.loggingConstraints) }
        coVerify(exactly = 0) { cloudWatchLogManager.startSync() }
    }

    @Test
    fun `on enable`() = runTest {
        coEvery { cloudWatchLogManager.startSync() }.answers { }
        awsCloudWatchLoggingPluginImplementation.enable()
        assertTrue(awsCloudWatchLoggingPluginImplementation.isPluginEnabled)
        coVerify(exactly = 1) { cloudWatchLogManager.startSync() }
    }

    @Test
    fun `on disable`() {
        every { cloudWatchLogManager.stopSync() }.answers { }
        awsCloudWatchLoggingPluginImplementation.disable()
        assertFalse(awsCloudWatchLoggingPluginImplementation.isPluginEnabled)
        verify(exactly = 1) { cloudWatchLogManager.stopSync() }
    }

    @Test
    fun `on flush logs success`() = runTest {
        val onSuccess = mockk<Action>()
        val onError = mockk<Consumer<AmplifyException>>()
        coEvery { cloudWatchLogManager.syncLogEventsWithCloudwatch() }.answers { }
        every { onSuccess.call() }.answers { }
        awsCloudWatchLoggingPluginImplementation.flushLogs(onSuccess, onError)
        coVerify(exactly = 1) { cloudWatchLogManager.syncLogEventsWithCloudwatch() }
        verify(exactly = 1) { onSuccess.call() }
        verify(exactly = 0) { onError.accept(any()) }
    }

    @Test
    fun `on flush logs error`() = runTest {
        val onSuccess = mockk<Action>()
        val onError = mockk<Consumer<AmplifyException>>()
        val exception = slot<AmplifyException>()
        coEvery { cloudWatchLogManager.syncLogEventsWithCloudwatch() }.throws(IllegalStateException())
        every { onError.accept(capture(exception)) }.answers { }
        awsCloudWatchLoggingPluginImplementation.flushLogs(onSuccess, onError)
        coVerify(exactly = 1) { cloudWatchLogManager.syncLogEventsWithCloudwatch() }
        verify(exactly = 0) { onSuccess.call() }
        verify(exactly = 1) { onError.accept(exception.captured) }
        assertEquals("Failed to flush logs", exception.captured.message)
    }

    @Test
    fun `test logger for namespace`() {
        val namespace = "NAMESPACE"
        val logger = awsCloudWatchLoggingPluginImplementation.logger(namespace) as CloudWatchLogger
        assertEquals(namespace, logger.namespace)
    }
}
