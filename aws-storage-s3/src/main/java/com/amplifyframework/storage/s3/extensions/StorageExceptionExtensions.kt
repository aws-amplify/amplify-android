/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.storage.s3.extensions

import com.amplifyframework.storage.ProgressStallTimeoutException
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StorageFilePermissionException
import com.amplifyframework.storage.StoragePathValidationException

internal fun StoragePathValidationException.Companion.invalidStoragePathException() = StorageException(
    "Invalid StoragePath provided",
    StoragePathValidationException("Invalid StoragePath provided", "Path must not be empty or start with /"),
    "Path must not be empty or start with /"
)

internal fun StoragePathValidationException.Companion.unsupportedStoragePathException() = StorageException(
    "Unsupported StoragePath provided",
    StoragePathValidationException(
        "Unsupported StoragePath provided",
        "Provided StoragePath not supported by AWS S3 Storage Plugin"
    ),
    "Provided StoragePath not supported by AWS S3 Storage Plugin"
)

internal fun StorageFilePermissionException.Companion.unableToOverwriteFileException() = StorageException(
    "Unable to overwrite this file for download.",
    StorageFilePermissionException(
        "Unable to overwrite this file for download.",
        "Acquire write permission for this file before attempting to overwrite it."
    ),
    "Acquire write permission for this file before attempting to overwrite it."
)

internal fun ProgressStallTimeoutException.Companion.progressStallTimeoutException(
    cause: ProgressStallTimeoutException? = null
) = StorageException(
    "Upload cancelled due to progress stall timeout.",
    cause ?: ProgressStallTimeoutException(
        "Upload cancelled due to progress stall timeout.",
        "Increase the configured progress stall timeout or verify the network conditions, " +
            "then retry the upload."
    ),
    "Increase the configured progress stall timeout or verify the network conditions, " +
        "then retry the upload."
)

/**
 * Wraps [this] into the appropriate [StorageException] for an upload callback.
 *
 * When the underlying failure is a [ProgressStallTimeoutException], the typed stall exception is
 * surfaced verbatim via [ProgressStallTimeoutException.Companion.progressStallTimeoutException] so
 * callers can branch on `storageException.cause is ProgressStallTimeoutException`. All other
 * throwables are wrapped in a generic [StorageException] using [defaultMessage].
 */
internal fun Throwable.toStorageUploadException(defaultMessage: String): StorageException {
    val stall = findProgressStallTimeoutCause()
    if (stall != null) {
        return ProgressStallTimeoutException.progressStallTimeoutException(stall)
    }
    return StorageException(
        defaultMessage,
        this,
        "See attached exception for more information and suggestions"
    )
}

private tailrec fun Throwable.findProgressStallTimeoutCause(): ProgressStallTimeoutException? {
    if (this is ProgressStallTimeoutException) return this
    val next = cause ?: return null
    if (next === this) return null
    return next.findProgressStallTimeoutCause()
}
