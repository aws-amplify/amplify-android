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

package com.amplifyframework.foundation.logging

import com.amplifyframework.foundation.logging.LogLevel.Debug
import com.amplifyframework.foundation.logging.LogLevel.Error
import com.amplifyframework.foundation.logging.LogLevel.Info
import com.amplifyframework.foundation.logging.LogLevel.Verbose
import com.amplifyframework.foundation.logging.LogLevel.Warn
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import org.junit.Test

class AmplifyLoggingTest {

    private val sink = TestSink()

    @BeforeTest
    fun setup() {
        AmplifyLogging.addSink(sink)
    }

    @AfterTest
    fun teardown() {
        AmplifyLogging.removeSink(sink)
    }

    @Test
    fun `logs have appropriate name set`() {
        val logger1 = AmplifyLogging.logger("nameTest")
        val logger2 = AmplifyLogging.logger<AmplifyLoggingTest>()

        logger1.verbose("message1")
        logger2.verbose("message2")

        sink.logs shouldHaveSize 2
        sink.logs[0].name shouldBe "nameTest"
        sink.logs[1].name shouldBe "AmplifyLoggingTest"
    }

    @Test
    fun `logs have appropriate level set`() {
        val logger = AmplifyLogging.logger("test")

        logger.info("message1")
        logger.debug("message2")
        logger.error("message3")
        logger.warn("message4")
        logger.verbose("message5")

        sink.logs shouldHaveSize 5
        sink.logs[0].level shouldBe Info
        sink.logs[1].level shouldBe Debug
        sink.logs[2].level shouldBe Error
        sink.logs[3].level shouldBe Warn
        sink.logs[4].level shouldBe Verbose
    }

    @Test
    fun `logs below threshold are not emitted`() {
        val logger = AmplifyLogging.logger("test")
        sink.threshold = Warn

        logger.verbose("verbose")
        logger.debug("debug")
        logger.info("info")
        logger.warn("warn")
        logger.error("error")

        sink.logs shouldHaveSize 2
        sink.logs[0].level shouldBe Warn
        sink.logs[1].level shouldBe Error
    }

    @Test
    fun `logs with message are captured`() {
        val logger = AmplifyLogging.logger("loggerTest")

        logger.verbose("a log")

        sink.logs shouldHaveSingleElement LogMessage(
            level = Verbose,
            name = "loggerTest",
            content = "a log",
            cause = null
        )
    }

    @Test
    fun `logs with lazy logging are captured`() {
        val logger = AmplifyLogging.logger("name")

        logger.verbose { "a lazy log" }

        sink.logs shouldHaveSingleElement LogMessage(
            level = Verbose,
            name = "name",
            content = "a lazy log",
            cause = null
        )
    }

    @Test
    fun `logs with throwable are captured`() {
        val logger = AmplifyLogging.logger("test")
        val error = Exception("Just a test")

        logger.error("An error occurred", error)

        sink.logs shouldHaveSingleElement LogMessage(
            level = Error,
            name = "test",
            content = "An error occurred",
            cause = error
        )
    }

    @Test
    fun `logs with lazy logging and throwable are captured`() {
        val logger = AmplifyLogging.logger("name")
        val error = Exception("Just a test")

        logger.verbose(error) { "a lazy log" }

        sink.logs shouldHaveSingleElement LogMessage(
            level = Verbose,
            name = "name",
            content = "a lazy log",
            cause = error
        )
    }

    @Test
    fun `lazy messages are only evaluated once for multiple sinks`() {
        var invocations = 0

        val otherSink = TestSink()
        withAddedSink(otherSink) {
            val logger = AmplifyLogging.logger("lazy")
            logger.info {
                invocations++
                "message"
            }
            val expected = LogMessage(
                level = Info,
                name = "lazy",
                content = "message",
                cause = null
            )
            sink.logs shouldHaveSingleElement expected
            otherSink.logs shouldHaveSingleElement expected
            invocations shouldBe 1
        }
    }

    @Test
    fun `lazy messages are not invoked when no sinks will emit them`() {
        var invocations = 0
        val otherSink = TestSink(Error)
        sink.threshold = Info
        withAddedSink(otherSink) {
            val logger = AmplifyLogging.logger("lazy")
            logger.verbose {
                invocations++
                "message"
            }
            sink.logs.shouldBeEmpty()
            otherSink.logs.shouldBeEmpty()
            invocations shouldBe 0
        }
    }

    @Test
    fun `multiple sinks all receive logs`() {
        val anotherSink = TestSink()

        withAddedSink(anotherSink) {
            val logger = AmplifyLogging.logger("test")
            logger.warn("A warning")
            logger.info("Another")
            logger.verbose { "One more" }
            sink.logs shouldHaveSize 3
            sink.logs shouldContainExactly anotherSink.logs
        }
    }

    @Test
    fun `sinks added after logger is created do not receive logs`() {
        val anotherSink = TestSink()
        val logger = AmplifyLogging.logger("test")

        withAddedSink(anotherSink) {
            logger.warn("A warning")
            sink.logs shouldHaveSize 1
            anotherSink.logs.shouldBeEmpty()
        }
    }

    private fun withAddedSink(sink: LogSink, func: () -> Unit) {
        try {
            AmplifyLogging.addSink(sink)
            func()
        } finally {
            AmplifyLogging.removeSink(sink)
        }
    }

    // LogSink that just records messages in a list
    private class TestSink(var threshold: LogLevel = Verbose) : LogSink {
        val logs = mutableListOf<LogMessage>()

        override fun isEnabledFor(level: LogLevel) = threshold allows level
        override fun emit(message: LogMessage) {
            logs += message
        }
    }
}
