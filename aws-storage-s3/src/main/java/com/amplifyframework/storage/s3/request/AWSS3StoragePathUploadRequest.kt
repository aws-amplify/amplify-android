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
package com.amplifyframework.storage.s3.request

import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.s3.ServerSideEncryption

/**
 * Parameters to provide to S3 that describe a request to upload a
 * file or input stream.
 */
internal data class AWSS3StoragePathUploadRequest<L>(
    val path: StoragePath,
    val local: L,
    val contentType: String?,
    val serverSideEncryption: ServerSideEncryption,
    val metadata: Map<String, String>,
    val useAccelerateEndpoint: Boolean
)
