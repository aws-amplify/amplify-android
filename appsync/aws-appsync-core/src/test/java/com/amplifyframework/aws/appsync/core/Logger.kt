/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.aws.appsync.core

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import org.junit.Test

// We only need to test the suppliers as the other logs levels don't react to thresholds. That is up to the
// class that implements Logger and writes the implementation for each log message type.
class LoggerTest {

    private val errorLog = "error"
    private val errorLogWithThrowable = Pair(errorLog, IllegalStateException())
    private val warnLog = "warn"
    private val warnLogWithThrowable = Pair(warnLog, IllegalStateException())
    private val infoLog = "info"
    private val debugLog = "debug"
    private val verboseLog = "verbose"

    @Test
    fun `test suppliers with error threshold`() {
        val logger = TestSupplierLogger(LogLevel.ERROR)

        writeTestLogs(logger)

        logger.errorLogs shouldBeEqual listOf(Pair(errorLog, null), errorLogWithThrowable)
        logger.warnLogs shouldHaveSize 0
        logger.infoLogs shouldHaveSize 0
        logger.debugLogs shouldHaveSize 0
        logger.verboseLogs shouldHaveSize 0
    }

    @Test
    fun `test suppliers with warn threshold`() {
        val logger = TestSupplierLogger(LogLevel.WARN)

        writeTestLogs(logger)

        logger.errorLogs shouldBeEqual listOf(Pair(errorLog, null), errorLogWithThrowable)
        logger.warnLogs shouldBeEqual listOf(Pair(warnLog, null), warnLogWithThrowable)
        logger.infoLogs shouldHaveSize 0
        logger.debugLogs shouldHaveSize 0
        logger.verboseLogs shouldHaveSize 0
    }

    @Test
    fun `test suppliers with info threshold`() {
        val logger = TestSupplierLogger(LogLevel.INFO)

        writeTestLogs(logger)

        logger.errorLogs shouldBeEqual listOf(Pair(errorLog, null), errorLogWithThrowable)
        logger.warnLogs shouldBeEqual listOf(Pair(warnLog, null), warnLogWithThrowable)
        logger.infoLogs shouldBeEqual listOf(infoLog)
        logger.debugLogs shouldHaveSize 0
        logger.verboseLogs shouldHaveSize 0
    }

    @Test
    fun `test suppliers with debug threshold`() {
        val logger = TestSupplierLogger(LogLevel.DEBUG)

        writeTestLogs(logger)

        logger.errorLogs shouldBeEqual listOf(Pair(errorLog, null), errorLogWithThrowable)
        logger.warnLogs shouldBeEqual listOf(Pair(warnLog, null), warnLogWithThrowable)
        logger.infoLogs shouldBeEqual listOf(infoLog)
        logger.debugLogs shouldBeEqual listOf(debugLog)
        logger.verboseLogs shouldHaveSize 0
    }

    @Test
    fun `test suppliers with verbose threshold`() {
        val logger = TestSupplierLogger(LogLevel.VERBOSE)

        writeTestLogs(logger)

        logger.errorLogs shouldBeEqual listOf(Pair(errorLog, null), errorLogWithThrowable)
        logger.warnLogs shouldBeEqual listOf(Pair(warnLog, null), warnLogWithThrowable)
        logger.infoLogs shouldBeEqual listOf(infoLog)
        logger.debugLogs shouldBeEqual listOf(debugLog)
        logger.verboseLogs shouldBeEqual listOf(verboseLog)
    }

    private fun writeTestLogs(logger: Logger) {
        logger.error { errorLog }
        logger.error(errorLogWithThrowable.second) { errorLogWithThrowable.first }
        logger.warn { warnLog }
        logger.warn(warnLogWithThrowable.second) { warnLogWithThrowable.first }
        logger.info { infoLog }
        logger.debug { debugLog }
        logger.verbose { verboseLog }
    }
}

private class TestSupplierLogger(override val thresholdLevel: LogLevel) : Logger {
    val errorLogs = mutableListOf<Pair<String, Throwable?>>()
    val warnLogs = mutableListOf<Pair<String, Throwable?>>()
    val infoLogs = mutableListOf<String>()
    val debugLogs = mutableListOf<String>()
    val verboseLogs = mutableListOf<String>()

    override fun error(message: String) {
        errorLogs.add(Pair(message, null))
    }

    override fun error(message: String, error: Throwable?) {
        errorLogs.add(Pair(message, error))
    }

    override fun warn(message: String) {
        warnLogs.add(Pair(message, null))
    }

    override fun warn(message: String, issue: Throwable?) {
        warnLogs.add(Pair(message, issue))
    }

    override fun info(message: String) {
        infoLogs.add(message)
    }

    override fun debug(message: String) {
        debugLogs.add(message)
    }

    override fun verbose(message: String) {
        verboseLogs.add(message)
    }
}
