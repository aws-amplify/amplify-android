/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amplifyframework.recordcache

/**
 * Internal error type used by [RecordClient] / [RecordStorage].
 * Mapped to the public Kinesis exception type at the AmplifyKinesisClient boundary.
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

/** Record input validation failed (e.g. oversized record, invalid partition key). */
internal class RecordCacheValidationException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : RecordCacheException(message, recoverySuggestion, cause)
