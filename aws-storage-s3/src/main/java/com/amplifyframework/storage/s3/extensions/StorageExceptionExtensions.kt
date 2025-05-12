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
