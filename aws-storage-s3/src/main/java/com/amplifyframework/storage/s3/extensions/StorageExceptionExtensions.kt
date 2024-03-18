package com.amplifyframework.storage.s3.extensions

import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StoragePathValidationException

internal fun StoragePathValidationException.Companion.invalidStoragePathException() = StorageException(
    "Invalid StoragePath provided",
    StoragePathValidationException("Invalid StoragePath provided", "Path must be NonEmpty and start with /"),
    "Path must be NonEmpty and start with /"
)

internal fun StoragePathValidationException.Companion.unsupportedStoragePathException() = StorageException(
    "Unsupported StoragePath provided",
    StoragePathValidationException(
        "Unsupported StoragePath provided",
        "Provided StoragePath not supported by AWS S3 Storage Plugin"
    ),
    "Provided StoragePath not supported by AWS S3 Storage Plugin"
)