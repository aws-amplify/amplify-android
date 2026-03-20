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
package com.amplifyframework.firehose

import com.amplifyframework.kinesis.TestableStreamClient

/** Wraps [AmplifyFirehoseClient] as a [TestableStreamClient]. Direct mapping — no partition key needed. */
fun AmplifyFirehoseClient.asTestable(): TestableStreamClient =
    object : TestableStreamClient {
        override suspend fun record(data: ByteArray, streamName: String) =
            this@asTestable.record(data, streamName)
        override suspend fun flush() = this@asTestable.flush()
        override suspend fun clearCache() = this@asTestable.clearCache()
        override fun enable() = this@asTestable.enable()
        override fun disable() = this@asTestable.disable()
    }
