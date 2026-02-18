package com.amplifyframework.kinesis

import com.amplifyframework.AmplifyException
import com.amplifyframework.recordcache.DEFAULT_RECOVERY_SUGGESTION
import com.amplifyframework.recordcache.RecordCacheDatabaseException
import com.amplifyframework.recordcache.RecordCacheException
import com.amplifyframework.recordcache.RecordCacheLimitExceededException

/**
 * Base exception for all Kinesis operations.
 *
 * This is a sealed hierarchy. Callers can exhaustively match on the subtype
 * to determine the category of failure:
 * - [KinesisStorageException] — local cache / database errors
 * - [KinesisLimitExceededException] — local cache is full
 * - [KinesisServiceException] — Kinesis API / SDK errors
 *
 * @param message Error message describing what went wrong
 * @param recoverySuggestion Suggested action to resolve the error
 * @param cause Underlying cause of the exception
 */
sealed class KinesisException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, cause, recoverySuggestion) {
    companion object {
        /**
         * Maps a [Throwable] into the appropriate [KinesisException] subtype,
         * handling [RecordCacheException], Kinesis SDK exceptions, and unknown errors.
         */
        fun from(error: Throwable): KinesisException = when (error) {
            is KinesisException -> error
            is RecordCacheDatabaseException -> KinesisStorageException(
                message = error.message ?: "A database error occurred",
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is RecordCacheLimitExceededException -> KinesisLimitExceededException(
                message = error.message ?: "Cache limit exceeded",
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is RecordCacheException -> KinesisStorageException(
                message = error.message ?: "A cache error occurred",
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            else -> KinesisServiceException(
                message = error.message ?: "A service error occurred",
                recoverySuggestion = DEFAULT_RECOVERY_SUGGESTION,
                cause = error
            )
        }
    }
}

/** Local storage / database error. */
class KinesisStorageException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : KinesisException(message, recoverySuggestion, cause)

/** Kinesis API / SDK error. */
class KinesisServiceException(
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
