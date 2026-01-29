package com.amplifyframework.recordcache

data class PutRecordsResponse(
    val successfulIds: List<Long>,
    val retryableIds: List<Long>,
    val failedIds: List<Long>
)

interface RecordSender {
    suspend fun putRecords(streamName: String, records: List<Record>): Result<PutRecordsResponse>
}
