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
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.logging.cloudwatch.models.LoggingConstraints
import com.amplifyframework.logging.cloudwatch.models.UserLogLevel
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class LoggingConstraintsResolverTest {

    private lateinit var loggingConstraintsResolver: LoggingConstraintsResolver
    private val userId = "USER_ID"

    @Test
    fun `test default logLevel`() {
        val loggingConstraintsResolver = LoggingConstraintsResolver()
        assertEquals(LogLevel.ERROR, loggingConstraintsResolver.resolveLogLevel("ANY", CategoryType.LOGGING))
    }

    @Test
    fun `test local config for default`() = runTest {
        val remoteLoggingConstraintProvider = mockk<RemoteLoggingConstraintProvider>(relaxed = true)
        every {
            remoteLoggingConstraintProvider.fetchLoggingConfig(any<Consumer<LoggingConstraints>>(), any())
        }.answers {
            (it.invocation.args[1] as? Consumer<Exception>)?.accept(
                IllegalStateException()
            )
            Unit
        }
        every { remoteLoggingConstraintProvider.getConstraintsSyncInterval() }.answers { 20 }
        loggingConstraintsResolver = LoggingConstraintsResolver(
            context = ApplicationProvider.getApplicationContext(),
            localLoggingConstraint = LoggingConstraints(
                defaultLogLevel = LogLevel.INFO,
                categoryLogLevel = mapOf<CategoryType, LogLevel>(
                    CategoryType.AUTH to LogLevel.NONE,
                    CategoryType.API to LogLevel.WARN
                )
            ),
            remoteLoggingConstraintProvider = remoteLoggingConstraintProvider,
            userId = userId
        )
        val resolvedLogLevel = loggingConstraintsResolver.resolveLogLevel("ANY", CategoryType.LOGGING)
        assertEquals(LogLevel.INFO, resolvedLogLevel)
    }

    @Test
    fun `test local config for category`() = runTest {
        val remoteLoggingConstraintProvider = mockk<RemoteLoggingConstraintProvider>(relaxed = true)
        every {
            remoteLoggingConstraintProvider.fetchLoggingConfig(any<Consumer<LoggingConstraints>>(), any())
        }.answers {
            (it.invocation.args[1] as? Consumer<Exception>)?.accept(
                IllegalStateException()
            )
            Unit
        }
        every { remoteLoggingConstraintProvider.getConstraintsSyncInterval() }.answers { 20 }
        loggingConstraintsResolver = LoggingConstraintsResolver(
            context = ApplicationProvider.getApplicationContext(),
            localLoggingConstraint = LoggingConstraints(
                defaultLogLevel = LogLevel.INFO,
                categoryLogLevel = mapOf<CategoryType, LogLevel>(
                    CategoryType.AUTH to LogLevel.NONE,
                    CategoryType.API to LogLevel.WARN
                )
            ),
            remoteLoggingConstraintProvider = remoteLoggingConstraintProvider,
            userId = userId
        )
        val resolvedLogLevel = loggingConstraintsResolver.resolveLogLevel("ANY", CategoryType.AUTH)
        assertEquals(LogLevel.NONE, resolvedLogLevel)
    }

    @Test
    fun `test local config for user`() = runTest {
        val remoteLoggingConstraintProvider = mockk<RemoteLoggingConstraintProvider>(relaxed = true)
        every {
            remoteLoggingConstraintProvider.fetchLoggingConfig(any<Consumer<LoggingConstraints>>(), any())
        }.answers {
            (it.invocation.args[1] as? Consumer<Exception>)?.accept(
                IllegalStateException()
            )
        }
        every { remoteLoggingConstraintProvider.getConstraintsSyncInterval() }.answers { 20 }
        loggingConstraintsResolver = LoggingConstraintsResolver(
            context = ApplicationProvider.getApplicationContext(),
            localLoggingConstraint = LoggingConstraints(
                defaultLogLevel = LogLevel.INFO,
                categoryLogLevel = mapOf<CategoryType, LogLevel>(
                    CategoryType.AUTH to LogLevel.NONE,
                    CategoryType.API to LogLevel.WARN
                ),
                userLogLevel = mapOf(userId to UserLogLevel(defaultLogLevel = LogLevel.WARN))
            ),
            remoteLoggingConstraintProvider = remoteLoggingConstraintProvider,
            userId = userId
        )
        val resolvedLogLevel = loggingConstraintsResolver.resolveLogLevel("ANY", CategoryType.AUTH)
        assertEquals(LogLevel.WARN, resolvedLogLevel)
    }

    @Test
    fun `test remote config for default`() = runTest {
        val remoteLoggingConstraintProvider = mockk<RemoteLoggingConstraintProvider>(relaxed = true)
        val countDownLatch = CountDownLatch(1)
        every {
            remoteLoggingConstraintProvider.fetchLoggingConfig(any<Consumer<LoggingConstraints>>(), any())
        }.answers {
            (it.invocation.args[0] as Consumer<LoggingConstraints>).accept(
                LoggingConstraints(defaultLogLevel = LogLevel.VERBOSE)
            )
            countDownLatch.countDown()
        }
        every { remoteLoggingConstraintProvider.getConstraintsSyncInterval() }.answers { 20 }
        loggingConstraintsResolver = LoggingConstraintsResolver(
            context = ApplicationProvider.getApplicationContext(),
            localLoggingConstraint = LoggingConstraints(
                defaultLogLevel = LogLevel.INFO,
                categoryLogLevel = mapOf<CategoryType, LogLevel>(
                    CategoryType.AUTH to LogLevel.NONE,
                    CategoryType.API to LogLevel.WARN
                )
            ),
            remoteLoggingConstraintProvider = remoteLoggingConstraintProvider,
            userId = userId
        )
        countDownLatch.await(2, TimeUnit.SECONDS)
        val resolvedLogLevel = loggingConstraintsResolver.resolveLogLevel("ANY", CategoryType.AUTH)
        assertEquals(LogLevel.VERBOSE, resolvedLogLevel)
    }

    @Test
    fun `test remote config for category`() = runTest {
        val remoteLoggingConstraintProvider = mockk<RemoteLoggingConstraintProvider>(relaxed = true)
        val countDownLatch = CountDownLatch(1)
        every {
            remoteLoggingConstraintProvider.fetchLoggingConfig(any<Consumer<LoggingConstraints>>(), any())
        }.answers {
            (it.invocation.args[0] as Consumer<LoggingConstraints>).accept(
                LoggingConstraints(
                    defaultLogLevel = LogLevel.VERBOSE,
                    categoryLogLevel = mapOf(CategoryType.AUTH to LogLevel.DEBUG)
                )
            )
            countDownLatch.countDown()
        }
        every { remoteLoggingConstraintProvider.getConstraintsSyncInterval() }.answers { 20 }
        loggingConstraintsResolver = LoggingConstraintsResolver(
            context = ApplicationProvider.getApplicationContext(),
            localLoggingConstraint = LoggingConstraints(
                defaultLogLevel = LogLevel.INFO,
                categoryLogLevel = mapOf<CategoryType, LogLevel>(
                    CategoryType.AUTH to LogLevel.NONE,
                    CategoryType.API to LogLevel.WARN
                )
            ),
            remoteLoggingConstraintProvider = remoteLoggingConstraintProvider,
            userId = userId
        )
        // wait for remote config to be loaded
        countDownLatch.await(2, TimeUnit.SECONDS)
        val resolvedLogLevel = loggingConstraintsResolver.resolveLogLevel("ANY", CategoryType.AUTH)
        assertEquals(LogLevel.DEBUG, resolvedLogLevel)
    }

    @Test
    fun `test remote config for user`() = runTest {
        val remoteLoggingConstraintProvider = mockk<RemoteLoggingConstraintProvider>(relaxed = true)
        val countDownLatch = CountDownLatch(1)
        every {
            remoteLoggingConstraintProvider.fetchLoggingConfig(any<Consumer<LoggingConstraints>>(), any())
        }.answers {
            (it.invocation.args[0] as Consumer<LoggingConstraints>).accept(
                LoggingConstraints(
                    defaultLogLevel = LogLevel.VERBOSE,
                    categoryLogLevel = mapOf(CategoryType.AUTH to LogLevel.DEBUG),
                    userLogLevel = mapOf(userId to UserLogLevel(defaultLogLevel = LogLevel.WARN))
                )
            )
            countDownLatch.countDown()
        }
        every { remoteLoggingConstraintProvider.getConstraintsSyncInterval() }.answers { 20 }
        loggingConstraintsResolver = LoggingConstraintsResolver(
            context = ApplicationProvider.getApplicationContext(),
            localLoggingConstraint = LoggingConstraints(
                defaultLogLevel = LogLevel.INFO,
                categoryLogLevel = mapOf<CategoryType, LogLevel>(
                    CategoryType.AUTH to LogLevel.NONE,
                    CategoryType.API to LogLevel.WARN
                )
            ),
            remoteLoggingConstraintProvider = remoteLoggingConstraintProvider,
            userId = userId
        )
        // wait for remote config to be loaded
        countDownLatch.await(2, TimeUnit.SECONDS)
        val resolvedLogLevel = loggingConstraintsResolver.resolveLogLevel("ANY", CategoryType.AUTH)
        assertEquals(LogLevel.WARN, resolvedLogLevel)
    }

    @Test
    fun `test remote config is persisted locally`() = runTest {
        val remoteLoggingConstraintProvider = mockk<RemoteLoggingConstraintProvider>(relaxed = true)
        val context: Context = ApplicationProvider.getApplicationContext()
        val countDownLatch = CountDownLatch(1)
        val remoteConfig = LoggingConstraints(
            defaultLogLevel = LogLevel.VERBOSE,
            categoryLogLevel = mapOf(CategoryType.AUTH to LogLevel.DEBUG),
            userLogLevel = mapOf(userId to UserLogLevel(defaultLogLevel = LogLevel.WARN))
        )
        every {
            remoteLoggingConstraintProvider.fetchLoggingConfig(any<Consumer<LoggingConstraints>>(), any())
        }.answers {
            (it.invocation.args[0] as Consumer<LoggingConstraints>).accept(
                remoteConfig
            )
            countDownLatch.countDown()
        }
        every { remoteLoggingConstraintProvider.getConstraintsSyncInterval() }.answers { 20 }
        loggingConstraintsResolver = LoggingConstraintsResolver(
            context = context,
            localLoggingConstraint = LoggingConstraints(
                defaultLogLevel = LogLevel.INFO,
                categoryLogLevel = mapOf<CategoryType, LogLevel>(
                    CategoryType.AUTH to LogLevel.NONE,
                    CategoryType.API to LogLevel.WARN
                )
            ),
            remoteLoggingConstraintProvider = remoteLoggingConstraintProvider,
            userId = userId
        )
        // wait for remote config to be loaded
        countDownLatch.await(2, TimeUnit.SECONDS)
        val locallyPersistedRemoteConfig = LoggingConstraints.fromString(
            context.getSharedPreferences(
                AWSCloudWatchLoggingPlugin.SHARED_PREFERENCE_FILENAME,
                Context.MODE_PRIVATE
            ).getString(LoggingConstraintsResolver.REMOTE_LOGGING_CONSTRAINTS_KEY, null)
                ?: throw IllegalStateException("Failed to load config from shared preferences")
        )
        assertEquals(remoteConfig, locallyPersistedRemoteConfig)
    }
}
