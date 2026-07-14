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
            "custom" to mapOf(
                "CustomerProfiles" to mapOf(
                    "region" to "us-east-1",
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
            "custom" to mapOf(
                "CustomerProfiles" to mapOf(
                    "region" to "us-west-2",
                    "endpoint" to "https://example.com/"
                )
            )
        )
        val config = ConnectClientConfiguration.fromAmplifyOutputs(outputs)
        config.endpoint shouldBe "https://example.com"
    }

    @Test
    fun `fromAmplifyOutputs throws when custom section missing`() {
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(emptyMap())
        }
        exception.message shouldContain "custom.CustomerProfiles"
    }

    @Test
    fun `fromAmplifyOutputs throws when CustomerProfiles section missing`() {
        val outputs = mapOf("custom" to mapOf("Other" to "value"))
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(outputs)
        }
        exception.message shouldContain "custom.CustomerProfiles"
    }

    @Test
    fun `fromAmplifyOutputs throws when endpoint missing`() {
        val outputs = mapOf(
            "custom" to mapOf(
                "CustomerProfiles" to mapOf(
                    "region" to "us-east-1"
                )
            )
        )
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(outputs)
        }
        exception.message shouldContain "endpoint"
    }

    @Test
    fun `fromAmplifyOutputs throws when region missing`() {
        val outputs = mapOf(
            "custom" to mapOf(
                "CustomerProfiles" to mapOf(
                    "endpoint" to "https://example.com"
                )
            )
        )
        val exception = shouldThrow<ConnectConfigurationException> {
            ConnectClientConfiguration.fromAmplifyOutputs(outputs)
        }
        exception.message shouldContain "region"
    }

    @Test
    fun `blank endpoint throws`() {
        shouldThrow<IllegalArgumentException> {
            ConnectClientConfiguration(endpoint = "", region = "us-east-1")
        }
    }

    @Test
    fun `blank region throws`() {
        shouldThrow<IllegalArgumentException> {
            ConnectClientConfiguration(endpoint = "https://x.com", region = "")
        }
    }
}
