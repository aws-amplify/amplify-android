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

import android.graphics.RectF
import android.os.Build
import androidx.annotation.VisibleForTesting
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.util.emptyAttributes
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Action
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.aws.BuildConfig
import com.amplifyframework.predictions.aws.exceptions.AccessDeniedException
import com.amplifyframework.predictions.aws.exceptions.FaceLivenessSessionNotFoundException
import com.amplifyframework.predictions.aws.models.liveness.BoundingBox
import com.amplifyframework.predictions.aws.models.liveness.ClientChallenge
import com.amplifyframework.predictions.aws.models.liveness.ClientSessionInformationEvent
import com.amplifyframework.predictions.aws.models.liveness.ColorDisplayed
import com.amplifyframework.predictions.aws.models.liveness.FaceMovementAndLightClientChallenge
import com.amplifyframework.predictions.aws.models.liveness.FreshnessColor
import com.amplifyframework.predictions.aws.models.liveness.InitialFace
import com.amplifyframework.predictions.aws.models.liveness.LivenessResponseStream
import com.amplifyframework.predictions.aws.models.liveness.SessionInformation
import com.amplifyframework.predictions.aws.models.liveness.TargetFace
import com.amplifyframework.predictions.aws.models.liveness.VideoEvent
import com.amplifyframework.predictions.models.FaceLivenessSessionInformation
import com.amplifyframework.util.UserAgent
import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

internal class LivenessWebSocket(
    val credentialsProvider: CredentialsProvider,
    val endpoint: String,
    val region: String,
    val sessionInformation: FaceLivenessSessionInformation,
    val onSessionInformationReceived: Consumer<SessionInformation>,
    val onErrorReceived: Consumer<PredictionsException>,
    val onComplete: Action
) {

    private val signer = AWSV4Signer()
    private var credentials: Credentials? = null

    @VisibleForTesting
    internal var webSocket: WebSocket? = null
    internal val challengeId = UUID.randomUUID().toString()
    private var initialDetectedFace: BoundingBox? = null
    private var faceDetectedStart = 0L
    private var videoStartTimestamp = 0L
    private var videoEndTimestamp = 0L
    private var webSocketError: PredictionsException? = null
    internal var clientStoppedSession = false
    val json = Json { ignoreUnknownKeys = true }

    @VisibleForTesting
    internal var webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            LOG.debug("WebSocket onOpen")
            super.onOpen(webSocket, response)
            this@LivenessWebSocket.webSocket = webSocket
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            LOG.debug("WebSocket onMessage text")
            super.onMessage(webSocket, text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            LOG.debug("WebSocket onMessage bytes")
            try {
                val livenessResponseStream = LivenessEventStream.decode(bytes, json)
                livenessResponseStream?.let { livenessResponse ->
                    if (livenessResponse.serverSessionInformationEvent != null) {
                        onSessionInformationReceived.accept(
                            livenessResponse.serverSessionInformationEvent.sessionInformation
                        )
                    } else if (livenessResponse.disconnectionEvent != null) {
                        this@LivenessWebSocket.webSocket?.close(1000, "Liveness flow completed.")
                    } else {
                        handleWebSocketError(livenessResponse)
                    }
                }
            } catch (e: Exception) {
                LOG.debug("WebSocket unable to decode message from server")
            }

            super.onMessage(webSocket, bytes)
        }
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            LOG.debug("WebSocket onClosing")
            super.onClosing(webSocket, code, reason)
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            LOG.debug("WebSocket onClosed")
            super.onClosed(webSocket, code, reason)
            if (code != 1000 && !clientStoppedSession) {
                val faceLivenessException = webSocketError ?: PredictionsException(
                    "An error occurred during the face liveness check.",
                    reason
                )
                onErrorReceived.accept(faceLivenessException)
            } else {
                onComplete.call()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            LOG.debug("WebSocket onFailure")
            super.onFailure(webSocket, t, response)
            if (!clientStoppedSession) {
                val faceLivenessException = webSocketError ?: PredictionsException(
                    "An unknown error occurred during the Liveness flow.",
                    t,
                    "See attached exception for more details."
                )
                onErrorReceived.accept(faceLivenessException)
            }
        }
    }

    fun start() {
        val userAgent = getUserAgent()

        val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(
                Interceptor { chain ->
                    val requestWithUserAgent = chain.request().newBuilder()
                        .header("x-amz-user-agent", userAgent)
                        .build()
                    chain.proceed(requestWithUserAgent)
                }
            )
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentials = credentialsProvider.resolve(emptyAttributes())
                this@LivenessWebSocket.credentials = credentials
                val signedUri = signer.getSignedUri(URI.create(endpoint), credentials, region, userAgent)
                if (signedUri != null) {
                    val signedEndpoint = URLDecoder.decode(signedUri.toString(), "UTF-8")
                    val signedEndpointNoSpaces = signedEndpoint.replace(" ", signer.encodedSpace)
                    startWebSocket(okHttpClient, signedEndpointNoSpaces)
                } else {
                    onErrorReceived.accept(
                        PredictionsException(
                            "Failed to create the face liveness endpoint.",
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                        )
                    )
                }
            } catch (error: Exception) {
                onErrorReceived.accept(
                    PredictionsException(
                        "Failed to start the face liveness session.",
                        error,
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    )
                )
            }
        }
    }

    private fun getUserAgent(): String {
        val amplifyVersion = BuildConfig.VERSION_NAME
        val deviceManufacturer = Build.MANUFACTURER.replace(" ", "_")
        val deviceName = Build.MODEL.replace(" ", "_")
        val userAgent = "${UserAgent.string()} os/Android/${Build.VERSION.SDK_INT} md/device/$deviceName " +
            "md/device-manufacturer/$deviceManufacturer api/rekognitionstreaming/$amplifyVersion"
        return userAgent.replace(Build.MANUFACTURER, deviceManufacturer).replace(Build.MODEL, deviceName)
            .replace("+", "_")
    }

    private fun startWebSocket(okHttpClient: OkHttpClient, url: String) {
        okHttpClient.newWebSocket(
            Request.Builder().url(url).build(),
            webSocketListener
        )
    }

    private fun handleWebSocketError(livenessResponse: LivenessResponseStream) {
        webSocketError = if (livenessResponse.validationException != null) {
            PredictionsException(
                "An error occurred during the face liveness flow.",
                livenessResponse.validationException,
                "See attached exception for more details."
            )
        } else if (livenessResponse.internalServerException != null) {
            PredictionsException(
                "An error occurred during the face liveness flow.",
                livenessResponse.internalServerException,
                "Retry the face liveness flow."
            )
        } else if (livenessResponse.throttlingException != null) {
            PredictionsException(
                "Failed since the user made too many requests for a face liveness check.",
                livenessResponse.throttlingException,
                "Make sure the face liveness requests are controlled and errors are properly handled."
            )
        } else if (livenessResponse.serviceQuotaExceededException != null) {
            PredictionsException(
                "Failed since the user made too many requests for a face liveness check.",
                livenessResponse.serviceQuotaExceededException,
                "Make sure the face liveness requests are controlled and errors are properly handled."
            )
        } else if (livenessResponse.serviceUnavailableException != null) {
            PredictionsException(
                "Service is currently unavailable.",
                livenessResponse.serviceUnavailableException,
                "Retry the face liveness check."
            )
        } else if (livenessResponse.sessionNotFoundException != null) {
            FaceLivenessSessionNotFoundException(
                cause = livenessResponse.sessionNotFoundException
            )
        } else if (livenessResponse.accessDeniedException != null) {
            AccessDeniedException(
                cause = livenessResponse.accessDeniedException
            )
        } else {
            PredictionsException(
                "An unknown error occurred during the Liveness flow.",
                AmplifyException.TODO_RECOVERY_SUGGESTION
            )
        }
        this.destroy()
    }

    fun sendInitialFaceDetectedEvent(
        initialFaceRect: RectF,
        videoStartTime: Long
    ) {
        // Send initial ClientSessionInformationEvent
        videoStartTimestamp = videoStartTime
        initialDetectedFace = BoundingBox(
            left = initialFaceRect.left / sessionInformation.videoWidth,
            top = initialFaceRect.top / sessionInformation.videoHeight,
            height = initialFaceRect.height() / sessionInformation.videoHeight,
            width = initialFaceRect.width() / sessionInformation.videoWidth
        )
        faceDetectedStart = videoStartTime
        val clientInfoEvent =
            ClientSessionInformationEvent(
                challenge = ClientChallenge(
                    faceMovementAndLightChallenge = FaceMovementAndLightClientChallenge(
                        challengeId = challengeId,
                        initialFace = InitialFace(
                            boundingBox = initialDetectedFace!!,
                            initialFaceDetectedTimestamp = faceDetectedStart
                        ),
                        videoStartTimestamp = videoStartTimestamp
                    )
                )
            )
        sendClientInfoEvent(clientInfoEvent)
    }

    fun sendFinalEvent(targetFaceRect: RectF, faceMatchedStart: Long, faceMatchedEnd: Long) {
        val finalClientInfoEvent = ClientSessionInformationEvent(
            challenge = ClientChallenge(
                FaceMovementAndLightClientChallenge(
                    challengeId = challengeId,
                    videoEndTimestamp = videoEndTimestamp,
                    initialFace = InitialFace(
                        boundingBox = initialDetectedFace!!,
                        initialFaceDetectedTimestamp = faceDetectedStart
                    ),
                    targetFace = TargetFace(
                        faceDetectedInTargetPositionStartTimestamp = faceMatchedStart,
                        faceDetectedInTargetPositionEndTimestamp = faceMatchedEnd,
                        boundingBox = BoundingBox(
                            left = targetFaceRect.left / sessionInformation.videoWidth,
                            top = targetFaceRect.top / sessionInformation.videoHeight,
                            height = targetFaceRect.height() / sessionInformation.videoHeight,
                            width = targetFaceRect.width() / sessionInformation.videoWidth
                        )
                    )
                )
            )
        )
        sendClientInfoEvent(finalClientInfoEvent)
    }

    fun sendColorDisplayedEvent(
        currentColor: FreshnessColor,
        previousColor: FreshnessColor,
        sequenceNumber: Int,
        colorStartTime: Long
    ) {
        val freshnessClientInfo = ClientSessionInformationEvent(
            challenge = ClientChallenge(
                faceMovementAndLightChallenge = FaceMovementAndLightClientChallenge(
                    challengeId = challengeId,
                    colorDisplayed = ColorDisplayed(
                        currentColor = currentColor,
                        previousColor = previousColor,
                        sequenceNumber = sequenceNumber,
                        currentColorStartTimestamp = colorStartTime
                    )
                )
            )
        )
        sendClientInfoEvent(freshnessClientInfo)
    }

    private fun sendClientInfoEvent(clientInfoEvent: ClientSessionInformationEvent) {
        credentials?.let {
            val jsonString = Json.encodeToString(clientInfoEvent)
            val jsonPayload = jsonString.encodeUtf8().toByteArray()
            val encodedPayload = LivenessEventStream.encode(
                jsonPayload,
                mapOf(
                    ":event-type" to "ClientSessionInformationEvent",
                    ":message-type" to "event",
                    ":content-type" to "application/json"
                )
            )
            val eventDate = Date()
            val signedPayload = signer.getSignedFrame(
                region,
                encodedPayload.array(),
                it.secretAccessKey,
                Pair(":date", eventDate)
            )
            val signedPayloadBytes = signedPayload.chunked(2).map { hexChar -> hexChar.toInt(16).toByte() }
                .toByteArray()
            val encodedRequest = LivenessEventStream.encode(
                encodedPayload.array(),
                mapOf(
                    ":date" to eventDate,
                    ":chunk-signature" to signedPayloadBytes
                )
            )

            webSocket?.send(ByteString.of(*encodedRequest.array()))
        }
    }

    fun sendVideoEvent(videoBytes: ByteArray, videoEventTime: Long) {
        if (videoBytes.isNotEmpty()) {
            videoEndTimestamp = videoEventTime
        }
        credentials?.let {
            val videoBuffer = ByteBuffer.wrap(videoBytes)
            val videoEvent = VideoEvent(
                timestampMillis = videoEventTime,
                videoChunk = videoBuffer
            )
            val videoJsonString = Json.encodeToString(videoEvent)
            val videoJsonPayload = videoJsonString.encodeUtf8().toByteArray()
            val encodedVideoPayload = LivenessEventStream.encode(
                videoJsonPayload,
                mapOf(
                    ":event-type" to "VideoEvent",
                    ":message-type" to "event",
                    ":content-type" to "application/json"
                )
            )
            val videoEventDate = Date()
            val signedVideoPayload = signer.getSignedFrame(
                region,
                encodedVideoPayload.array(),
                it.secretAccessKey,
                Pair(":date", videoEventDate)
            )
            val signedVideoPayloadBytes = signedVideoPayload.chunked(2)
                .map { hexChar -> hexChar.toInt(16).toByte() }.toByteArray()
            val encodedVideoRequest = LivenessEventStream.encode(
                encodedVideoPayload.array(),
                mapOf(
                    ":date" to videoEventDate,
                    ":chunk-signature" to signedVideoPayloadBytes
                )
            )
            webSocket?.send(ByteString.of(*encodedVideoRequest.array()))
        }
    }

    fun destroy() {
        // Close gracefully; 1000 means "normal closure"
        webSocket?.close(1000, null)
    }

    companion object {
        private val LOG = Amplify.Logging.logger(CategoryType.PREDICTIONS, "amplify:aws-predictions")
    }
}
