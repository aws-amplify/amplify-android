/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.predictions.aws.service

import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.predictions.models.Challenge
import com.amplifyframework.predictions.models.FaceLivenessSessionInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RunFaceLivenessSessionTest {

    private val region = "us-east-1"
    private val sessionId = "123456"
    private val videoWidth = 480f
    private val videoHeight = 640f

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun shutDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test websocket endpoint generation using the old liveness library`() {
        val sessionInformation = FaceLivenessSessionInformation(
            videoHeight = videoHeight,
            videoWidth = videoWidth,
            challenge = "FaceMovementAndLightChallenge_1.0.0",
            region = region
        )

        val session = RunFaceLivenessSession(
            sessionId = sessionId,
            clientSessionInformation = sessionInformation,
            credentialsProvider = CognitoCredentialsProvider(),
            livenessVersion = "1.0.0",
            onSessionStarted = {},
            onComplete = {},
            onError = {}
        )

        Assert.assertEquals(
            generateEndpoint("FaceMovementAndLightChallenge_1.0.0"),
            session.buildWebSocketEndpoint()
        )
    }

    @Test
    fun `test websocket endpoint generation supplying attempt count and enabled start view`() {
        val attemptCount = 1
        val preCheckEnabled = true
        val sessionInformation = FaceLivenessSessionInformation(
            videoHeight = videoHeight,
            videoWidth = videoWidth,
            challengeVersions = listOf(
                Challenge.FaceMovementAndLightChallenge("2.0.0")
            ),
            region = region,
            preCheckViewEnabled = preCheckEnabled,
            attemptCount = attemptCount
        )

        val session = RunFaceLivenessSession(
            sessionId = sessionId,
            clientSessionInformation = sessionInformation,
            credentialsProvider = CognitoCredentialsProvider(),
            livenessVersion = "1.0.0",
            onSessionStarted = {},
            onComplete = {},
            onError = {}
        )

        Assert.assertEquals(
            generateEndpoint("FaceMovementAndLightChallenge_2.0.0", preCheckEnabled, attemptCount),
            session.buildWebSocketEndpoint()
        )
    }

    @Test
    fun `test websocket endpoint generation supplying different attempt count and enabled start view`() {
        val attemptCount = 3
        val preCheckEnabled = true
        val sessionInformation = FaceLivenessSessionInformation(
            videoHeight = videoHeight,
            videoWidth = videoWidth,
            challengeVersions = listOf(
                Challenge.FaceMovementAndLightChallenge("2.0.0")
            ),
            attemptCount = attemptCount,
            region = region,
            preCheckViewEnabled = preCheckEnabled
        )

        val session = RunFaceLivenessSession(
            sessionId = sessionId,
            clientSessionInformation = sessionInformation,
            credentialsProvider = CognitoCredentialsProvider(),
            livenessVersion = "1.0.0",
            onSessionStarted = {},
            onComplete = {},
            onError = {}
        )

        Assert.assertEquals(
            generateEndpoint("FaceMovementAndLightChallenge_2.0.0", preCheckEnabled, attemptCount),
            session.buildWebSocketEndpoint()
        )
    }

    @Test
    fun `test websocket endpoint generation supplying attempt count and disabled start view`() {
        val attemptCount = 3
        val preCheckEnabled = false
        val sessionInformation = FaceLivenessSessionInformation(
            videoHeight = videoHeight,
            videoWidth = videoWidth,
            challengeVersions = listOf(
                Challenge.FaceMovementAndLightChallenge("2.0.0")
            ),
            attemptCount = attemptCount,
            region = region,
            preCheckViewEnabled = preCheckEnabled
        )

        val session = RunFaceLivenessSession(
            sessionId = sessionId,
            clientSessionInformation = sessionInformation,
            credentialsProvider = CognitoCredentialsProvider(),
            livenessVersion = "1.0.0",
            onSessionStarted = {},
            onComplete = {},
            onError = {}
        )

        Assert.assertEquals(
            generateEndpoint("FaceMovementAndLightChallenge_2.0.0", preCheckEnabled, attemptCount),
            session.buildWebSocketEndpoint()
        )
    }

    private fun generateEndpoint(
        challenges: String,
        preCheckEnabled: Boolean? = null,
        attemptCount: Int? = null
    ): String {
        var endpoint = "wss://streaming-rekognition.$region.amazonaws.com:443/start-face-liveness-session-websocket" +
            "?session-id=$sessionId&video-width=${videoWidth.toInt()}&video-height=${videoHeight.toInt()}" +
            "&challenge-versions=$challenges"

        if (preCheckEnabled != null) {
            val value = if (preCheckEnabled) "1" else "0"
            endpoint = "$endpoint&precheck-view-enabled=$value"
        }

        if (attemptCount != null) {
            endpoint = "$endpoint&attempt-count=$attemptCount"
        }

        return endpoint
    }
}
