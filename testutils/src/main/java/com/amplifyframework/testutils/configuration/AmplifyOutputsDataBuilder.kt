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

package com.amplifyframework.testutils.configuration

import com.amplifyframework.core.configuration.AmplifyOutputsData
import kotlinx.serialization.json.JsonObject

fun amplifyOutputsData(func: AmplifyOutputsDataBuilder.() -> Unit): AmplifyOutputsData =
    AmplifyOutputsDataBuilder().apply(func)

class AmplifyOutputsDataBuilder : AmplifyOutputsData {
    override var version = "1"
    override var analytics: AmplifyOutputsData.Analytics? = null
    override var auth: AmplifyOutputsData.Auth? = null
    override val data: AmplifyOutputsData.Data? = null
    override val geo: AmplifyOutputsData.Geo? = null
    override val notifications: AmplifyOutputsData.Notifications? = null
    override val storage: AmplifyOutputsData.Storage? = null
    override val custom: JsonObject? = null

    fun analytics(func: AnalyticsBuilder.() -> Unit) {
        analytics = AnalyticsBuilder().apply(func)
    }
}

class AnalyticsBuilder : AmplifyOutputsData.Analytics {
    override var amazonPinpoint: AmplifyOutputsData.Analytics.AmazonPinpoint? = null

    fun amazonPinpoint(func: AmazonPinpointBuilder.() -> Unit) {
        amazonPinpoint = AmazonPinpointBuilder().apply(func)
    }
}

class AmazonPinpointBuilder : AmplifyOutputsData.Analytics.AmazonPinpoint {
    override var awsRegion: String = "us-east-1"
    override var appId: String = "analytics-app-id"
}
