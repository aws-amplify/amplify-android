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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test

class ConnectClientConfigurationTest {

    @Test
    fun `fromAmplifyOutputs parses valid config`() {
        val outputs = mapOf(
            "notifications" to mapOf(
                "amazon_connect_customer_profiles" to mapOf(
                    "aws_region" to "us-east-1",
                    "endpoint" to "https://abc123.execute-api.us-east-1.amazonaws.com"
                )
            )
        )
        val config = ConnectClientConfiguration.fromAmplifyOutputs(outputs)
        config.endpoint shouldBe "https://abc123.execute-api.us-east-1.amazonaws.com"
        config.region shouldBe "us-east-1"
    }

    @Test
    fun `fromAmplifyOutputs trims trailing slash`() {
        val outputs = mapOf(
            "notifications" to mapOf(
                "amazon_connect_customer_profiles" to mapOf(
                    "aws_region" to "us-west-2",
                    "endpoint" to "https://example.com/"
                )
            )
        )
        val config = ConnectClientConfiguration.fromAmplifyOutputs(outputs)
        config.endpoint shouldBe "https://example.com"
    }

    @Test
    fun `fromAmplifyOutputs throws when notifications section missing`() {
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(emptyMap())
        }
        exception.message shouldContain "notifications.amazon_connect_customer_profiles"
    }

    @Test
    fun `fromAmplifyOutputs throws when amazon_connect_customer_profiles section missing`() {
        val outputs = mapOf("notifications" to mapOf("other" to "value"))
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(outputs)
        }
        exception.message shouldContain "notifications.amazon_connect_customer_profiles"
    }

    @Test
    fun `fromAmplifyOutputs throws when endpoint missing`() {
        val outputs = mapOf(
            "notifications" to mapOf(
                "amazon_connect_customer_profiles" to mapOf(
                    "aws_region" to "us-east-1"
                )
            )
        )
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(outputs)
        }
        exception.message shouldContain "endpoint"
    }

    @Test
    fun `fromAmplifyOutputs throws when aws_region missing`() {
        val outputs = mapOf(
            "notifications" to mapOf(
                "amazon_connect_customer_profiles" to mapOf(
                    "endpoint" to "https://example.com"
                )
            )
        )
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(outputs)
        }
        exception.message shouldContain "aws_region"
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
    fun `fromAmplifyOutputs rejects http endpoint`() {
        val outputs = mapOf(
            "notifications" to mapOf(
                "amazon_connect_customer_profiles" to mapOf(
                    "aws_region" to "us-east-1",
                    "endpoint" to "http://insecure.com"
                )
            )
        )
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(outputs)
        }
        exception.message shouldContain "https"
    }

    @Test
    fun `blank region throws`() {
        shouldThrow<IllegalArgumentException> {
            ConnectClientConfiguration(endpoint = "https://x.com", region = "")
        }
    }
}
