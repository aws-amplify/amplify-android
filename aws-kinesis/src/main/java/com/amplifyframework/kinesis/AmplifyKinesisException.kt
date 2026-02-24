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
 * @param cause Underlying cause of the exception
 * @param recoverySuggestion Suggested action to resolve the error
 */
sealed class AmplifyKinesisException(
    message: String,
    cause: Throwable? = null,
    recoverySuggestion: String
) : AmplifyException(message, cause, recoverySuggestion) {
    companion object {
        /**
         * Maps a [Throwable] into the appropriate [AmplifyKinesisException] subtype,
         * handling [RecordCacheException], Kinesis SDK exceptions, and unknown errors.
         */
        fun from(error: Throwable): AmplifyKinesisException = when (error) {
            is AmplifyKinesisException -> error
            is RecordCacheDatabaseException -> AmplifyKinesisStorageException(
                message = error.message,
                cause = error,
                recoverySuggestion = error.recoverySuggestion
            )
            is RecordCacheLimitExceededException -> AmplifyKinesisLimitExceededException(
                message = error.message,
                cause = error,
                recoverySuggestion = error.recoverySuggestion
            )
            is RecordCacheException -> AmplifyKinesisStorageException(
                message = error.message,
                cause = error,
                recoverySuggestion = error.recoverySuggestion
            )
            is SdkKinesisException -> AmplifyKinesisServiceException(
                message = "A service error occurred",
                cause = error,
                recoverySuggestion = DEFAULT_RECOVERY_SUGGESTION
            )
            else -> AmplifyKinesisUnknownException(
                message = error.message ?: "An unknown error occurred",
                cause = error,
                recoverySuggestion = DEFAULT_RECOVERY_SUGGESTION
            )
        }
    }
}

/** Local storage / database error. */
class AmplifyKinesisStorageException(
    message: String,
    cause: Throwable? = null,
    recoverySuggestion: String
) : AmplifyKinesisException(message, cause, recoverySuggestion)

/** Kinesis API / SDK error. */
class AmplifyKinesisServiceException(
    message: String,
    override val cause: SdkKinesisException,
    recoverySuggestion: String
) : AmplifyKinesisException(message, cause, recoverySuggestion)

/** Local cache size or record limit exceeded. */
class AmplifyKinesisLimitExceededException(
    message: String,
    cause: Throwable? = null,
    recoverySuggestion: String
) : AmplifyKinesisException(message, cause, recoverySuggestion)

/** Unexpected / uncategorized error. */
class AmplifyKinesisUnknownException(
    message: String,
    cause: Throwable? = null,
    recoverySuggestion: String
) : AmplifyKinesisException(message, cause, recoverySuggestion)
