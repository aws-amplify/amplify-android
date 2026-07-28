/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.connect

import com.amplifyframework.core.configuration.AmplifyOutputsData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class ConnectClientConfigurationTest {

    private fun outputs(
        endpoint: String? = "https://abc123.execute-api.us-east-1.amazonaws.com",
        awsRegion: String = "us-east-1",
        includeConnect: Boolean = true,
        includeNotifications: Boolean = true
    ): AmplifyOutputsData {
        val connect = if (includeConnect && endpoint != null) {
            AmplifyOutputsData.Notifications.AmazonConnect(awsRegion = awsRegion, endpoint = endpoint)
        } else {
            null
        }
        val notifications = if (includeNotifications) {
            AmplifyOutputsData.Notifications(
                awsRegion = "us-east-1",
                amazonPinpointAppId = "app-id",
                channels = emptyList(),
                amazonConnect = connect
            )
        } else {
            null
        }
        return AmplifyOutputsData(notifications = notifications)
    }

    @Test
    fun `fromAmplifyOutputs parses valid config`() {
        val config = ConnectClientConfiguration.fromAmplifyOutputs(outputs())
        config.endpoint shouldBe "https://abc123.execute-api.us-east-1.amazonaws.com"
        config.region shouldBe "us-east-1"
    }

    @Test
    fun `fromAmplifyOutputs trims trailing slash`() {
        val config = ConnectClientConfiguration.fromAmplifyOutputs(
            outputs(endpoint = "https://example.com/", awsRegion = "us-west-2")
        )
        config.endpoint shouldBe "https://example.com"
        config.region shouldBe "us-west-2"
    }

    @Test
    fun `fromAmplifyOutputs throws when notifications section missing`() {
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(outputs(includeNotifications = false))
        }
        exception.message shouldContain "notifications.amazon_connect"
    }

    @Test
    fun `fromAmplifyOutputs throws when amazon_connect section missing`() {
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(outputs(includeConnect = false))
        }
        exception.message shouldContain "notifications.amazon_connect"
    }

    @Test
    fun `fromAmplifyOutputs rejects http endpoint`() {
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(outputs(endpoint = "http://insecure.com"))
        }
        exception.message shouldContain "https"
    }

    @Test
    fun `blank endpoint throws`() {
        shouldThrow<IllegalArgumentException> {
            ConnectClientConfiguration(endpoint = "", region = "us-east-1")
        }
    }

    @Test
    fun `http endpoint throws`() {
        shouldThrow<IllegalArgumentException> {
            ConnectClientConfiguration(endpoint = "http://example.com", region = "us-east-1")
        }
    }

    @Test
    fun `https endpoint accepted`() {
        val config = ConnectClientConfiguration(
            endpoint = "https://example.com",
            region = "us-east-1"
        )
        config.endpoint shouldBe "https://example.com"
    }

    @Test
    fun `blank region throws`() {
        shouldThrow<IllegalArgumentException> {
            ConnectClientConfiguration(endpoint = "https://x.com", region = "")
        }
    }
}
