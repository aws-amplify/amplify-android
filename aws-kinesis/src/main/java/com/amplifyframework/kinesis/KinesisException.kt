package com.amplifyframework.kinesis

import com.amplifyframework.AmplifyException
import com.amplifyframework.recordcache.RecordCacheException
import com.amplifyframework.recordcache.RecordCacheLimitExceededException
import com.amplifyframework.recordcache.RecordCacheNetworkException
import com.amplifyframework.recordcache.RecordCacheStorageException

/**
 * Base exception for all Kinesis operations.
 *
 * This is a sealed hierarchy. Callers can exhaustively match on the subtype
 * to determine the category of failure:
 * - [KinesisStorageException] - local cache / database errors
 * - [KinesisNetworkException] - API / connectivity errors
 * - [KinesisLimitExceededException] - local cache is full
 *
 * @param message Error message describing what went wrong
 * @param recoverySuggestion Suggested action to resolve the error
 * @param cause Underlying cause of the exception
 */
sealed class KinesisException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, cause, recoverySuggestion)

/** Local storage / database error. */
class KinesisStorageException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : KinesisException(message, recoverySuggestion, cause)

/** Network or Kinesis API error. */
class KinesisNetworkException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : KinesisException(message, recoverySuggestion, cause)

/** Local cache size or record limit exceeded. */
class KinesisLimitExceededException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : KinesisException(message, recoverySuggestion, cause)

internal fun RecordCacheException.toKinesisException(): KinesisException = when (this) {
    is RecordCacheStorageException -> KinesisStorageException(
        message = this.message ?: "Kinesis storage operation failed",
        recoverySuggestion = this.recoverySuggestion,
        cause = this
    )
    is RecordCacheNetworkException -> KinesisNetworkException(
        message = this.message ?: "Kinesis network operation failed",
        recoverySuggestion = this.recoverySuggestion,
        cause = this
    )
    is RecordCacheLimitExceededException -> KinesisLimitExceededException(
        message = this.message ?: "Kinesis cache limit exceeded",
        recoverySuggestion = this.recoverySuggestion,
        cause = this
    )
    else -> KinesisNetworkException(
        message = this.message ?: "Kinesis operation failed",
        recoverySuggestion = this.recoverySuggestion,
        cause = this
    )
}
