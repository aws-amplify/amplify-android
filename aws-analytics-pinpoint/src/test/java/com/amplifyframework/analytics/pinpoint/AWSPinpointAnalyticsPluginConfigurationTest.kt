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

package com.amplifyframework.analytics.pinpoint

import com.amplifyframework.testutils.configuration.amplifyOutputsData
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.Test

class AWSPinpointAnalyticsPluginConfigurationTest {

    @Test
    fun `reads values from AmplifyOutputsData and Options`() {
        val outputs = amplifyOutputsData {
            analytics {
                amazonPinpoint {
                    awsRegion = "test-region"
                    appId = "test-app"
                }
            }
        }
        val options = AWSPinpointAnalyticsPlugin.Options {
            autoFlushEventsInterval = 42
            trackLifecycleEvents = false
        }

        val configuration = AWSPinpointAnalyticsPluginConfiguration.from(outputs, options)

        configuration.appId shouldBe "test-app"
        configuration.region shouldBe "test-region"
        configuration.autoFlushEventsInterval shouldBe 42
        configuration.isTrackAppLifecycleEvents shouldBe false
    }

    @Test
    fun `default auto flush interval is 30 seconds`() {
        val configuration = AWSPinpointAnalyticsPluginConfiguration.builder().build()
        configuration.autoFlushEventsInterval shouldBe 30_000
    }

    @Test
    fun `default track lifecycle events is true`() {
        val configuration = AWSPinpointAnalyticsPluginConfiguration.builder().build()
        configuration.isTrackAppLifecycleEvents.shouldBeTrue()
    }
}
