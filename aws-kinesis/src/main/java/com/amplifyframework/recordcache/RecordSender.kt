package com.amplifyframework.recordcache

internal data class PutRecordsResponse(
    val successfulIds: List<Long>,
    val retryableIds: List<Long>,
    val failedIds: List<Long>
)

internal interface RecordSender {
    suspend fun putRecords(streamName: String, records: List<Record>): Result<PutRecordsResponse>
}
