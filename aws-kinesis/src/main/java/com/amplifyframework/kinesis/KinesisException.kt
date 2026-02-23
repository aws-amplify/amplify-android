package com.amplifyframework.kinesis

import aws.sdk.kotlin.services.kinesis.model.KinesisException as SdkKinesisException
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
 * - [KinesisUnknownException] — unexpected / uncategorized errors
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
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is RecordCacheLimitExceededException -> KinesisLimitExceededException(
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is RecordCacheException -> KinesisStorageException(
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is SdkKinesisException -> KinesisServiceException(
                message = "A service error occurred",
                recoverySuggestion = DEFAULT_RECOVERY_SUGGESTION,
                cause = error
            )
            else -> KinesisUnknownException(
                message = error.message ?: "An unknown error occurred",
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
    override val cause: SdkKinesisException
) : KinesisException(message, recoverySuggestion, cause)

/** Local cache size or record limit exceeded. */
class KinesisLimitExceededException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : KinesisException(message, recoverySuggestion, cause)

/** Unexpected / uncategorized error. */
class KinesisUnknownException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : KinesisException(message, recoverySuggestion, cause)
