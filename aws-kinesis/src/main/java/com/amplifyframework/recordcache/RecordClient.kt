package com.amplifyframework.recordcache

import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.foundation.result.mapSuccess
import java.util.concurrent.atomic.AtomicBoolean

internal class RecordClient(
    private val sender: RecordSender,
    private val storage: RecordStorage
) {
    private val logger: Logger = AmplifyLogging.logger<RecordClient>()
    private val isFlushing = AtomicBoolean(false)

    suspend fun record(record: RecordInput): Result<RecordData, Throwable> =
        storage.addRecord(record) mapSuccess { RecordData() }

    suspend fun flush(): Result<FlushData, Throwable> {
        // Guard against concurrent flushes to return early
        if (!isFlushing.compareAndSet(false, true)) {
            logger.debug { "Flush already in progress, skipping" }
            return Result.Success(FlushData(recordsFlushed = 0, flushInProgress = true))
        }
        return try {
            val recordsByStream = storage.getRecordsByStream().getOrThrow()
            logger.debug { "Retrieved ${recordsByStream.size} stream(s) with records to flush" }

            val totalFlushed = recordsByStream
                .map { records ->
                    val streamName = records.first().streamName
                    val recordCount = records.size
                    logger.verbose { "Flushing $recordCount records to stream: $streamName" }

                    val result = sender.putRecords(streamName, records).getOrThrow()

                    val deleteSuccessful = storage.deleteRecords(result.successfulIds)
                    val deleteFailed = storage.deleteRecords(result.failedIds)
                    val incrementRetry = storage.incrementRetryCount(result.retryableIds)

                    // Ensure all updates are triggered before checking for exceptions
                    deleteSuccessful.getOrThrow()
                    deleteFailed.getOrThrow()
                    incrementRetry.getOrThrow()

                    logger.verbose {
                        "Stream $streamName: ${result.successfulIds.size} succeeded, " +
                            "${result.retryableIds.size} retryable, ${result.failedIds.size} failed"
                    }

                    result.successfulIds
                }
                .map { it.size }.sum()

            Result.Success(FlushData(totalFlushed))
        } catch (e: Throwable) {
            Result.Failure(e)
        } finally {
            isFlushing.set(false)
        }
    }

    suspend fun clearCache(): Result<ClearCacheData, Throwable> = storage.clearRecords()
}
