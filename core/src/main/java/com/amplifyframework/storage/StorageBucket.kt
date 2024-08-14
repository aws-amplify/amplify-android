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
package com.amplifyframework.storage

abstract class StorageBucket {
    companion object {
        @JvmStatic
        fun fromOutputs(name: String): StorageBucket = OutputsStorageBucket(name)
        @JvmStatic
        fun fromBucketInfo(bucketInfo: BucketInfo): StorageBucket = ResolvedStorageBucket(bucketInfo)
    }
}

// While class may be technically public, the constructor is internal,
// Customer is never expected to see or attempt to use this extended class
data class OutputsStorageBucket internal constructor(val name: String) : StorageBucket()

// While class may be technically public, the constructor is internal,
// Customer is never expected to see or attempt to use this extended class
data class ResolvedStorageBucket internal constructor(val bucketInfo: BucketInfo) : StorageBucket()
