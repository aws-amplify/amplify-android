/**
 * Copyright 2015-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amplifyframework.storage.s3.transfer

import android.database.Cursor
import com.amplifyframework.storage.TransferState
import com.amplifyframework.storage.s3.utils.JsonUtils

internal data class TransferRecord(
    var id: Int,
    var transferId: String,
    var mainUploadId: Int = 0,
    var isRequesterPays: Int = 0,
    var isMultipart: Int = 0,
    var isLastPart: Int = 0,
    var isEncrypted: Int = 0,
    var partNumber: Int = 0,
    var bytesTotal: Long = 0,
    var bytesCurrent: Long = 0,
    var speed: Long = 0,
    var rangeStart: Long = 0,
    var rangeLast: Long = 0,
    var fileOffset: Long = 0,
    var type: TransferType? = null,
    var state: TransferState? = null,
    var bucketName: String? = null,
    var key: String? = null,
    var versionId: String? = null,
    var file: String = "",
    var multipartId: String? = null,
    var eTag: String? = null,
    var headerContentType: String? = null,
    var headerContentLanguage: String? = null,
    var headerContentDisposition: String? = null,
    var headerContentEncoding: String? = null,
    var headerCacheControl: String? = null,
    var headerExpire: String? = null,
    var headerStorageClass: String? = null,
    var userMetadata: Map<String, String>? = null,
    var expirationTimeRuleId: String? = null,
    var httpExpires: String? = null,
    var sseAlgorithm: String? = null,
    var sseKMSKey: String? = null,
    var md5: String? = null,
    var cannedAcl: String? = null,
    var workManagerRequestId: String? = null,
    var useAccelerateEndpoint: Int = 0
) {
    companion object {

        const val MINIMUM_UPLOAD_PART_SIZE = 5 * 1024 * 1024
        const val MAXIMUM_UPLOAD_PARTS = 10000

        @JvmStatic
        fun updateFromDB(c: Cursor): TransferRecord {
            val id = c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_ID))
            val transferId = c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_TRANSFER_ID))
            return TransferRecord(id, transferId).apply {
                this.mainUploadId =
                    c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_MAIN_UPLOAD_ID))
                this.type =
                    TransferType.valueOf(
                        c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_TYPE))
                    )
                this.state =
                    TransferState.getState(
                        c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_STATE))
                    )
                this.bucketName =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_BUCKET_NAME))
                this.key = c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_KEY))
                this.versionId =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_VERSION_ID))
                this.bytesTotal =
                    c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_BYTES_TOTAL))
                this.bytesCurrent =
                    c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_BYTES_CURRENT))
                this.speed = c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_SPEED))
                this.isRequesterPays =
                    c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_IS_REQUESTER_PAYS))
                this.isMultipart =
                    c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_IS_MULTIPART))
                this.isLastPart =
                    c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_IS_LAST_PART))
                this.isEncrypted =
                    c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_IS_ENCRYPTED))
                this.partNumber = c.getInt(c.getColumnIndexOrThrow(TransferTable.COLUMN_PART_NUM))
                this.eTag = c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_ETAG))
                this.file = c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_FILE))
                this.multipartId =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_MULTIPART_ID))
                this.rangeStart =
                    c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_DATA_RANGE_START))
                this.rangeLast =
                    c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_DATA_RANGE_LAST))
                this.fileOffset =
                    c.getLong(c.getColumnIndexOrThrow(TransferTable.COLUMN_FILE_OFFSET))
                this.headerContentType =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_CONTENT_TYPE))
                this.headerContentLanguage =
                    c.getString(
                        c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_CONTENT_LANGUAGE)
                    )
                this.headerContentDisposition =
                    c.getString(
                        c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_CONTENT_DISPOSITION)
                    )
                this.headerContentEncoding =
                    c.getString(
                        c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_CONTENT_ENCODING)
                    )
                this.headerCacheControl =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_CACHE_CONTROL))
                this.headerExpire =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_EXPIRE))
                c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_USER_METADATA))?.let {
                    JsonUtils.jsonToMap(it)
                }.also {
                    this.userMetadata = it as? Map<String, String>
                }
                this.expirationTimeRuleId =
                    c.getString(
                        c.getColumnIndexOrThrow(TransferTable.COLUMN_EXPIRATION_TIME_RULE_ID)
                    )
                this.httpExpires =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HTTP_EXPIRES_DATE))
                this.sseAlgorithm =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_SSE_ALGORITHM))
                this.sseKMSKey =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_SSE_KMS_KEY))
                this.md5 = c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_CONTENT_MD5))
                this.cannedAcl =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_CANNED_ACL))
                this.headerStorageClass =
                    c.getString(c.getColumnIndexOrThrow(TransferTable.COLUMN_HEADER_STORAGE_CLASS))
                this.useAccelerateEndpoint = c.getInt(
                    c.getColumnIndexOrThrow(TransferTable.COLUMN_USE_ACCELERATE_ENDPOINT)
                )
            }
        }
    }

    internal fun isMainRecord(): Boolean {
        return isMultipart == 0 || (isMultipart == 1 && mainUploadId == 0)
    }
}
