/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.logging.cloudwatch.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amplifyframework.logging.cloudwatch.models.CloudWatchLogEvent
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.Matcher
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.collections.shouldMatchEach
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CloudWatchLoggingDatabaseInstrumentationTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val testCoroutine = UnconfinedTestDispatcher()
    private val loggingDbClass = CloudWatchLoggingDatabase(context, testCoroutine)

    private val testTimestamp1 = Instant.now().epochSecond
    private val testTimestamp2 = Instant.now().epochSecond + 300
    private val testTimestamp3 = Instant.now().epochSecond + 600
    private val testTimestamp4 = Instant.now().epochSecond + 900
    private val testCloudWatchLogEvent1 = CloudWatchLogEvent(testTimestamp1, "Customer Obsession")
    private val testCloudWatchLogEvent2 = CloudWatchLogEvent(testTimestamp2, "Ownership")
    private val testCloudWatchLogEvent3 = CloudWatchLogEvent(testTimestamp3, "Bias for Action")
    private val testCloudWatchLogEvent4 = CloudWatchLogEvent(testTimestamp4, "Frugality")

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testCoroutine)
    }

    @After
    fun tearDown() = runTest {
        // Clear the db used for testing
        loggingDbClass.clearDatabase()
    }

    /**
     * This test verifies if the CloudWatch Logs are saved as intended by passing
     * CloudWatchLogEvent to saveLogEvent() method
     */
    @Test
    fun saveLogEvent_Saves_Two_Logs_After_Two_Calls() = runTest {
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent1)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent2)

        val savedLogs = loggingDbClass.queryAllEvents()

        savedLogs shouldMatchEach listOf(
            { it shouldMatchEvent testCloudWatchLogEvent1 },
            { it shouldMatchEvent testCloudWatchLogEvent2 }
        )
    }

    /**
     * This test verifies if the CloudWatch Logs are retrieved
     */
    @Test
    fun queryAllEvents_Successfully_Retrieves_Every_Log() = runTest {
        loggingDbClass.queryAllEvents().size shouldBe 0
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent1)
        loggingDbClass.queryAllEvents().size shouldBe 1
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent3)

        val savedLogs = loggingDbClass.queryAllEvents()

        savedLogs shouldMatchEach listOf(
            { it shouldMatchEvent testCloudWatchLogEvent1 },
            { it shouldMatchEvent testCloudWatchLogEvent3 }
        )
        savedLogs[savedLogs.size -1] shouldMatchEvent testCloudWatchLogEvent3
    }

    /**
     * This test verifies if the CloudWatch Logs are correctly deleted by passing a list of one id
     * to bulkDelete() method
     */
    @Test
    fun bulkDelete_Removes_Only_One_Item() = runTest {
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent1)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent2)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent3)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent4)
        val deleteTargetLogs = loggingDbClass.queryAllEvents().take(1).map { it.id }

        loggingDbClass.bulkDelete(deleteTargetLogs)

        val savedLogs = loggingDbClass.queryAllEvents()

        savedLogs.size shouldBe 3
        savedLogs shouldMatchEach listOf(
            { it shouldMatchEvent testCloudWatchLogEvent2 },
            { it shouldMatchEvent testCloudWatchLogEvent3 },
            { it shouldMatchEvent testCloudWatchLogEvent4 }
        )
        savedLogs[0].message shouldBe "Ownership"
    }

    /**
     * This test verifies if the Cloudwatch Logs are correctly deleted by passing a list of two ids
     * to bulkDelete() method
     */
    @Test
    fun bulkDelete_Removes_With_List_Containing_Two_Ids() = runTest {
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent1)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent2)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent3)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent4)
        val deleteTargetLogs = loggingDbClass.queryAllEvents().map { it.id } .toMutableList()
        deleteTargetLogs.removeAt(1)
        deleteTargetLogs.removeAt(2)

        loggingDbClass.bulkDelete(deleteTargetLogs)

        val savedLogs = loggingDbClass.queryAllEvents()

        savedLogs.size shouldBe 2
        savedLogs shouldMatchEach listOf(
            { it shouldMatchEvent testCloudWatchLogEvent2 },
            { it shouldMatchEvent testCloudWatchLogEvent4 }
        )
        savedLogs[savedLogs.size -1].message shouldBe "Frugality"
    }

    /**
     * This test verifies if the Database is cleared after clearDatabase() method is called
     */
    @Test
    fun clearDatabase_Wipes_All_Logs_Out() = runTest {
        loggingDbClass.clearDatabase()
        loggingDbClass.queryAllEvents().size shouldBe 0
    }

    /**
     * This test verifies getDatabasePassphrase() does not change its value every call
     */
    @Test
    fun getDatabasePassphrase_Should_Be_Same_Each_Run() = runTest {
        loggingDbClass.getDatabasePassphrase() shouldBe loggingDbClass.getDatabasePassphrase()
    }

    private infix fun LogEvent.shouldMatchEvent(event: CloudWatchLogEvent) = this should Matcher {
        MatcherResult(
            it.message == event.message && it.timestamp == event.timestamp,
            { "event $it should match $event but it does not" },
            { "event $it should not match $event but it does" }
        )
    }

    private fun List<LogEvent>.shouldMatchEvents(vararg events: CloudWatchLogEvent) =
        this shouldMatchEach events.map { event -> { it shouldMatchEvent event } }
}