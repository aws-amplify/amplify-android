package com.amplifyframework.kinesis

import com.amplifyframework.AmplifyException
import com.amplifyframework.recordcache.RecordCacheException

/**
 * Exception thrown by Kinesis operations.
 *
 * @param message Error message describing what went wrong
 * @param recoverySuggestion Suggested action to resolve the error
 * @param cause Underlying cause of the exception
 */
class KinesisException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, cause, recoverySuggestion)

internal fun RecordCacheException.toKinesisException(): KinesisException = KinesisException(
    message = this.message ?: "Kinesis operation failed",
    recoverySuggestion = this.recoverySuggestion,
    cause = this
)
