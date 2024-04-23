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

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.Test

class OptionsTest {
    @Test
    fun `default auto flush interval is the same as in the configuration class`() {
        val options = AWSPinpointAnalyticsPlugin.Options.defaults()
        val configuration = AWSPinpointAnalyticsPluginConfiguration.builder().build()

        options.autoFlushEventsInterval shouldBe configuration.autoFlushEventsInterval
    }

    @Test
    fun `default track lifecycle events is true`() {
        val options = AWSPinpointAnalyticsPlugin.Options.defaults()
        options.trackLifecycleEvents.shouldBeTrue()
    }
}
