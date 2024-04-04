/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.storage.operation.StorageListOperation
import com.amplifyframework.storage.result.StorageListResult
import com.amplifyframework.storage.s3.extensions.toS3ServiceKey
import com.amplifyframework.storage.s3.request.AWSS3StoragePathListRequest
import com.amplifyframework.storage.s3.service.AWSS3StorageService
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.runBlocking

/**
 * An operation to list items from AWS S3.
 */
internal class AWSS3StoragePathListOperation(
    private val storageService: AWSS3StorageService,
    private val executorService: ExecutorService,
    private val authCredentialsProvider: AuthCredentialsProvider,
    private val request: AWSS3StoragePathListRequest,
    private val onSuccess: Consumer<StorageListResult>,
    private val onError: Consumer<StorageException>
) : StorageListOperation<AWSS3StoragePathListRequest>(request) {
    override fun start() {
        executorService.submit {
            val serviceKey = try {
                runBlocking {
                    request.path.toS3ServiceKey(authCredentialsProvider)
                }
            } catch (se: StorageException) {
                onError.accept(se)
                return@submit
            }

            try {
                onSuccess.accept(
                    storageService.listFiles(serviceKey, request.pageSize, request.nextToken)
                )
            } catch (exception: Exception) {
                onError.accept(
                    StorageException(
                        "Something went wrong with your AWS S3 Storage list operation",
                        exception,
                        "See attached exception for more information and suggestions"
                    )
                )
            }
        }
    }
}
