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
    private val database by lazy {
        System.loadLibrary("sqlcipher")
        val passPhraseGetter = loggingDbClass.javaClass.getDeclaredMethod("getDatabasePassphrase", String::class.java)
        passPhraseGetter.isAccessible = true
        CloudWatchDatabaseHelper(context).getWritableDatabase(passPhraseGetter.invoke(loggingDbClass) as String)
    }

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
    fun `saveLogEvent$aws_logging_cloudwatch_debug`() = runTest {
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent1)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent2)

        val savedLogs = loggingDbClass.queryAllEvents()

        assertNotNull(savedLogs)
        assertEquals(2, savedLogs.size)
        assertEquals("Customer Obsession", savedLogs[0].message)
        assertEquals(testTimestamp2, savedLogs[1].timestamp)
        loggingDbClass.clearDatabase()
    }

    /**
     * This test verifies if the CloudWatch Logs are retrieved
     */
    @Test
    fun `queryAllEvents$aws_logging_cloudwatch_debug`() = runTest {
        assertEquals(0, loggingDbClass.queryAllEvents().size)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent1)
        assertEquals(1, loggingDbClass.queryAllEvents().size)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent3)

        val savedLogs = loggingDbClass.queryAllEvents()
        assertEquals("Bias for Action", savedLogs[savedLogs.size -1].message)
        loggingDbClass.clearDatabase()
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
        var deleteTargetLogs: MutableList<Long> = loggingDbClass.queryAllEvents().map { it.id } .toMutableList()
        deleteTargetLogs = deleteTargetLogs.subList(0,1)

        loggingDbClass.bulkDelete(deleteTargetLogs)

        val savedLogs = loggingDbClass.queryAllEvents()

        assertEquals(3, savedLogs.size)
        assertEquals("Ownership", savedLogs[0].message)
        loggingDbClass.clearDatabase()
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
        val deleteTargetLogs: MutableList<Long> = loggingDbClass.queryAllEvents().map { it.id } .toMutableList()
        deleteTargetLogs.removeAt(1)
        deleteTargetLogs.removeAt(2)

        loggingDbClass.bulkDelete(deleteTargetLogs)

        val savedLogs = loggingDbClass.queryAllEvents()

        assertEquals(2, savedLogs.size)
        assertEquals("Frugality", savedLogs[1].message)
        loggingDbClass.clearDatabase()
    }

    @Test
    fun `isCacheFull$aws_logging_cloudwatch_debug`() {
    }

    /**
     * This test verifies if the Database is cleared after clearDatabase() method is called
     */
    @Test
    fun clearDatabase_Successfully_Deletes_All_Logs() = runTest {
        assertEquals(0, loggingDbClass.queryAllEvents().size)
        loggingDbClass.saveLogEvent(testCloudWatchLogEvent1)
        assertEquals(1, loggingDbClass.queryAllEvents().size)
        loggingDbClass.clearDatabase()
        assertEquals(0, loggingDbClass.queryAllEvents().size)
    }
}