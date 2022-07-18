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
package com.amplifyframework.analytics.pinpoint

import android.content.Context
import android.net.Uri
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import com.amplifyframework.analytics.AnalyticsEvent
import com.amplifyframework.analytics.pinpoint.database.PinpointDatabase
import com.amplifyframework.analytics.pinpoint.models.PinpointEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class EventRecorder(
    val context: Context,
    val pinpointClient: PinpointClient,
    private val pinpointDatabase: PinpointDatabase,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    internal suspend fun recordEvent(pinpointEvent: PinpointEvent): Uri {
        return withContext(coroutineDispatcher) {
            pinpointDatabase.saveEvent(pinpointEvent)
        }
    }

    internal suspend fun submitEvents(): List<AnalyticsEvent> {
        TODO()
    }
}
