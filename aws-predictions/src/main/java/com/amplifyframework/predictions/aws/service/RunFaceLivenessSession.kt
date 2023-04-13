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

package com.amplifyframework.predictions.aws.service

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.aws.http.LivenessWebSocket
import com.amplifyframework.predictions.aws.models.ColorChallenge
import com.amplifyframework.predictions.aws.models.ColorChallengeResponse
import com.amplifyframework.predictions.aws.models.ColorChallengeType
import com.amplifyframework.predictions.aws.models.ColorDisplayInformation
import com.amplifyframework.predictions.aws.models.FaceTargetChallenge
import com.amplifyframework.predictions.aws.models.FaceTargetChallengeResponse
import com.amplifyframework.predictions.aws.models.FaceTargetMatchingParameters
import com.amplifyframework.predictions.aws.models.InitialFaceDetected
import com.amplifyframework.predictions.aws.models.RgbColor
import com.amplifyframework.predictions.aws.models.liveness.FreshnessColor
import com.amplifyframework.predictions.aws.models.liveness.SessionInformation
import com.amplifyframework.predictions.models.ChallengeResponseEvent
import com.amplifyframework.predictions.models.FaceLivenessSession
import com.amplifyframework.predictions.models.FaceLivenessSessionChallenge
import com.amplifyframework.predictions.models.FaceLivenessSessionInformation
import com.amplifyframework.predictions.models.VideoEvent

internal class RunFaceLivenessSession(
    sessionId: String,
    sessionInformation: FaceLivenessSessionInformation,
    val credentialsProvider: CredentialsProvider,
    onSessionStarted: Consumer<FaceLivenessSession>,
    onComplete: Action,
    onError: Consumer<PredictionsException>
) {

    private val livenessEndpoint = "wss://streaming-rekognition.${sessionInformation.region}.amazonaws.com:443"

    private val livenessWebSocket = LivenessWebSocket(
        credentialsProvider = credentialsProvider,
        endpoint = "$livenessEndpoint/start-face-liveness-session-websocket?session-id=$sessionId" +
            "&challenge-versions=${sessionInformation.challengeVersions}&video-width=" +
            "${sessionInformation.videoWidth.toInt()}&video-height=${sessionInformation.videoHeight.toInt()}",
        region = sessionInformation.region,
        sessionInformation = sessionInformation,
        onSessionInformationReceived = { sessionInformation ->
            val challenges = processSessionInformation(sessionInformation)
            val faceLivenessSession = FaceLivenessSession(
                challenges,
                this@RunFaceLivenessSession::processVideoEvent,
                this@RunFaceLivenessSession::processChallengeResponseEvent,
                this@RunFaceLivenessSession::stopLivenessSession
            )
            onSessionStarted.accept(faceLivenessSession)
        },
        onComplete = {
            onComplete.call()
        },
        onErrorReceived = { error ->
            onError.accept(error)
        }
    ).apply {
        start()
    }

    private fun processSessionInformation(sessionInformation: SessionInformation): List<FaceLivenessSessionChallenge> {
        val challenge = sessionInformation.challenge.faceMovementAndLightChallenge
        val sessionChallenges = mutableListOf<FaceLivenessSessionChallenge>()

        // Face target challenge
        val ovalParameters = challenge.ovalParameters
        val challengeConfig = challenge.challengeConfig
        val faceTargetMatching = FaceTargetMatchingParameters(
            challengeConfig.ovalIouThreshold,
            challengeConfig.ovalIouWidthThreshold,
            challengeConfig.ovalIouHeightThreshold,
            challengeConfig.faceIouWidthThreshold,
            challengeConfig.faceIouHeightThreshold
        )
        val faceTargetChallenge = FaceTargetChallenge(
            ovalParameters.width,
            ovalParameters.height,
            ovalParameters.centerX,
            ovalParameters.centerY,
            faceTargetMatching
        )
        sessionChallenges.add(faceTargetChallenge)

        // Freshness color challenge
        val colorChallengeType = ColorChallengeType.SEQUENTIAL
        val challengeColors = mutableListOf<ColorDisplayInformation>()
        challenge.colorSequences.forEachIndexed { index, colorSequence ->
            val currentColor = colorSequence.freshnessColor.rGB
            val rgbColor = RgbColor(currentColor[0], currentColor[1], currentColor[2])
            var duration = colorSequence.flatDisplayDuration
            var shouldScroll = false
            if (colorSequence.flatDisplayDuration == 0f) {
                duration = colorSequence.downscrollDuration
                shouldScroll = true
            }
            challengeColors.add(
                ColorDisplayInformation(
                    rgbColor,
                    duration,
                    shouldScroll
                )
            )
        }
        val colorChallenge = ColorChallenge(
            livenessWebSocket.challengeId,
            colorChallengeType,
            challengeColors.toList()
        )
        sessionChallenges.add(colorChallenge)

        return sessionChallenges.toList()
    }

    private fun processVideoEvent(videoEvent: VideoEvent) {
        livenessWebSocket.sendVideoEvent(videoEvent.bytes, videoEvent.timestamp.time)
    }

    private fun processChallengeResponseEvent(challengeResponseEvent: ChallengeResponseEvent) {
        when (challengeResponseEvent) {
            is InitialFaceDetected -> {
                livenessWebSocket.sendInitialFaceDetectedEvent(
                    challengeResponseEvent.faceLocation,
                    challengeResponseEvent.timestamp.time
                )
            }
            is ColorChallengeResponse -> {
                val currentColor = listOf(
                    challengeResponseEvent.currentColor.red,
                    challengeResponseEvent.currentColor.green,
                    challengeResponseEvent.currentColor.blue
                )
                val previousColor = if (challengeResponseEvent.previousColor != null) {
                    listOf(
                        challengeResponseEvent.previousColor.red,
                        challengeResponseEvent.previousColor.green,
                        challengeResponseEvent.previousColor.blue
                    )
                } else {
                    currentColor
                }
                livenessWebSocket.sendColorDisplayedEvent(
                    FreshnessColor(
                        currentColor
                    ),
                    FreshnessColor(
                        previousColor
                    ),
                    challengeResponseEvent.colorSequenceIndex,
                    challengeResponseEvent.currentColorStartTime.time
                )
            }
            is FaceTargetChallengeResponse -> {
                livenessWebSocket.sendFinalEvent(
                    challengeResponseEvent.targetLocation,
                    challengeResponseEvent.faceInTargetStartTimestamp.time,
                    challengeResponseEvent.faceInTargetEndTimestamp.time
                )
            }
        }
    }

    private fun stopLivenessSession() {
        livenessWebSocket.clientStoppedSession = true
        livenessWebSocket.destroy()
    }
}
