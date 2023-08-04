/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth

import aws.smithy.kotlin.runtime.time.toJvmInstant
import com.amplifyframework.auth.AWSCredentials.Factory.createAWSCredentials
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertTrue
import org.junit.Test

class AWSCredentialsFactoryTest {

    @Test
    fun `factory returns null when either accessKeyId or secretAccessKey is null`() {
        val noAccessKey = createAWSCredentials(null, "secret", "ST", 12L)
        val noSecretKey = createAWSCredentials(null, "secret", "ST", 12L)

        assertTrue(listOfNotNull(noAccessKey, noSecretKey).isEmpty())
    }

    @Test
    fun `factory creates AWSCredentials object when either session or expiry is null`() {
        val credentials = createAWSCredentials("accessKey", "secret", null, null)
        val credentialNoSession = createAWSCredentials("accessKey", "secret", null, 12L)
        val credentialNoExpiry = createAWSCredentials("accessKey", "secret", "ST", null)

        listOf(credentials, credentialNoSession, credentialNoExpiry).forEach {
            assertIs<AWSCredentials>(it)
            assertIsNot<AWSTemporaryCredentials>(it)
        }
    }

    @Test
    fun `factory creates AWSTemporaryCredentials object when both session or expiry is not null`() {
        val expectedExpirationInSeconds = Instant.now().epochSecond
        val credentials = createAWSCredentials("accessKey", "secret", "ST", expectedExpirationInSeconds)

        assertIs<AWSTemporaryCredentials>(credentials)
        //assertEquals(expectedExpirationInSeconds, credentials.expiresAt.toJvmInstant().epochSecond)
        assertEquals(expectedExpirationInSeconds, credentials.expiresAt.epochSecond)
    }
}
