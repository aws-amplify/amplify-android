/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.recordcache

internal data class RecordInput(
    val streamName: String,
    val partitionKey: String? = null,
    val data: ByteArray,
    val dataSize: Int = data.size + (partitionKey?.toByteArray(Charsets.UTF_8)?.size ?: 0)
)

internal data class Record(
    val id: Long,
    val streamName: String,
    val partitionKey: String? = null,
    val data: ByteArray,
    val dataSize: Int,
    val retryCount: Int,
    val createdAt: Long
)
