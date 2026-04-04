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
package com.amplifyframework.firehose

import com.amplifyframework.foundation.exceptions.AmplifyException
import com.amplifyframework.foundation.exceptions.DEFAULT_RECOVERY_SUGGESTION
import com.amplifyframework.recordcache.RecordCacheDatabaseException
import com.amplifyframework.recordcache.RecordCacheException
import com.amplifyframework.recordcache.RecordCacheLimitExceededException
import com.amplifyframework.recordcache.RecordCacheValidationException

/**
 * Base exception for all Firehose operations.
 *
 * This is a sealed hierarchy. Callers can exhaustively match on the subtype
 * to determine the category of failure:
 * - [AmplifyFirehoseStorageException] — local cache / database errors
 * - [AmplifyFirehoseLimitExceededException] — local cache is full
 * - [AmplifyFirehoseValidationException] — record input validation failed
 * - [AmplifyFirehoseUnknownException] — unexpected / uncategorized errors
 */
sealed class AmplifyFirehoseException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyException(message, recoverySuggestion, cause) {
    companion object {
        internal fun from(error: Throwable): AmplifyFirehoseException = when (error) {
            is AmplifyFirehoseException -> error
            is RecordCacheValidationException -> AmplifyFirehoseValidationException(
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is RecordCacheDatabaseException -> AmplifyFirehoseStorageException(
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is RecordCacheLimitExceededException -> AmplifyFirehoseLimitExceededException(
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            is RecordCacheException -> AmplifyFirehoseStorageException(
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
            else -> AmplifyFirehoseUnknownException(
                message = error.message ?: "An unknown error occurred",
                recoverySuggestion = DEFAULT_RECOVERY_SUGGESTION,
                cause = error
            )
        }
    }
}

/** Local storage / database error. */
class AmplifyFirehoseStorageException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyFirehoseException(message, recoverySuggestion, cause)

/** Local cache size or record limit exceeded. */
class AmplifyFirehoseLimitExceededException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyFirehoseException(message, recoverySuggestion, cause)

/** Unexpected / uncategorized error. */
class AmplifyFirehoseUnknownException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyFirehoseException(message, recoverySuggestion, cause)

/** Record input validation failed (e.g. oversized record). */
class AmplifyFirehoseValidationException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyFirehoseException(message, recoverySuggestion, cause)
