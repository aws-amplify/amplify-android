/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.storage

import com.amplifyframework.AmplifyException
import com.amplifyframework.annotations.InternalAmplifyApi

/**
 * Exception thrown when the necessary file permissions needed to handle a file have not been granted.
 */
class StorageFilePermissionException @InternalAmplifyApi constructor(
    message: String,
    recoverySuggestion: String
) : AmplifyException(message, recoverySuggestion) {
    @InternalAmplifyApi companion object
}
