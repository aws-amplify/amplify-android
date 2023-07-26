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

import com.amplifyframework.predictions.aws.models.liveness.AccessDeniedException
import com.amplifyframework.predictions.aws.models.liveness.ChallengeConfig
import com.amplifyframework.predictions.aws.models.liveness.ColorSequence
import com.amplifyframework.predictions.aws.models.liveness.DisconnectionEvent
import com.amplifyframework.predictions.aws.models.liveness.FaceMovementAndLightServerChallenge
import com.amplifyframework.predictions.aws.models.liveness.FreshnessColor
import com.amplifyframework.predictions.aws.models.liveness.InternalServerException
import com.amplifyframework.predictions.aws.models.liveness.LightChallengeType
import com.amplifyframework.predictions.aws.models.liveness.LivenessResponseStream
import com.amplifyframework.predictions.aws.models.liveness.OvalParameters
import com.amplifyframework.predictions.aws.models.liveness.ServerChallenge
import com.amplifyframework.predictions.aws.models.liveness.ServerSessionInformationEvent
import com.amplifyframework.predictions.aws.models.liveness.ServiceQuotaExceededException
import com.amplifyframework.predictions.aws.models.liveness.ServiceUnavailableException
import com.amplifyframework.predictions.aws.models.liveness.SessionInformation
import com.amplifyframework.predictions.aws.models.liveness.SessionNotFoundException
import com.amplifyframework.predictions.aws.models.liveness.ThrottlingException
import com.amplifyframework.predictions.aws.models.liveness.ValidationException
import java.util.Arrays
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class LivenessEventStreamTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun `test basic model with string header`() {
        val expectedArray: ByteArray = byteArrayOf(
            // Total Byte Length
            // 84 bytes
            0, 0, 0, 84,

            // Headers Byte Length
            // 16 bytes
            0, 0, 0, 16,

            // Prelude CRC
            -75, 6, -90, 6,

            // Headers
            // Headers - Header Name Byte Length
            9,
            // Headers - Header Name
            // "string_ex"
            115, 116, 114, 105, 110, 103, 95, 101, 120,
            // Headers - Header Value Type
            // 7 == String
            7,
            // Headers - Value String Byte Length
            // 3 bytes
            0, 3,
            // Headers - Value String
            // "abc"
            97, 98, 99,

            // Payload
            123, 34, 102, 111, 111, 34, 58, 52, 50, 44, 34,
            98, 97, 114, 34, 58, 34, 104, 101, 108, 108, 111,
            44, 32, 119, 111, 114, 108, 100, 33, 34, 44, 34,
            98, 97, 122, 34, 58, 123, 34, 113, 117, 117, 120,
            34, 58, 116, 114, 117, 101, 125, 125,

            // Message CRC
            55, 84, 52, 84
        )

        val model = Model(foo = 42, bar = "hello, world!", baz = Baz(quux = true))
        val headers = mapOf(Pair("string_ex", "abc"))
        val data = json.encodeToString(model)
        val encoded = LivenessEventStream.encode(data.toByteArray(), headers)

        assertTrue(Arrays.equals(expectedArray, encoded.array()))
    }

    @Test
    fun `test decoding ServerSessionInformationEvent`() {
        val event = ServerSessionInformationEvent(
            sessionInformation = SessionInformation(
                challenge = ServerChallenge(
                    faceMovementAndLightChallenge = FaceMovementAndLightServerChallenge(
                        ovalParameters = OvalParameters(1.0f, 2.0f, .5f, .7f),
                        lightChallengeType = LightChallengeType.SEQUENTIAL,
                        challengeConfig = ChallengeConfig(1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f),
                        colorSequences = listOf(
                            ColorSequence(FreshnessColor(listOf(0, 1, 2)), 4.0f, 5.0f)
                        )
                    )
                )
            )
        )
        val headers = mapOf(
            ":event-type" to "ServerSessionInformationEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val expectedResponse = LivenessResponseStream(serverSessionInformationEvent = event)

        val data = json.encodeToString(event)
        val encoded = LivenessEventStream.encode(data.toByteArray(), headers)
        val decoded = LivenessEventStream.decode(encoded.array().toByteString(), json)

        assertEquals(expectedResponse, decoded)
    }
    @Test
    fun `test decoding DisconnectionEvent`() {
        val event = DisconnectionEvent(timestampMillis = System.currentTimeMillis())
        val headers = mapOf(
            ":exception-type" to "DisconnectionEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val expectedResponse = LivenessResponseStream(disconnectionEvent = event)

        val data = json.encodeToString(event)
        val encoded = LivenessEventStream.encode(data.toByteArray(), headers)
        val decoded = LivenessEventStream.decode(encoded.array().toByteString(), json)

        assertEquals(expectedResponse, decoded)
    }

    @Test
    fun `test decoding ValidationException`() {
        val event = ValidationException("Validation error")
        val headers = mapOf(
            ":exception-type" to "ValidationException",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val expectedResponse = LivenessResponseStream(validationException = event)

        val data = json.encodeToString(event)
        val encoded = LivenessEventStream.encode(data.toByteArray(), headers)
        val decoded = LivenessEventStream.decode(encoded.array().toByteString(), json)

        assertEquals(expectedResponse, decoded)
    }

    @Test
    fun `test decoding InternalServerException`() {
        val event = InternalServerException("error")
        val headers = mapOf(
            ":exception-type" to "InternalServerException",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val expectedResponse = LivenessResponseStream(internalServerException = event)

        val data = json.encodeToString(event)
        val encoded = LivenessEventStream.encode(data.toByteArray(), headers)
        val decoded = LivenessEventStream.decode(encoded.array().toByteString(), json)

        assertEquals(expectedResponse, decoded)
    }

    @Test
    fun `test decoding ThrottlingException`() {
        val event = ThrottlingException("error")
        val headers = mapOf(
            ":exception-type" to "ThrottlingException",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val expectedResponse = LivenessResponseStream(throttlingException = event)

        val data = json.encodeToString(event)
        val encoded = LivenessEventStream.encode(data.toByteArray(), headers)
        val decoded = LivenessEventStream.decode(encoded.array().toByteString(), json)

        assertEquals(expectedResponse, decoded)
    }

    @Test
    fun `test decoding ServiceQuotaExceededException`() {
        val event = ServiceQuotaExceededException("error")
        val headers = mapOf(
            ":exception-type" to "ServiceQuotaExceededException",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val expectedResponse = LivenessResponseStream(serviceQuotaExceededException = event)

        val data = json.encodeToString(event)
        val encoded = LivenessEventStream.encode(data.toByteArray(), headers)
        val decoded = LivenessEventStream.decode(encoded.array().toByteString(), json)

        assertEquals(expectedResponse, decoded)
    }

    @Test
    fun `test decoding ServiceUnavailableException`() {
        val event = ServiceUnavailableException("error")
        val headers = mapOf(
            ":exception-type" to "ServiceUnavailableException",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val expectedResponse = LivenessResponseStream(serviceUnavailableException = event)

        val data = json.encodeToString(event)
        val encoded = LivenessEventStream.encode(data.toByteArray(), headers)
        val decoded = LivenessEventStream.decode(encoded.array().toByteString(), json)

        assertEquals(expectedResponse, decoded)
    }

    @Test
    fun `test decoding SessionNotFoundException`() {
        val event = SessionNotFoundException("error")
        val headers = mapOf(
            ":exception-type" to "SessionNotFoundException",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val expectedResponse = LivenessResponseStream(sessionNotFoundException = event)

        val data = json.encodeToString(event)
        val encoded = LivenessEventStream.encode(data.toByteArray(), headers)
        val decoded = LivenessEventStream.decode(encoded.array().toByteString(), json)

        assertEquals(expectedResponse, decoded)
    }

    @Test
    fun `test decoding AccessDeniedException`() {
        val event = AccessDeniedException("error")
        val headers = mapOf(
            ":exception-type" to "AccessDeniedException",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val expectedResponse = LivenessResponseStream(accessDeniedException = event)

        val data = json.encodeToString(event)
        val encoded = LivenessEventStream.encode(data.toByteArray(), headers)
        val decoded = LivenessEventStream.decode(encoded.array().toByteString(), json)

        assertEquals(expectedResponse, decoded)
    }

    @Serializable
    internal data class Baz(val quux: Boolean)

    @Serializable
    internal data class Model(val foo: Int, val bar: String, val baz: Baz)
}
