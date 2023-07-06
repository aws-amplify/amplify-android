/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.store

import com.amplifyframework.annotations.InternalApiWarning
import java.util.concurrent.ConcurrentHashMap

@InternalApiWarning
class InMemoryKeyValueRepository : KeyValueRepository {
    private val cache = ConcurrentHashMap<String, String?>()

    override fun put(dataKey: String, value: String?) {
        value?.run { cache.put(dataKey, value) }
    }

    override fun get(dataKey: String): String? = cache.getOrDefault(dataKey, null)

    override fun remove(dataKey: String) {
        cache.remove(dataKey)
    }

    override fun removeAll() {
        cache.clear()
    }
}
