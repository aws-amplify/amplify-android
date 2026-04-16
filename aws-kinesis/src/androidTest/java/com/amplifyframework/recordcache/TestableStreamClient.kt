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
package com.amplifyframework.recordcache

import com.amplifyframework.foundation.result.Result

/**
 * Thin test-only abstraction over [com.amplifyframework.kinesis.AmplifyKinesisClient] and
 * [com.amplifyframework.firehose.AmplifyFirehoseClient] so shared
 * instrumentation tests can be written once.
 *
 * The only API difference between the two clients is that Kinesis
 * requires a `partitionKey` on `record()` while Firehose does not.
 * This interface normalises that by dropping `partitionKey` — the
 * Kinesis adapter supplies a sensible default internally.
 */
interface TestableStreamClient {
    suspend fun record(data: ByteArray, streamName: String): Result<RecordData, Throwable>
    suspend fun flush(): Result<FlushData, Throwable>
    suspend fun clearCache(): Result<ClearCacheData, Throwable>
    fun enable()
    fun disable()
}
