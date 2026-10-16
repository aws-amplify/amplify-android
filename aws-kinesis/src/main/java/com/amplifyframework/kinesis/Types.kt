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
import com.amplifyframework.recordcache.CacheClearData
import com.amplifyframework.recordcache.CacheFlushData

/**
 * Result data for [AmplifyKinesisClient.record].
 *
 * @param success Whether the record operation succeeded
 */
data class RecordData(val success: Boolean = true)

/**
 * Result data for [AmplifyKinesisClient.flush].
 *
 * @property recordsFlushed The number of records successfully flushed to Kinesis Data Streams.
 * @property flushInProgress `true` if this flush was skipped because another flush is already
 *   in progress. When `true`, [recordsFlushed] will always be `0`. The skipped records will
 *   be picked up by the next scheduled flush cycle.
 */
data class FlushData(val recordsFlushed: Int = 0, val flushInProgress: Boolean = false)

/**
 * Result data for [AmplifyKinesisClient.clearCache].
 *
 * @param recordsCleared Number of records cleared from cache
 */
data class ClearCacheData(val recordsCleared: Int = 0)

/** Result of [AmplifyKinesisClient.record]. */
typealias RecordResult = Result<RecordData, AmplifyKinesisException>

/** Result of [AmplifyKinesisClient.flush]. */
typealias FlushResult = Result<FlushData, AmplifyKinesisException>

/** Result of [AmplifyKinesisClient.clearCache]. */
typealias ClearCacheResult = Result<ClearCacheData, AmplifyKinesisException>

internal fun CacheFlushData.toFlushData() = FlushData(
    recordsFlushed = recordsFlushed,
    flushInProgress = flushInProgress
)

internal fun CacheClearData.toClearCacheData() = ClearCacheData(recordsCleared = recordsCleared)
