package com.amplifyframework.recordcache

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
data class ClearCacheData(val recordsCleared: Int = 0)

typealias RecordResult = Result<RecordData>
typealias FlushResult = Result<FlushData>
typealias ClearCacheResult = Result<ClearCacheData>
