package com.amplifyframework.recordcache

/**
 * Result data for record operations.
 *
 * @param success Whether the record operation succeeded
 */
data class RecordData(val success: Boolean = true)

/**
 * Result data for flush operations.
 *
 * @param recordsFlushed Number of records successfully flushed
 */
data class FlushData(val recordsFlushed: Int = 0)

/**
 * Result data for cache clearing operations.
 *
 * @param recordsCleared Number of records cleared from cache
 */
data class ClearCacheData(val recordsCleared: Int = 0)

typealias RecordResult = Result<RecordData>
typealias FlushResult = Result<FlushData>
typealias ClearCacheResult = Result<ClearCacheData>
