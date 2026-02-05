package com.amplifyframework.recordcache

import kotlin.mapCatching

internal class RecordClient<E : Exception>(
    private val sender: RecordSender,
    private val storage: RecordStorage,
    private val exceptionMapper: (RecordCacheException) -> E
) {
    suspend fun record(record: RecordInput): RecordResult = mapErrorResult(
        storage.addRecord(record).mapCatching { RecordData() }
    )

    suspend fun flush(): FlushResult = mapErrorResult(
        runCatching {
            val r = storage.getRecordsByStream()
                .getOrThrow()
                .map { records ->
                    val streamName = records.first().streamName
                    val result = sender.putRecords(streamName, records).getOrThrow()

                    val deleteSuccessful = storage.deleteRecords(result.successfulIds)
                    val deleteFailed = storage.deleteRecords(result.failedIds)
                    val incrementRetry = storage.incrementRetryCount(result.retryableIds)

                    // Ensure all updates are triggered before checking for exceptions
                    deleteSuccessful.getOrThrow()
                    deleteFailed.getOrThrow()
                    incrementRetry.getOrThrow()
                    
                    result.successfulIds
                }
                .map { it.size }.sum()
            FlushData(r)
        }
    )

    suspend fun clearCache(): ClearCacheResult = mapErrorResult(storage.clearRecords())

    fun <T> mapErrorResult(result: Result<T>): Result<T> = if (result.isSuccess) {
        result
    } else {
        val exc = result.exceptionOrNull()
        Result.failure(
            exceptionMapper(
                exc as? RecordCacheException
                    ?: // TODO: What to pass here?
                    RecordCacheException("", "", exc)
            )
        )
    }
}
