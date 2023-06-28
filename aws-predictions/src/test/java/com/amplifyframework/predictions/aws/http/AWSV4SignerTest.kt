/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.predictions.aws.http

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.time.Instant
import java.net.URI
import java.sql.Date
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class AWSV4SignerTest {

    private lateinit var signer: AWSV4Signer

    @Before
    fun setUp() {
        signer = AWSV4Signer()
    }

    @Test
    fun `get signed uri with session token`() {
        val expectedUrl = "wss://streaming-rekognition.us-east-1.amazon.com/start-face-liveness-session-websocket" +
            "?X-Amz-Algorithm=AWS4-HMAC-SHA256" +
            "&X-Amz-Credential=accessKeyIdTest%252F19700120%252Fus-east-1%252Frekognition%252Faws4_request" +
            "&X-Amz-Date=19700120T104017Z" +
            "&X-Amz-Expires=299" +
            "&X-Amz-Security-Token=sessionTokenTest" +
            "&X-Amz-SignedHeaders=host" +
            "&tK=tV" +
            "&x-amz-user-agent=userAgentTest" +
            "&X-Amz-Signature=f9a94d627c77aac5c9e1ecf850231dda2fc6edc7294c3506f07b43f7d2bb4ad2"

        val uri = URI(
            "wss://streaming-rekognition.us-east-1.amazon.com/start-face-liveness-session-websocket?tK=tV"
        )
        val credentials = Credentials(
            accessKeyId = "accessKeyIdTest",
            secretAccessKey = "secretAccessKeyTest",
            sessionToken = "sessionTokenTest",
            expiration = Instant.fromIso8601("2023-03-28T15:28:29+0000"),
            providerName = "providerNameTest"
        )
        val dateMillis = 1680017309L
        val signedUri = signer.getSignedUri(uri, credentials, "us-east-1", "userAgentTest", dateMillis)

        assertEquals(expectedUrl, signedUri.toString())
    }

    @Test
    fun `get signed uri without session token`() {
        val expectedUrl = "wss://streaming-rekognition.us-east-1.amazon.com/start-face-liveness-session-websocket" +
            "?X-Amz-Algorithm=AWS4-HMAC-SHA256" +
            "&X-Amz-Credential=accessKeyIdTest%252F19700120%252Fus-east-1%252Frekognition%252Faws4_request" +
            "&X-Amz-Date=19700120T104017Z" +
            "&X-Amz-Expires=299" +
            "&X-Amz-SignedHeaders=host" +
            "&tK=tV" +
            "&x-amz-user-agent=userAgentTest" +
            "&X-Amz-Signature=8bf221f80e5f69908ce8e3be0f03ad7cf96c1d329795218ca7ba5322056628ef"

        val uri = URI(
            "wss://streaming-rekognition.us-east-1.amazon.com/start-face-liveness-session-websocket?tK=tV"
        )
        val credentials = Credentials(
            accessKeyId = "accessKeyIdTest",
            secretAccessKey = "secretAccessKeyTest",
            expiration = Instant.fromIso8601("2023-03-28T15:28:29+0000"),
            providerName = "providerNameTest"
        )
        val dateMillis = 1680017309L
        val signedUri = signer.getSignedUri(uri, credentials, "us-east-1", "userAgentTest", dateMillis)

        assertEquals(expectedUrl, signedUri.toString())
    }

    @Test
    @Config(qualifiers = "ar")
    fun `get signed uri for different locale`() {
        signer = AWSV4Signer()
        val expectedUrl = "wss://streaming-rekognition.us-east-1.amazon.com/start-face-liveness-session-websocket" +
            "?X-Amz-Algorithm=AWS4-HMAC-SHA256" +
            "&X-Amz-Credential=accessKeyIdTest%252F19700120%252Fus-east-1%252Frekognition%252Faws4_request" +
            "&X-Amz-Date=19700120T104017Z" +
            "&X-Amz-Expires=299" +
            "&X-Amz-SignedHeaders=host" +
            "&tK=tV" +
            "&x-amz-user-agent=userAgentTest" +
            "&X-Amz-Signature=8bf221f80e5f69908ce8e3be0f03ad7cf96c1d329795218ca7ba5322056628ef"

        val uri = URI(
            "wss://streaming-rekognition.us-east-1.amazon.com/start-face-liveness-session-websocket?tK=tV"
        )
        val credentials = Credentials(
            accessKeyId = "accessKeyIdTest",
            secretAccessKey = "secretAccessKeyTest",
            expiration = Instant.fromIso8601("2023-03-28T15:28:29+0000"),
            providerName = "providerNameTest"
        )
        val dateMillis = 1680017309L
        val signedUri = signer.getSignedUri(uri, credentials, "us-east-1", "userAgentTest", dateMillis)

        assertEquals(expectedUrl, signedUri.toString())
    }

    @Test
    fun `get signed frame returns expected and stores previous signature`() {
        val expectedFirstFrame = "1415d07838d82f035231c87641b2477c2fa4f00e9813a04ee95d45368d6b1051"
        val expectedSecondFrame = "31df24081f0088143236e16a1230b0690a29bedf8c7f9a7dbf3f211d176baf73"

        val firstFrame = signer.getSignedFrame(
            "us-east-1-test",
            "testFrame1".toByteArray(),
            "sKey1",
            Pair(":date", Date(1680017309L))
        )
        val secondFrame = signer.getSignedFrame(
            "us-east-1-test",
            "testFrame2".toByteArray(),
            "sKey2",
            Pair(":date", Date(1680018309L))
        )

        assertEquals(expectedFirstFrame, firstFrame)
        assertEquals(expectedSecondFrame, secondFrame)
    }

    @Test
    @Config(qualifiers = "ar")
    fun `get signed frame for different locale returns expected and stores previous signature`() {
        signer = AWSV4Signer()
        val expectedFirstFrame = "1415d07838d82f035231c87641b2477c2fa4f00e9813a04ee95d45368d6b1051"
        val expectedSecondFrame = "31df24081f0088143236e16a1230b0690a29bedf8c7f9a7dbf3f211d176baf73"

        val firstFrame = signer.getSignedFrame(
            "us-east-1-test",
            "testFrame1".toByteArray(),
            "sKey1",
            Pair(":date", Date(1680017309L))
        )
        val secondFrame = signer.getSignedFrame(
            "us-east-1-test",
            "testFrame2".toByteArray(),
            "sKey2",
            Pair(":date", Date(1680018309L))
        )

        assertEquals(expectedFirstFrame, firstFrame)
        assertEquals(expectedSecondFrame, secondFrame)
    }

    @Test
    fun `get signed uri with special characters in user agent`() {
        val expectedUrl = "wss://streaming-rekognition.us-east-1.amazon.com/start-face-liveness-session-websocket" +
            "?X-Amz-Algorithm=AWS4-HMAC-SHA256" +
            "&X-Amz-Credential=accessKeyIdTest%252F19700120%252Fus-east-1%252Frekognition%252Faws4_request" +
            "&X-Amz-Date=19700120T104017Z" +
            "&X-Amz-Expires=299" +
            "&X-Amz-SignedHeaders=host" +
            "&tK=tV" +
            "&x-amz-user-agent=userAgent%2528Test%2529" +
            "&X-Amz-Signature=5bf3eddb52fe25a5035438737320ef6fdec816258800ec7c5dd44a322dce0c48"

        val uri = URI(
            "wss://streaming-rekognition.us-east-1.amazon.com/start-face-liveness-session-websocket?tK=tV"
        )
        val credentials = Credentials(
            accessKeyId = "accessKeyIdTest",
            secretAccessKey = "secretAccessKeyTest",
            expiration = Instant.fromIso8601("2023-03-28T15:28:29+0000"),
            providerName = "providerNameTest"
        )
        val dateMillis = 1680017309L
        val signedUri = signer.getSignedUri(uri, credentials, "us-east-1", "userAgent(Test)", dateMillis)

        assertEquals(expectedUrl, signedUri.toString())
    }
}
