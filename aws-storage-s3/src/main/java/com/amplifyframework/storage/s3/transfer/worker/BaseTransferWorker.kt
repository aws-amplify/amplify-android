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

package com.amplifyframework.storage.s3.transfer.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.RequestPayer
import aws.sdk.kotlin.services.s3.model.ServerSideEncryption
import aws.sdk.kotlin.services.s3.model.StorageClass
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.fromFile
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.storage.ObjectMetadata
import com.amplifyframework.storage.s3.transfer.ProgressListener
import com.amplifyframework.storage.s3.transfer.TransferRecord
import java.io.File

/**
 * Base worker to perform transfer file task.
 */
internal interface BaseTransferWorker {

    companion object {
        internal const val PART_RECORD_ID = "PART_RECORD_ID"
        internal const val RUN_AS_FOREGROUND_TASK = "RUN_AS_FOREGROUND_TASK"
        internal const val WORKER_ID = "WORKER_ID"
        private val CANNED_ACL_MAP =
            ObjectCannedAcl.values().associateBy { it.value }
        internal const val MULTI_PART_UPLOAD_ID = "multipartUploadId"
        internal const val TRANSFER_RECORD_ID = "TRANSFER_RECORD_ID"
        internal const val OUTPUT_TRANSFER_RECORD_ID = "OUTPUT_TRANSFER_RECORD_ID"
        internal const val completionRequestTag: String = "COMPLETION_REQUEST_TAG_%s"
        internal const val initiationRequestTag: String = "INITIATION_REQUEST_TAG_%s"
        internal const val MULTIPART_UPLOAD: String = "MULTIPART_UPLOAD"
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }

    fun createPutObjectRequest(
        transferRecord: TransferRecord,
        progressListener: ProgressListener?
    ): PutObjectRequest {
        val file = File(transferRecord.file)
        return PutObjectRequest {
            bucket = transferRecord.bucketName
            key = transferRecord.key
            body = ByteStream.fromFile(file)
            cacheControl = transferRecord.headerCacheControl
            contentDisposition = transferRecord.headerContentDisposition
            serverSideEncryption = transferRecord.sseAlgorithm?.let {
                ServerSideEncryption.fromValue(it)
            }
            sseCustomerKey = transferRecord.sseKMSKey
            contentEncoding = transferRecord.headerContentEncoding
            contentType = transferRecord.headerContentType
            expires = transferRecord.httpExpires?.let { Instant.fromEpochSeconds(it) }
            metadata = transferRecord.userMetadata
            contentMd5 = transferRecord.md5
            storageClass = transferRecord.headerStorageClass?.let { StorageClass.fromValue(it) }
            websiteRedirectLocation = transferRecord.userMetadata?.get(
                ObjectMetadata.REDIRECT_LOCATION
            )
            acl = transferRecord.cannedAcl?.let { CANNED_ACL_MAP[it] }
            requestPayer = transferRecord.userMetadata?.get(ObjectMetadata.REQUESTER_PAYS_HEADER)
                ?.let { RequestPayer.fromValue(it) }
            tagging = transferRecord.userMetadata?.get(ObjectMetadata.S3_TAGGING)
        }
    }
}
