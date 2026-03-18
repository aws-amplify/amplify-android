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
package com.amplifyframework.kinesis

import com.amplifyframework.foundation.exceptions.AmplifyException
import com.amplifyframework.foundation.exceptions.DEFAULT_RECOVERY_SUGGESTION
import com.amplifyframework.recordcache.RecordCacheDatabaseException
import com.amplifyframework.recordcache.RecordCacheException
import com.amplifyframework.recordcache.RecordCacheLimitExceededException
import com.amplifyframework.recordcache.RecordCacheValidationException

/**
 * Base exception for all Kinesis operations.
 *
 * This is a sealed hierarchy. Callers can exhaustively match on the subtype
 * to determine the category of failure:
 * - [AmplifyKinesisStorageException] — local cache / database errors
 * - [AmplifyKinesisLimitExceededException] — local cache is full
 * - [AmplifyKinesisValidationException] — record input validation failed
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
         * handling [RecordCacheException] and unknown errors.
         */
        internal fun from(error: Throwable): AmplifyKinesisException = when (error) {
            is AmplifyKinesisException -> error
            is RecordCacheValidationException -> AmplifyKinesisValidationException(
                message = error.message,
                recoverySuggestion = error.recoverySuggestion,
                cause = error
            )
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

/** Record input validation failed (e.g. oversized record, invalid partition key). */
class AmplifyKinesisValidationException(
    message: String,
    recoverySuggestion: String,
    cause: Throwable? = null
) : AmplifyKinesisException(message, recoverySuggestion, cause)
