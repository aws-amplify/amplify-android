package com.amplifyframework.recordcache

abstract class RecordStorage(
    val maxRecords: Int,
    val maxBytes: Long,
    val identifier: String
) {
    abstract suspend fun addRecord(record: RecordInput): Result<Unit>
    abstract suspend fun getRecordsByStream(): Result<List<List<Record>>>
    abstract suspend fun deleteRecords(ids: List<Long>): Result<Unit>
    abstract suspend fun incrementRetryCount(ids: List<Long>): Result<Unit>
    abstract suspend fun getCurrentCacheSize(): Result<Int>
    abstract suspend fun clearRecords(): Result<ClearCacheData>
}
