package com.amplifyframework.storage

import java.util.Date
import java.util.TreeMap

/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
data class ObjectMetadata @JvmOverloads constructor(
    var userMetadata: Map<String, String> = TreeMap(String.CASE_INSENSITIVE_ORDER),
    var metaData: Map<String, Any> = TreeMap(String.CASE_INSENSITIVE_ORDER),
    var httpExpiresDate: Date? = null,
    var expirationTime: Date? = null,
    var expirationTimeRuleId: String? = null,
    var ongoingRestore: Boolean? = false,
    var restoreExpirationTime: Date? = null,
) {
    companion object {
        const val CONTENT_TYPE = "Content-Type"
        const val CONTENT_ENCODING = "Content-Encoding"
        const val CACHE_CONTROL = "Cache-Control"
        const val CONTENT_MD5 = "Content-MD5"
        const val CONTENT_DISPOSITION = "Content-Disposition"
        const val SERVER_SIDE_ENCRYPTION = "x-amz-server-side-encryption"
        const val STORAGE_CLASS = "x-amz-storage-class"
        const val SERVER_SIDE_ENCRYPTION_KMS_KEY_ID = "x-amz-server-side-encryption-aws-kms-key-id"
        const val REQUESTER_PAYS_HEADER = "x-amz-request-payer"
        const val REDIRECT_LOCATION = "x-amz-website-redirect-location"
        const val S3_TAGGING = "x-amz-tagging"
    }
}
