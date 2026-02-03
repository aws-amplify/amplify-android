package com.amplifyframework.recordcache

import com.amplifyframework.AmplifyException

open class RecordCacheException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, cause, recoverySuggestion)

class RecordCacheStorageException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : RecordCacheException(message, recoverySuggestion, cause)

class RecordCacheLimitExceededException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : RecordCacheException(message, recoverySuggestion, cause)

class RecordCacheNetworkException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : RecordCacheException(message, recoverySuggestion, cause)
