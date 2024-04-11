package com.amplifyframework.analytics.pinpoint

import io.kotest.matchers.shouldBe
import org.junit.Test

class AWSPinpointAnalyticsPluginOptionsTest {
    @Test
    fun `default auto flush interval is the same as in the configuration class`() {
        val options = AWSPinpointAnalyticsPluginOptions.defaults()
        val configuration = AWSPinpointAnalyticsPluginConfiguration.builder().build()

        options.autoFlushEventsInterval shouldBe configuration.autoFlushEventsInterval
    }
}
