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
package com.amplifyframework.kinesis

import com.amplifyframework.recordcache.CacheClearData
import com.amplifyframework.recordcache.CacheFlushData
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class KinesisTypesMappingTest {

    @Test
    fun `toFlushData should copy records flushed`() {
        val result = CacheFlushData(recordsFlushed = 7).toFlushData()
        result shouldBe FlushData(recordsFlushed = 7, flushInProgress = false)
    }

    @Test
    fun `toFlushData should copy flush in progress`() {
        val result = CacheFlushData(recordsFlushed = 0, flushInProgress = true).toFlushData()
        result shouldBe FlushData(recordsFlushed = 0, flushInProgress = true)
    }

    @Test
    fun `toClearCacheData should copy records cleared`() {
        val result = CacheClearData(recordsCleared = 3).toClearCacheData()
        result shouldBe ClearCacheData(recordsCleared = 3)
    }

    @Test
    fun `Interval flush strategy should default to 30 seconds`() {
        FlushStrategy.Interval().interval shouldBe 30.seconds
    }

    @Test
    fun `options should default to a 30 second interval flush strategy`() {
        AmplifyKinesisClientOptions.defaults().flushStrategy shouldBe FlushStrategy.Interval(30.seconds)
    }
}
