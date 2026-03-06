package com.amplifyframework.recordcache

import com.amplifyframework.foundation.result.Result

internal abstract class RecordStorage(
    val maxRecords: Int,
    val cacheMaxBytes: Long,
    val identifier: String,
    val maxRecordSizeBytes: Long,
    val maxBytesPerStream: Long,
    val maxPartitionKeyLength: Int
) {
    abstract suspend fun addRecord(record: RecordInput): Result<Unit, RecordCacheException>
    abstract suspend fun getRecordsByStream(excludingIds: Set<Long>): Result<List<List<Record>>, RecordCacheException>
    abstract suspend fun deleteRecords(ids: List<Long>): Result<Unit, RecordCacheException>
    abstract suspend fun incrementRetryCount(ids: List<Long>): Result<Unit, RecordCacheException>
    abstract suspend fun getCurrentCacheSize(): Result<Int, RecordCacheException>
    abstract suspend fun clearRecords(): Result<ClearCacheData, RecordCacheException>
}
