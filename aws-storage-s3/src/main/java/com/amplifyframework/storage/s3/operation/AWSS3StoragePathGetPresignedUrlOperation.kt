/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.storage.s3.operation

import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.operation.StorageGetUrlOperation
import com.amplifyframework.storage.result.StorageGetUrlResult
import com.amplifyframework.storage.s3.extensions.toS3ServiceKey
import com.amplifyframework.storage.s3.request.AWSS3StoragePathGetPresignedUrlRequest
import com.amplifyframework.storage.s3.service.StorageService
import java.util.concurrent.ExecutorService

/**
 * An operation to retrieve pre-signed object URL from AWS S3.
 */
internal class AWSS3StoragePathGetPresignedUrlOperation(
    private val storageService: StorageService,
    private val executorService: ExecutorService,
    private val authCredentialsProvider: AuthCredentialsProvider,
    private val request: AWSS3StoragePathGetPresignedUrlRequest,
    private val onSuccess: Consumer<StorageGetUrlResult>,
    private val onError: Consumer<StorageException>
) : StorageGetUrlOperation<AWSS3StoragePathGetPresignedUrlRequest>(request) {
    override fun start() {
        executorService.submit {

            val serviceKey = try {
                request.path.toS3ServiceKey(authCredentialsProvider)
            } catch (se: StorageException) {
                onError.accept(se)
                return@submit
            }

            try {
                val url = storageService.getPresignedUrl(
                    serviceKey,
                    request.expires,
                    request.useAccelerateEndpoint
                )
                onSuccess.accept(StorageGetUrlResult.fromUrl(url))
            } catch (exception: Exception) {
                onError.accept(
                    StorageException(
                        "Encountered an issue while generating pre-signed URL",
                        exception,
                        "See included exception for more details and suggestions to fix."
                    )
                )
            }
        }
    }
}
