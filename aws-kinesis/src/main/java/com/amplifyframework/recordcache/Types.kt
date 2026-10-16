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

/**
 * Result of a cache flush. Each client maps this to its own public type.
 *
 * @property recordsFlushed The number of records successfully flushed to the remote service.
 * @property flushInProgress `true` if this flush was skipped because another flush was already running.
 */
internal data class CacheFlushData(val recordsFlushed: Int = 0, val flushInProgress: Boolean = false)

/**
 * Result of clearing the cache. Each client maps this to its own public type.
 *
 * @property recordsCleared Number of records removed from the cache.
 */
internal data class CacheClearData(val recordsCleared: Int = 0)
