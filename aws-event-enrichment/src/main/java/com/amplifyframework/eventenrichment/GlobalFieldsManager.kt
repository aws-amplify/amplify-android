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
package com.amplifyframework.eventenrichment

import java.util.concurrent.ConcurrentHashMap

/**
 * Manages global attributes and metrics that are stamped on every event.
 *
 * Values are in-memory only and not persisted between sessions.
 */
class GlobalFieldsManager {
    private val attributesMap = ConcurrentHashMap<String, String>()
    private val metricsMap = ConcurrentHashMap<String, Double>()

    /** Current global attributes (snapshot copy). */
    val attributes: Map<String, String>
        get() = attributesMap.toMap()

    /** Current global metrics (snapshot copy). */
    val metrics: Map<String, Double>
        get() = metricsMap.toMap()

    /** Adds a global attribute stamped on every subsequent event. */
    fun addAttribute(key: String, value: String) {
        attributesMap[key] = value
    }

    /** Removes a global attribute by key. */
    fun removeAttribute(key: String) {
        attributesMap.remove(key)
    }

    /** Adds a global metric stamped on every subsequent event. */
    fun addMetric(key: String, value: Double) {
        metricsMap[key] = value
    }

    /** Removes a global metric by key. */
    fun removeMetric(key: String) {
        metricsMap.remove(key)
    }
}
