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
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.analytics.pinpoint.database.PinpointDatabase
import com.amplifyframework.analytics.pinpoint.models.SDKInfo
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import com.amplifyframework.analytics.pinpoint.targeting.data.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.targeting.data.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.targeting.util.getUniqueId
import com.amplifyframework.core.BuildConfig
import com.amplifyframework.util.UserAgent.Platform

/**
 * PinpointManager is the entry point to Pinpoint Analytics and Targeting.
 */
internal class PinpointManager constructor(
    val context: Context,
    private val awsPinpointConfiguration: AWSPinpointAnalyticsPluginConfiguration,
    private val credentialsProvider: CredentialsProvider?
) {
    val analyticsClient: AnalyticsClient
    val sessionClient: SessionClient
    val targetingClient: TargetingClient
    internal val pinpointClient: PinpointClient = PinpointClient {
        credentialsProvider = this@PinpointManager.credentialsProvider
        region = awsPinpointConfiguration.region
    }

    companion object {
        private val SDK_NAME = Platform.ANDROID.libraryName
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

        val androidAppDetails = AndroidAppDetails(context, awsPinpointConfiguration.appId)
        val androidDeviceDetails = AndroidDeviceDetails(context)
        targetingClient = TargetingClient(
            context,
            pinpointClient,
            sharedPrefs,
            androidAppDetails,
            androidDeviceDetails,
        )
        sessionClient = SessionClient(context, targetingClient, sharedPrefs.getUniqueId(), analyticsClient = null)
        analyticsClient = AnalyticsClient(
            context,
            pinpointClient,
            sessionClient,
            targetingClient,
            pinpointDatabase,
            sharedPrefs.getUniqueId(),
            androidAppDetails,
            androidDeviceDetails,
            SDKInfo(SDK_NAME, BuildConfig.VERSION_NAME)
        )
        sessionClient.setAnalyticsClient(analyticsClient)
        sessionClient.startSession()
    }
}
