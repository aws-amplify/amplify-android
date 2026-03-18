package com.amplifyframework.recordcache

import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.foundation.result.Result
import com.amplifyframework.foundation.result.mapFailure
import com.amplifyframework.foundation.result.resultCatching

internal data class PutRecordsResponse(
    val successfulIds: List<Long>,
    val retryableIds: List<Long>,
    val failedIds: List<Long>
)

internal interface RecordSender {
    suspend fun putRecords(streamName: String, records: List<Record>): Result<PutRecordsResponse, Throwable>
}

/**
 * Exception wrapper indicating a service-level SDK error that should be
 * silently skipped during flush (e.g. stream not found, throttling).
 * Other streams can still be flushed.
 */
internal class SkippableSdkException(
    message: String,
    cause: Throwable
) : Exception(message, cause)

/**
 * Like [com.amplifyframework.foundation.result.resultCatching], but wraps exceptions
 * of type [E] into [SkippableSdkException] so the shared [RecordClient] can skip
 * service-level errors without depending on SDK exception types.
 */
@OptIn(InternalAmplifyApi::class)
internal inline fun <reified E : Exception> resultCatchingSkippable(
    block: () -> PutRecordsResponse
): Result<PutRecordsResponse, Throwable> = resultCatching(block).mapFailure { e ->
    if (e is E) SkippableSdkException(e.message ?: "SDK error", e) else e
}

/**
 * Splits a batch response into success/retry/fail buckets based on per-record error codes.
 *
 * @param errorCodes Error code per record from the SDK response (null = success)
 * @param records The records that were sent, in the same order
 * @param maxRetries Maximum retry attempts before a record is considered failed
 */
internal fun splitResults(
    errorCodes: List<String?>,
    records: List<Record>,
    maxRetries: Int
): PutRecordsResponse {
    val successfulIds = mutableListOf<Long>()
    val retryableIds = mutableListOf<Long>()
    val failedIds = mutableListOf<Long>()
    errorCodes.forEachIndexed { index, errorCode ->
        val recordId = records[index].id
        val retryCount = records[index].retryCount
        if (errorCode == null) {
            successfulIds.add(recordId)
        } else if (retryCount >= maxRetries) {
            failedIds.add(recordId)
        } else {
            retryableIds.add(recordId)
        }
    }
    return PutRecordsResponse(successfulIds, retryableIds, failedIds)
}
