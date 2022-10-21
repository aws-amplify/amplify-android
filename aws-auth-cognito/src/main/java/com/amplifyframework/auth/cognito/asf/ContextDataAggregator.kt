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

package com.amplifyframework.auth.cognito.asf

import android.content.Context

/**
 * Collect all the user context data.
 */
class ContextDataAggregator(deviceId: String) {
    private var dataCollectors = listOf(
        ApplicationDataCollector(),
        BuildDataCollector(),
        DeviceDataCollector(deviceId)
    )

    /**
     * Collect from all the data collectors and create user context data.
     * @param context Android application context.
     * @return key-value pair of the collected user context data.
     */
    fun getAggregatedData(context: Context): Map<String, String?> =
        dataCollectors
            .flatMap { it.collect(context).entries }
            .associate { it.key to it.value }
            .filterValues { !it.isNullOrEmpty() }
}
