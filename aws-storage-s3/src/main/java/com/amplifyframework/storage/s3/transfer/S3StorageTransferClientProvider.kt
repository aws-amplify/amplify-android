/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.storage.s3.transfer

import aws.sdk.kotlin.services.s3.S3Client
import com.amplifyframework.auth.AuthCredentialsProvider

internal class S3StorageTransferClientProvider(
    private val createS3Client: (region: String?, bucketName: String?) -> S3Client
) : StorageTransferClientProvider {
    companion object {
        @JvmStatic
        fun getS3Client(region: String, authCredentialsProvider: AuthCredentialsProvider): S3Client {
            return S3Client {
                this.region = region
                this.credentialsProvider = authCredentialsProvider
            }
        }
    }
    override fun getStorageTransferClient(region: String?, bucketName: String?): S3Client {
        return createS3Client(region, bucketName)
    }
}
