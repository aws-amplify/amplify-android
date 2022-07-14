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
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.models.SDKInfo
import com.amplifyframework.core.BuildConfig

/**
 * PinpointManager is the entry point to Pinpoint Analytics and Targeting.
 */
internal class PinpointManager constructor(
    val context: Context,
    private val awsPinpointAnalyticsPluginConfiguration: AWSPinpointAnalyticsPluginConfiguration,
    private val credentialsProvider: CredentialsProvider?
) {

    val analyticsClient: AnalyticsClient
    private val sdkName = "" // TODO: confirm whether to use amplify-android or aws-sdk-android
    init {
        val pinpointClient = PinpointClient {
            credentialsProvider = this@PinpointManager.credentialsProvider
            region = awsPinpointAnalyticsPluginConfiguration.region
        }
        val pinpointDatabase = PinpointDatabase(context)
        val sessionClient = SessionClient(context, null)
        analyticsClient = AnalyticsClient(
            context,
            pinpointClient = pinpointClient,
            sessionClient = sessionClient,
            pinpointDatabase = pinpointDatabase,
            androidAppDetails = AndroidAppDetails(context, awsPinpointAnalyticsPluginConfiguration.appId),
            androidDeviceDetails = AndroidDeviceDetails(getCarrier(context)),
            sdkInfo = SDKInfo(sdkName, BuildConfig.VERSION_NAME),
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
