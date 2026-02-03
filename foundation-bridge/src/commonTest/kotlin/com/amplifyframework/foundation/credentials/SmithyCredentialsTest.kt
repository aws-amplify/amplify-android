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

package com.amplifyframework.foundation.credentials

import aws.smithy.kotlin.runtime.time.Instant as SmithyInstant
import io.kotest.matchers.shouldBe
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.junit.Test

@OptIn(ExperimentalTime::class)
class SmithyCredentialsTest {
    private val expiration = Instant.fromEpochSeconds(1770130997)

    @Test
    fun `maps static credentials`() {
        val awsCredentials = AwsCredentials.Static(
            accessKeyId = "access",
            secretAccessKey = "secret"
        )

        val mapped = awsCredentials.toSmithyCredentials()

        mapped.accessKeyId shouldBe "access"
        mapped.secretAccessKey shouldBe "secret"
    }

    @Test
    fun `maps temporary credentials`() {
        val awsCredentials = AwsCredentials.Temporary(
            accessKeyId = "access",
            secretAccessKey = "secret",
            sessionToken = "session",
            expiration = expiration
        )

        val mapped = awsCredentials.toSmithyCredentials()

        mapped.accessKeyId shouldBe "access"
        mapped.secretAccessKey shouldBe "secret"
        mapped.sessionToken shouldBe "session"
        mapped.expiration shouldBe SmithyInstant.fromEpochSeconds(expiration.epochSeconds)
    }
}
