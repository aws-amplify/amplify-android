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
package com.amplifyframework.analytics.pinpoint

import android.content.Context
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.core.store.EncryptedKeyValueRepository
import com.amplifyframework.pinpoint.core.AnalyticsClient
import com.amplifyframework.pinpoint.core.TargetingClient
import com.amplifyframework.pinpoint.core.data.AndroidAppDetails
import com.amplifyframework.pinpoint.core.data.AndroidDeviceDetails
import com.amplifyframework.pinpoint.core.database.PinpointDatabase
import com.amplifyframework.pinpoint.core.util.getUniqueId

/**
 * PinpointManager is the entry point to Pinpoint Analytics and Targeting.
 */
internal class PinpointManager constructor(
    context: Context,
    private val awsPinpointConfiguration: AWSPinpointAnalyticsPluginConfiguration,
    private val credentialsProvider: CredentialsProvider?
) {
    val analyticsClient: AnalyticsClient
    val targetingClient: TargetingClient
    internal val pinpointClient: PinpointClient = PinpointClient {
        credentialsProvider = this@PinpointManager.credentialsProvider
        region = awsPinpointConfiguration.region
    }

    companion object {
        /*
        Auth plugin needs to read from Pinpoint shared preferences, but we don't currently have an architecture
        that allows the plugins to pass data between each other. If the storage mechanism of UniqueId changes, we
        must also refactor AuthEnvironment from the Cognito Auth Plugin.
         */
        private const val PINPOINT_SHARED_PREFS_SUFFIX = "515d6767-01b7-49e5-8273-c8d11b0f331d"
    }

    init {
        val pinpointDatabase = PinpointDatabase(context)

        /*
        Auth plugin needs to read from Pinpoint shared preferences, but we don't currently have an architecture
        that allows the plugins to pass data between each other. If the storage mechanism of UniqueId changes, we
        must also refactor AuthEnvironment from the Cognito Auth Plugin.
         */
        val sharedPrefs = context.getSharedPreferences(
            "${awsPinpointConfiguration.appId}$PINPOINT_SHARED_PREFS_SUFFIX",
            Context.MODE_PRIVATE
        )

        val encryptedStore = EncryptedKeyValueRepository(
            context,
            "${awsPinpointConfiguration.appId}$PINPOINT_SHARED_PREFS_SUFFIX"
        )

        val androidAppDetails = AndroidAppDetails(context, awsPinpointConfiguration.appId)
        val androidDeviceDetails = AndroidDeviceDetails(context)
        targetingClient = TargetingClient(
            context,
            pinpointClient,
            encryptedStore,
            sharedPrefs,
            androidAppDetails,
            androidDeviceDetails,
        )
        analyticsClient = AnalyticsClient(
            context,
            awsPinpointConfiguration.autoFlushEventsInterval,
            pinpointClient,
            targetingClient,
            pinpointDatabase,
            sharedPrefs.getUniqueId(),
            androidAppDetails,
            androidDeviceDetails,
        )
    }
}
