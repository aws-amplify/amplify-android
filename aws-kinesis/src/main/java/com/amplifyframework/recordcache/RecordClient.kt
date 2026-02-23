package com.amplifyframework.recordcache

import java.util.concurrent.atomic.AtomicBoolean

internal class RecordClient(
    private val sender: RecordSender,
    private val storage: RecordStorage
) {
    private val isFlushing = AtomicBoolean(false)
    suspend fun record(record: RecordInput): RecordResult = storage.addRecord(record).mapCatching { RecordData() }

    suspend fun flush(): FlushResult {
        // Guard against concurrent flushes to return early
        if (!isFlushing.compareAndSet(false, true)) {
            return Result.success(FlushData(recordsFlushed = 0, flushInProgress = true))
        }
        return try {
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
        } finally {
            isFlushing.set(false)
        }
    }

    suspend fun clearCache(): ClearCacheResult = storage.clearRecords()
}
