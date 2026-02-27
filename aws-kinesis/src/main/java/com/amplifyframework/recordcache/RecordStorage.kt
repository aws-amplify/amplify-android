package com.amplifyframework.recordcache

import com.amplifyframework.foundation.result.Result

internal abstract class RecordStorage(
    val maxRecords: Int,
    val maxBytes: Long,
    val identifier: String
) {
    abstract suspend fun addRecord(record: RecordInput): Result<Unit, RecordCacheException>
    abstract suspend fun getRecordsByStream(): Result<List<List<Record>>, RecordCacheException>
    abstract suspend fun deleteRecords(ids: List<Long>): Result<Unit, RecordCacheException>
    abstract suspend fun incrementRetryCount(ids: List<Long>): Result<Unit, RecordCacheException>
    abstract suspend fun getCurrentCacheSize(): Result<Int, RecordCacheException>
    abstract suspend fun clearRecords(): Result<ClearCacheData, RecordCacheException>
}
