package com.amplifyframework.recordcache

/**
 * Internal error type used by [RecordClient] / [RecordStorage].
 * Mapped to the public Kinesis exception type at the KinesisDataStreams boundary.
 */
internal sealed class RecordCacheException(
    override val message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    val recoverySuggestion: String = recoverySuggestion
}

/** Database operation failed. */
internal class RecordCacheDatabaseException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : RecordCacheException(message, recoverySuggestion, cause)

/** Cache limit exceeded — no space for new records. */
internal class RecordCacheLimitExceededException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : RecordCacheException(message, recoverySuggestion, cause)

/** Default recovery suggestion for errors. */
internal const val DEFAULT_RECOVERY_SUGGESTION = "Inspect the underlying error for more details."
