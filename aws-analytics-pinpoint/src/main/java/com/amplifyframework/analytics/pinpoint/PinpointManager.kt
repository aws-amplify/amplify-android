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
import android.telephony.TelephonyManager
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.analytics.pinpoint.database.PinpointDatabase
import com.amplifyframework.analytics.pinpoint.internal.core.idresolver.SharedPrefsUniqueIdService
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.models.SDKInfo
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import com.amplifyframework.analytics.pinpoint.targeting.notification.PinpointNotificationClient
import com.amplifyframework.core.BuildConfig

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
    private val targetingClient: TargetingClient
    private val sdkName = "" // TODO: confirm whether to use amplify-android or aws-sdk-android

    init {
        val pinpointClient = PinpointClient {
            credentialsProvider = this@PinpointManager.credentialsProvider
            region = awsPinpointConfiguration.region
        }
        val pinpointDatabase = PinpointDatabase(context)
        sessionClient = SessionClient(context, null)
        val sharedPrefs =
            context.getSharedPreferences(awsPinpointConfiguration.appId, Context.MODE_PRIVATE)
        val androidAppDetails = AndroidAppDetails(context, awsPinpointConfiguration.appId)
        val androidDeviceDetails = AndroidDeviceDetails(getCarrier(context))
        targetingClient = TargetingClient(
            pinpointClient,
            PinpointNotificationClient(),
            SharedPrefsUniqueIdService(sharedPrefs),
            sharedPrefs,
            androidAppDetails,
            androidDeviceDetails,
            context
        )
        analyticsClient = AnalyticsClient(
            context,
            pinpointClient = pinpointClient,
            sessionClient = sessionClient,
            pinpointDatabase = pinpointDatabase,
            androidAppDetails = androidAppDetails,
            androidDeviceDetails = androidDeviceDetails,
            sdkInfo = SDKInfo(sdkName, BuildConfig.VERSION_NAME),
            targetingClient = targetingClient
        )
        sessionClient.setAnalyticsClient(analyticsClient)
        sessionClient.startSession()
    }

    private fun getCarrier(context: Context): String {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return telephony?.let {
            if (it.networkOperatorName.isNullOrBlank()) {
                it.networkOperatorName
            } else {
                "Unknown"
            }
        } ?: "Unknown"
    }
}
