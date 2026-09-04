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

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.foundation.logging.LogLevel
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

@OptIn(ExperimentalAmplifyApi::class)
class AmplifyCloudWatchClientOptionsTest {

    @Test
    fun `dsl applies defaults with only logGroupName set`() {
        val options = AmplifyCloudWatchClientOptions { logGroupName = "/app/log" }

        options.logGroupName shouldBe "/app/log"
        options.localStoreMaxSizeInMB shouldBe 5
        options.flushStrategy shouldBe FlushStrategy.Interval(60.seconds)
        options.loggingConstraints shouldBe LoggingConstraints()
        options.configureClient shouldBe null
    }

    @Test
    fun `builder sets all values`() {
        val constraints = LoggingConstraints(defaultLogLevel = LogLevel.Warn)

        val options = AmplifyCloudWatchClientOptions.builder()
            .logGroupName("/app/log")
            .localStoreMaxSizeInMB(10)
            .flushStrategy(FlushStrategy.None)
            .loggingConstraints(constraints)
            .build()

        options.logGroupName shouldBe "/app/log"
        options.localStoreMaxSizeInMB shouldBe 10
        options.flushStrategy shouldBe FlushStrategy.None
        options.loggingConstraints shouldBe constraints
    }

    @Test
    fun `build throws when logGroupName is missing`() {
        shouldThrow<IllegalArgumentException> {
            AmplifyCloudWatchClientOptions.builder().build()
        }
    }

    @Test
    fun `build throws when localStoreMaxSizeInMB is not positive`() {
        // A zero or negative value would make cacheSizeInMB * mb <= 0, so isCacheFull is always true
        // and every write triggers a flush.
        shouldThrow<IllegalArgumentException> {
            AmplifyCloudWatchClientOptions.builder().logGroupName("/g").localStoreMaxSizeInMB(0).build()
        }
        shouldThrow<IllegalArgumentException> {
            AmplifyCloudWatchClientOptions.builder().logGroupName("/g").localStoreMaxSizeInMB(-1).build()
        }
    }

    @Test
    fun `dsl supports a custom flush strategy`() {
        val options = AmplifyCloudWatchClientOptions {
            logGroupName = "/g"
            flushStrategy = FlushStrategy.Interval(30.seconds)
        }

        options.flushStrategy shouldBe FlushStrategy.Interval(30.seconds)
    }
}
