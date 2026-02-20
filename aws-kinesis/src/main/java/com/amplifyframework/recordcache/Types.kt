package com.amplifyframework.recordcache

import com.amplifyframework.foundation.result.Result as AmplifyResult
import com.amplifyframework.kinesis.AmplifyKinesisException

/**
 * Result data for record operations.
 *
 * @param success Whether the record operation succeeded
 */
data class RecordData(val success: Boolean = true)

/**
 * Result of flushing records.
 *
 * @property recordsFlushed The number of records successfully flushed to the remote service.
 * @property flushInProgress `true` if this flush was skipped because another flush is already
 *   in progress. When `true`, [recordsFlushed] will always be `0`. The skipped records will
 *   be picked up by the next scheduled flush cycle.
 */
data class FlushData(val recordsFlushed: Int = 0, val flushInProgress: Boolean = false)

/**
 * Result data for cache clearing operations.
 *
 * @param recordsCleared Number of records cleared from cache
 */
data class ClearCacheData(val recordsCleared: Int = 0)

typealias RecordResult = AmplifyResult<RecordData, AmplifyKinesisException>
typealias FlushResult = AmplifyResult<FlushData, AmplifyKinesisException>
typealias ClearCacheResult = AmplifyResult<ClearCacheData, AmplifyKinesisException>
