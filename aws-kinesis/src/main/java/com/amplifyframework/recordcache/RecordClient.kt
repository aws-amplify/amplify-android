package com.amplifyframework.recordcache

import aws.sdk.kotlin.services.kinesis.model.KinesisException as SdkKinesisException
import com.amplifyframework.foundation.logging.AmplifyLogging
import com.amplifyframework.foundation.logging.Logger
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.foundation.result.mapSuccess
import java.util.concurrent.atomic.AtomicBoolean

internal class RecordClient(
    private val sender: RecordSender,
    private val storage: RecordStorage,
    private val maxRetries: Int
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
            var totalFlushed = 0
            val lastIdByStream = mutableMapOf<String, Long>()

            var recordsByStream = storage.getRecordsByStream(lastIdByStream).getOrThrow()
            while (recordsByStream.isNotEmpty()) {
                logger.debug { "Retrieved ${recordsByStream.size} stream(s) with records to flush" }

                val batchFlushed = recordsByStream
                    .mapNotNull { records ->
                        val streamName = records.first().streamName
                        val recordCount = records.size
                        logger.verbose { "Flushing $recordCount records to stream: $streamName" }

                        // Track the last record ID per stream so subsequent batches start after it
                        val maxId = records.maxOf { it.id }
                        lastIdByStream[streamName] = maxId

                        try {
                            val result = sender.putRecords(streamName, records).getOrThrow()

                            val deleteSuccessful = storage.deleteRecords(result.successfulIds)
                            val deleteFailed = storage.deleteRecords(result.failedIds)
                            val incrementRetry = storage.incrementRetryCount(result.retryableIds)

                            // Ensure all updates are triggered before checking for exceptions
                            deleteSuccessful.getOrThrow()
                            deleteFailed.getOrThrow()
                            incrementRetry.getOrThrow()

                            logger.debug {
                                "Stream $streamName: ${result.successfulIds.size} succeeded, " +
                                    "${result.retryableIds.size} retryable, ${result.failedIds.size} failed"
                            }

                            result.successfulIds
                        } catch (e: Throwable) {
                            // Increment retry count for all records in the failed request and delete the ones at the limit
                            handleFailedRequest(records)

                            // SDK Kinesis exceptions are logged but not thrown
                            if (e is SdkKinesisException) {
                                logger.warn { "Kinesis SDK error flushing stream $streamName: ${e.message}. Skipping" }
                                null
                            } else {
                                // Network errors, storage errors, and unexpected errors — throw to caller
                                logger.warn { "Error flushing stream $streamName: ${e.message}. Aborting flush" }
                                throw e
                            }
                        }
                    }
                    .sumOf { it.size }

                totalFlushed += batchFlushed
                recordsByStream = storage.getRecordsByStream(lastIdByStream).getOrThrow()
            }

            Result.Success(FlushData(totalFlushed))
        } catch (e: Throwable) {
            Result.Failure(e)
        } finally {
            isFlushing.set(false)
        }
    }

    private suspend fun handleFailedRequest(records: List<Record>) {
        try {
            val (recordsToRetry, recordsToDelete) = records.partition { it.retryCount < maxRetries }
            val recordIdsToIncrement = recordsToRetry.map { it.id }
            val recordIdsToDelete = recordsToDelete.map { it.id }

            storage.incrementRetryCount(recordIdsToIncrement).getOrThrow()
            storage.deleteRecords(recordIdsToDelete).getOrThrow()

            if (recordIdsToDelete.isNotEmpty()) {
                val streamName = records.first().streamName
                logger.warn {
                    "Deleted ${recordIdsToDelete.size} records from stream $streamName " +
                        "that exceeded retry limit of $maxRetries after failed retries"
                }
            }
        } catch (storageError: Throwable) {
            logger.error { "Failed to update records for failed request: ${storageError.message}" }
        }
    }

    suspend fun clearCache(): Result<ClearCacheData, Throwable> = storage.clearRecords()
}
