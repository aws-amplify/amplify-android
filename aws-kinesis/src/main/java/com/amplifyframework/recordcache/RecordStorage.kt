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

import com.amplifyframework.foundation.result.Result

internal abstract class RecordStorage(
    val maxRecords: Int,
    val cacheMaxBytes: Long,
    val identifier: String,
    val maxRecordSizeBytes: Long,
    val maxBytesPerStream: Long,
    val maxPartitionKeyLength: Int
) {
    abstract suspend fun addRecord(record: RecordInput): Result<Unit, RecordCacheException>
    abstract suspend fun getRecordsByStream(
        afterIdByStream: Map<String, Long> = emptyMap()
    ): Result<List<List<Record>>, RecordCacheException>
    abstract suspend fun deleteRecords(ids: List<Long>): Result<Unit, RecordCacheException>
    abstract suspend fun incrementRetryCount(ids: List<Long>): Result<Unit, RecordCacheException>
    abstract suspend fun getCurrentCacheSize(): Result<Int, RecordCacheException>
    abstract suspend fun clearRecords(): Result<ClearCacheData, RecordCacheException>
}
