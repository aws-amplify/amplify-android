package com.amplifyframework.kinesis

import aws.sdk.kotlin.services.kinesis.model.KinesisException as SdkKinesisException
import com.amplifyframework.foundation.exceptions.AmplifyException
import com.amplifyframework.recordcache.DEFAULT_RECOVERY_SUGGESTION
import com.amplifyframework.recordcache.RecordCacheDatabaseException
import com.amplifyframework.recordcache.RecordCacheException
import com.amplifyframework.recordcache.RecordCacheLimitExceededException

/**
 * Base exception for all Kinesis operations.
 *
 * This is a sealed hierarchy. Callers can exhaustively match on the subtype
 * to determine the category of failure:
 * - [AmplifyKinesisStorageException] — local cache / database errors
 * - [AmplifyKinesisLimitExceededException] — local cache is full
 * - [AmplifyKinesisServiceException] — Kinesis API / SDK errors
 * - [AmplifyKinesisUnknownException] — unexpected / uncategorized errors
 *
 * @param message Error message describing what went wrong
 * @param recoverySuggestion Suggested action to resolve the error
 * @param cause Underlying cause of the exception
 */
sealed class AmplifyKinesisException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, recoverySuggestion, cause) {
    companion object {
        /**
         * Maps a [Throwable] into the appropriate [AmplifyKinesisException] subtype,
         * handling [RecordCacheException], Kinesis SDK exceptions, and unknown errors.
         */
        fun from(error: Throwable): AmplifyKinesisException = when (error) {
            is AmplifyKinesisException -> error
            is RecordCacheDatabaseException -> AmplifyKinesisStorageException(
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is RecordCacheLimitExceededException -> AmplifyKinesisLimitExceededException(
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is RecordCacheException -> AmplifyKinesisStorageException(
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is SdkKinesisException -> AmplifyKinesisServiceException(
                message = "A service error occurred",
                recoverySuggestion = DEFAULT_RECOVERY_SUGGESTION,
                cause = error
            )
            else -> AmplifyKinesisUnknownException(
                message = error.message ?: "An unknown error occurred",
                recoverySuggestion = DEFAULT_RECOVERY_SUGGESTION,
                cause = error
            )
        }
    }
}

/** Local storage / database error. */
class AmplifyKinesisStorageException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyKinesisException(message, recoverySuggestion, cause)

/** Kinesis API / SDK error. */
class AmplifyKinesisServiceException(
    message: String,
    recoverySuggestion: String,
    override val cause: SdkKinesisException
) : AmplifyKinesisException(message, recoverySuggestion, cause)

/** Local cache size or record limit exceeded. */
class AmplifyKinesisLimitExceededException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyKinesisException(message, recoverySuggestion, cause)

/** Unexpected / uncategorized error. */
class AmplifyKinesisUnknownException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyKinesisException(message, recoverySuggestion, cause)
