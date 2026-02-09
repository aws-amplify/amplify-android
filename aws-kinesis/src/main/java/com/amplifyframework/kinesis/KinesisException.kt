package com.amplifyframework.kinesis

import com.amplifyframework.AmplifyException
import com.amplifyframework.recordcache.RecordCacheException

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
