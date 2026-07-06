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
import org.junit.Test

class ConnectClientConfigurationTest {

    @Test
    fun `valid configuration is created successfully`() {
        val config = ConnectClientConfiguration(
            domainName = "my-domain",
            region = "us-west-2"
        )

        config.domainName shouldBe "my-domain"
        config.region shouldBe "us-west-2"
    }

    @Test
    fun `blank domainName throws`() {
        shouldThrow<IllegalArgumentException> {
            ConnectClientConfiguration(domainName = "", region = "us-east-1")
        }
    }

    @Test
    fun `blank region throws`() {
        shouldThrow<IllegalArgumentException> {
            ConnectClientConfiguration(domainName = "domain", region = "")
        }
    }

    @Test
    fun `whitespace-only domainName throws`() {
        shouldThrow<IllegalArgumentException> {
            ConnectClientConfiguration(domainName = "   ", region = "us-east-1")
        }
    }
}
