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

import android.net.Uri
import androidx.annotation.VisibleForTesting
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
import com.amplifyframework.predictions.aws.models.liveness.ChallengeConfig
import com.amplifyframework.predictions.aws.models.liveness.FreshnessColor
import com.amplifyframework.predictions.aws.models.liveness.OvalParameters
import com.amplifyframework.predictions.aws.models.liveness.SessionInformation
import com.amplifyframework.predictions.models.ChallengeResponseEvent
import com.amplifyframework.predictions.models.FaceLivenessSession
import com.amplifyframework.predictions.models.FaceLivenessSessionChallenge
import com.amplifyframework.predictions.models.FaceLivenessSessionInformation
import com.amplifyframework.predictions.models.VideoEvent

internal class RunFaceLivenessSession(
    private val sessionId: String,
    private val clientSessionInformation: FaceLivenessSessionInformation,
    val credentialsProvider: CredentialsProvider,
    livenessVersion: String?,
    onSessionStarted: Consumer<FaceLivenessSession>,
    onComplete: Action,
    onError: Consumer<PredictionsException>
) {

    private val livenessWebSocket = LivenessWebSocket(
        credentialsProvider = credentialsProvider,
        endpoint = buildWebSocketEndpoint(),
        region = clientSessionInformation.region,
        clientSessionInformation = clientSessionInformation,
        livenessVersion = livenessVersion,
        onSessionResponseReceived = { serverSessionResponse ->
            val challenges = processSessionInformation(serverSessionResponse.faceLivenessSession)
            val challengeType = serverSessionResponse.livenessChallengeType
            val faceLivenessSession = FaceLivenessSession(
                challengeId = getChallengeId(),
                challengeType = challengeType,
                challenges = challenges,
                onVideoEvent = this@RunFaceLivenessSession::processVideoEvent,
                onChallengeResponseEvent = this@RunFaceLivenessSession::processChallengeResponseEvent,
                stopLivenessSession = this@RunFaceLivenessSession::stopLivenessSession
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
        val sessionChallenges = mutableListOf<FaceLivenessSessionChallenge>()

        if (sessionInformation.challenge.faceMovementAndLightChallenge != null) {
            val challenge = sessionInformation.challenge.faceMovementAndLightChallenge

            // Face target challenge
            sessionChallenges.add(getFaceTargetChallenge(challenge.ovalParameters, challenge.challengeConfig))

            // Freshness color challenge
            val colorChallengeType = ColorChallengeType.SEQUENTIAL
            val challengeColors = mutableListOf<ColorDisplayInformation>()
            challenge.colorSequences.forEachIndexed { _, colorSequence ->
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
        } else if (sessionInformation.challenge.faceMovementChallenge != null) {
            val challenge = sessionInformation.challenge.faceMovementChallenge

            // Face target challenge
            sessionChallenges.add(getFaceTargetChallenge(challenge.ovalParameters, challenge.challengeConfig))
        }

        return sessionChallenges.toList()
    }

    private fun getChallengeId(): String = livenessWebSocket.challengeId

    private fun getFaceTargetChallenge(
        ovalParameters: OvalParameters,
        challengeConfig: ChallengeConfig
    ): FaceTargetChallenge {
        val faceTargetMatching = FaceTargetMatchingParameters(
            targetIouThreshold = challengeConfig.ovalIouThreshold,
            targetIouWidthThreshold = challengeConfig.ovalIouWidthThreshold,
            targetIouHeightThreshold = challengeConfig.ovalIouHeightThreshold,
            targetHeightWidthRatio = challengeConfig.ovalHeightWidthRatio,
            faceDetectionThreshold = challengeConfig.blazeFaceDetectionThreshold,
            faceIouWidthThreshold = challengeConfig.faceIouWidthThreshold,
            faceIouHeightThreshold = challengeConfig.faceIouHeightThreshold,
            faceDistanceThreshold = challengeConfig.faceDistanceThreshold,
            faceDistanceThresholdMin = challengeConfig.faceDistanceThresholdMin,
            ovalFitTimeout = challengeConfig.ovalFitTimeout
        )
        return FaceTargetChallenge(
            ovalParameters.width,
            ovalParameters.height,
            ovalParameters.centerX,
            ovalParameters.centerY,
            faceTargetMatching
        )
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

    private fun stopLivenessSession(reasonCode: Int?) {
        livenessWebSocket.clientStoppedSession = true
        reasonCode?.let { livenessWebSocket.destroy(it) } ?: livenessWebSocket.destroy()
    }

    @VisibleForTesting
    fun buildWebSocketEndpoint(): String {
        val challengeVersionString = clientSessionInformation.challengeVersions.joinToString(",") {
            it.toQueryParamString()
        }

        val uriBuilder = Uri.Builder()
            .scheme("wss")
            .encodedAuthority("streaming-rekognition.${clientSessionInformation.region}.amazonaws.com:443")
            .appendPath("start-face-liveness-session-websocket")
            .appendQueryParameter("session-id", sessionId)
            .appendQueryParameter("video-width", clientSessionInformation.videoWidth.toInt().toString())
            .appendQueryParameter("video-height", clientSessionInformation.videoHeight.toInt().toString())
            .appendQueryParameter(
                "challenge-versions",
                challengeVersionString
            )

        if (clientSessionInformation.preCheckViewEnabled != null) {
            uriBuilder.appendQueryParameter(
                "precheck-view-enabled",
                if (clientSessionInformation.preCheckViewEnabled!!) "1" else "0"
            )
        }

        if (clientSessionInformation.attemptCount != null) {
            uriBuilder.appendQueryParameter(
                "attempt-count",
                clientSessionInformation.attemptCount.toString()
            )
        }

        return uriBuilder.build().toString()
    }
}
