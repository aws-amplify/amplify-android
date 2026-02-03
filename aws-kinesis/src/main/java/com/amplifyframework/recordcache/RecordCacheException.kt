package com.amplifyframework.recordcache

import com.amplifyframework.AmplifyException

internal open class RecordCacheException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, cause, recoverySuggestion)

internal class RecordCacheStorageException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : RecordCacheException(message, recoverySuggestion, cause)

internal class RecordCacheLimitExceededException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : RecordCacheException(message, recoverySuggestion, cause)

internal class RecordCacheNetworkException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : RecordCacheException(message, recoverySuggestion, cause)
