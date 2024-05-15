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

package com.amplifyframework.logging

import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.mockk
import org.junit.Test

class LoggerTest {
    @Test
    fun `verbose log emitted`() {
        val logger = FakeLogger.instance(LogLevel.VERBOSE)
        logger.verbose { "test" }
        logger.logs.first().assertEquals(LogLevel.VERBOSE, "test")
    }

    @Test
    fun `verbose log not emitted`() {
        val logger = FakeLogger.instance(LogLevel.INFO)
        logger.verbose { "test" }
        logger.logs.shouldBeEmpty()
    }

    @Test
    fun `debug log emitted`() {
        val logger = FakeLogger.instance(LogLevel.DEBUG)
        logger.debug { "test" }
        logger.logs.first().assertEquals(LogLevel.DEBUG, "test")
    }

    @Test
    fun `debug log not emitted`() {
        val logger = FakeLogger.instance(LogLevel.INFO)
        logger.debug { "test" }
        logger.logs.shouldBeEmpty()
    }

    @Test
    fun `info log emitted`() {
        val logger = FakeLogger.instance(LogLevel.INFO)
        logger.info { "test" }
        logger.logs.first().assertEquals(LogLevel.INFO, "test")
    }

    @Test
    fun `info log not emitted`() {
        val logger = FakeLogger.instance(LogLevel.WARN)
        logger.info { "test" }
        logger.logs.shouldBeEmpty()
    }

    @Test
    fun `warn log emitted`() {
        val logger = FakeLogger.instance(LogLevel.WARN)
        logger.warn { "test" }
        logger.logs.first().assertEquals(LogLevel.WARN, "test")
    }

    @Test
    fun `warn log not emitted`() {
        val logger = FakeLogger.instance(LogLevel.ERROR)
        logger.warn { "test" }
        logger.logs.shouldBeEmpty()
    }

    @Test
    fun `warn log emitted with throwable`() {
        val throwable = mockk<Throwable>()
        val logger = FakeLogger.instance(LogLevel.WARN)
        logger.warn(throwable) { "test" }
        logger.logs.first().assertEquals(LogLevel.WARN, "test", throwable)
    }

    @Test
    fun `warn log not emitted with throwable`() {
        val throwable = mockk<Throwable>()
        val logger = FakeLogger.instance(LogLevel.ERROR)
        logger.warn(throwable) { "test" }
        logger.logs.shouldBeEmpty()
    }

    @Test
    fun `error log emitted`() {
        val logger = FakeLogger.instance(LogLevel.ERROR)
        logger.error { "test" }
        logger.logs.first().assertEquals(LogLevel.ERROR, "test")
    }

    @Test
    fun `error log not emitted`() {
        val logger = FakeLogger.instance(LogLevel.NONE)
        logger.error { "test" }
        logger.logs.shouldBeEmpty()
    }

    @Test
    fun `error log emitted with throwable`() {
        val throwable = mockk<Throwable>()
        val logger = FakeLogger.instance(LogLevel.ERROR)
        logger.error(throwable) { "test" }
        logger.logs.first().assertEquals(LogLevel.ERROR, "test", throwable)
    }

    @Test
    fun `error log not emitted with throwable`() {
        val throwable = mockk<Throwable>()
        val logger = FakeLogger.instance(LogLevel.NONE)
        logger.error(throwable) { "test" }
        logger.logs.shouldBeEmpty()
    }
}
