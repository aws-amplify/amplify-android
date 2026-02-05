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

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.time.Instant as SmithyInstant
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
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

        val mapped: Credentials = awsCredentials.toSmithyCredentials()

        mapped.accessKeyId shouldBe "access"
        mapped.secretAccessKey shouldBe "secret"
        mapped.sessionToken.shouldBeNull()
        mapped.expiration.shouldBeNull()
    }

    @Test
    fun `maps temporary credentials`() {
        val awsCredentials = AwsCredentials.Temporary(
            accessKeyId = "access",
            secretAccessKey = "secret",
            sessionToken = "session",
            expiration = expiration
        )

        val mapped: Credentials = awsCredentials.toSmithyCredentials()

        mapped.accessKeyId shouldBe "access"
        mapped.secretAccessKey shouldBe "secret"
        mapped.sessionToken shouldBe "session"
        mapped.expiration shouldBe SmithyInstant.fromEpochSeconds(expiration.epochSeconds)
    }

    @Test
    fun `provider maps static credentials`() = runTest {
        val staticProvider = AwsCredentialsProvider<AwsCredentials> {
            AwsCredentials.Static(accessKeyId = "access", secretAccessKey = "secret")
        }
        val mappedProvider = staticProvider.toSmithyProvider()
        val credentials: Credentials = mappedProvider.resolve(emptyAttributes())

        credentials.accessKeyId shouldBe "access"
        credentials.secretAccessKey shouldBe "secret"
        credentials.sessionToken.shouldBeNull()
        credentials.expiration.shouldBeNull()
    }

    @Test
    fun `provider maps temporary credentials`() = runTest {
        val temporaryProvider = AwsCredentialsProvider<AwsCredentials> {
            AwsCredentials.Temporary(
                accessKeyId = "access",
                secretAccessKey = "secret",
                sessionToken = "session",
                expiration = expiration
            )
        }
        val mappedProvider = temporaryProvider.toSmithyProvider()
        val credentials: Credentials = mappedProvider.resolve(emptyAttributes())

        credentials.accessKeyId shouldBe "access"
        credentials.secretAccessKey shouldBe "secret"
        credentials.sessionToken shouldBe "session"
        credentials.expiration shouldBe SmithyInstant.fromEpochSeconds(expiration.epochSeconds)
    }
}
