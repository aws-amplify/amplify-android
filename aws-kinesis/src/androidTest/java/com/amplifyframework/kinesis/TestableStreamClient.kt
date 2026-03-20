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

import com.amplifyframework.foundation.result.Result
import com.amplifyframework.recordcache.ClearCacheData
import com.amplifyframework.recordcache.FlushData
import com.amplifyframework.recordcache.RecordData

/**
 * Thin test-only abstraction over [AmplifyKinesisClient] and
 * [com.amplifyframework.firehose.AmplifyFirehoseClient] so shared
 * instrumentation tests can be written once.
 *
 * The only API difference between the two clients is that Kinesis
 * requires a `partitionKey` on `record()` while Firehose does not.
 * This interface normalises that by dropping `partitionKey` — the
 * Kinesis adapter supplies a sensible default internally.
 */
interface TestableStreamClient {
    suspend fun record(data: ByteArray, streamName: String): Result<RecordData, *>
    suspend fun flush(): Result<FlushData, *>
    suspend fun clearCache(): Result<ClearCacheData, *>
    fun enable()
    fun disable()
}

/** Wraps [AmplifyKinesisClient] with a default partition key. */
fun AmplifyKinesisClient.asTestable(defaultPartitionKey: String = "test-partition"): TestableStreamClient =
    object : TestableStreamClient {
        override suspend fun record(data: ByteArray, streamName: String) =
            this@asTestable.record(data, defaultPartitionKey, streamName)
        override suspend fun flush() = this@asTestable.flush()
        override suspend fun clearCache() = this@asTestable.clearCache()
        override fun enable() = this@asTestable.enable()
        override fun disable() = this@asTestable.disable()
    }
