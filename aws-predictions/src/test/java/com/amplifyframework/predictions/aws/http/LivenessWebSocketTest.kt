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

import android.os.Build
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import com.amplifyframework.core.Action
import com.amplifyframework.core.BuildConfig
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.aws.exceptions.FaceLivenessUnsupportedChallengeTypeException
import com.amplifyframework.predictions.aws.models.liveness.ChallengeConfig
import com.amplifyframework.predictions.aws.models.liveness.ChallengeEvent
import com.amplifyframework.predictions.aws.models.liveness.ColorSequence
import com.amplifyframework.predictions.aws.models.liveness.DisconnectionEvent
import com.amplifyframework.predictions.aws.models.liveness.FaceMovementAndLightServerChallenge
import com.amplifyframework.predictions.aws.models.liveness.FaceMovementServerChallenge
import com.amplifyframework.predictions.aws.models.liveness.FreshnessColor
import com.amplifyframework.predictions.aws.models.liveness.InvalidSignatureException
import com.amplifyframework.predictions.aws.models.liveness.LightChallengeType
import com.amplifyframework.predictions.aws.models.liveness.OvalParameters
import com.amplifyframework.predictions.aws.models.liveness.ServerChallenge
import com.amplifyframework.predictions.aws.models.liveness.ServerSessionInformationEvent
import com.amplifyframework.predictions.aws.models.liveness.SessionInformation
import com.amplifyframework.predictions.aws.models.liveness.ValidationException
import com.amplifyframework.predictions.models.Challenge
import com.amplifyframework.predictions.models.FaceLivenessChallengeType
import com.amplifyframework.predictions.models.FaceLivenessSessionInformation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class LivenessWebSocketTest {
    private val json = Json { encodeDefaults = true }

    private lateinit var server: MockWebServer

    private val onComplete = mockk<Action>(relaxed = true)
    private val onSessionResponseReceived = mockk<Consumer<LivenessWebSocket.SessionResponse>>(relaxed = true)
    private val onErrorReceived = mockk<Consumer<PredictionsException>>(relaxed = true)
    private val credentialsProvider = object : CredentialsProvider {
        override suspend fun resolve(attributes: Attributes): Credentials {
            return Credentials(
                "",
                "",
                "",
                null,
                ""
            )
        }
    }

    private val defaultSessionInformation = createClientSessionInformation(
        listOf(Challenge.FaceMovementChallenge("1.0.0"))
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer()
    }

    @After
    fun shutDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun `onClosing informs webSocket`() {
        val webSocket = mockk<WebSocket>(relaxed = true)
        val livenessWebSocket = createLivenessWebSocket()
        livenessWebSocket.webSocket = webSocket

        livenessWebSocket.webSocketListener.onClosing(webSocket, 4, "closing")

        verify { webSocket.close(4, "closing") }
    }

    @Test
    fun `normal status onClosed calls onComplete`() {
        val livenessWebSocket = createLivenessWebSocket()
        livenessWebSocket.webSocketListener.onClosed(mockk(), 1000, "closing")

        verify { onComplete.call() }
    }

    @Test
    fun `bad status onClosed calls onError`() {
        val livenessWebSocket = createLivenessWebSocket()
        livenessWebSocket.webSocketListener.onClosed(mockk(), 5000, "closing")

        verify { onErrorReceived.accept(any()) }
    }

    @Test
    fun `onClosed does not call onError if client stopped`() {
        val livenessWebSocket = createLivenessWebSocket()
        livenessWebSocket.clientStoppedSession = true

        livenessWebSocket.webSocketListener.onClosed(mockk(), 5000, "closing")

        verify(exactly = 0) { onErrorReceived.accept(any()) }
    }

    @Test
    fun `onFailure calls onError`() {
        val livenessWebSocket = createLivenessWebSocket()
        // Response does noted like to be mockk
        val response = Response.Builder()
            .code(200)
            .request(Request.Builder().url(URL("https://amazon.com")).build())
            .protocol(Protocol.HTTP_2)
            .message("Response")
            .build()

        livenessWebSocket.webSocketListener.onFailure(mockk(), mockk(), response)

        verify { onErrorReceived.accept(any()) }
    }

    @Test
    fun `onFailure does not call onError if client stopped`() {
        val livenessWebSocket = createLivenessWebSocket()
        livenessWebSocket.clientStoppedSession = true
        // Response does noted like to be mockk
        val response = Response.Builder()
            .code(200)
            .request(Request.Builder().url(URL("https://amazon.com")).build())
            .protocol(Protocol.HTTP_2)
            .message("Response")
            .build()

        livenessWebSocket.webSocketListener.onFailure(mockk(), mockk(), response)

        verify(exactly = 0) { onErrorReceived.accept(any()) }
    }

    @Test
    fun `web socket assigned on open`() {
        val livenessWebSocket = createLivenessWebSocket()
        val openLatch = CountDownLatch(1)
        val latchingListener = LatchingWebSocketResponseListener(
            livenessWebSocket.webSocketListener,
            openLatch = openLatch
        )
        livenessWebSocket.webSocketListener = latchingListener

        server.enqueue(MockResponse().withWebSocketUpgrade(ServerWebSocketListener()))
        server.start()
        livenessWebSocket.start()

        openLatch.await(3, TimeUnit.SECONDS)

        assertTrue(livenessWebSocket.webSocket != null)
        val originalRequest = livenessWebSocket.webSocket!!.request()

        assertTrue(
            originalRequest.url.queryParameter("X-Amz-Credential")!!.endsWith("//rekognition/aws4_request")
        )
        assertEquals("299", originalRequest.url.queryParameter("X-Amz-Expires"))
        assertEquals("host", originalRequest.url.queryParameter("X-Amz-SignedHeaders"))
        assertEquals("AWS4-HMAC-SHA256", originalRequest.url.queryParameter("X-Amz-Algorithm"))
    }

    @Test
    fun `unsupported challengetype returns an exception`() {
        val clientSessionInfo = createClientSessionInformation(
            listOf(Challenge.FaceMovementAndLightChallenge("2.0.0"))
        )
        val livenessWebSocket = createLivenessWebSocket(clientSessionInformation = clientSessionInfo)
        val unknownEvent = "{\"Type\":\"NewChallengeType\",\"Version\":\"1.0.0\"}"

        val challengeEventHeaders = mapOf(
            ":event-type" to "ChallengeEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )

        val encodedChallengeTypeByteString =
            LivenessEventStream.encode(unknownEvent.toByteArray(), challengeEventHeaders).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedChallengeTypeByteString)

        assertEquals(null, livenessWebSocket.challengeType)

        val event = ServerSessionInformationEvent(
            sessionInformation = SessionInformation(
                challenge = ServerChallenge(
                    faceMovementAndLightChallenge = FaceMovementAndLightServerChallenge(
                        ovalParameters = OvalParameters(1.0f, 2.0f, .5f, .7f),
                        lightChallengeType = LightChallengeType.SEQUENTIAL,
                        challengeConfig = ChallengeConfig(
                            1.0f,
                            1.1f,
                            1.2f,
                            1.3f,
                            1.4f,
                            1.5f,
                            1.6f,
                            1.7f,
                            1.8f,
                            1.9f,
                            10
                        ),
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

        val data = json.encodeToString(event)
        val encodedByteString = LivenessEventStream.encode(data.toByteArray(), headers).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedByteString)
        assertEquals(FaceLivenessUnsupportedChallengeTypeException(), livenessWebSocket.webSocketError)
    }

    @Test
    fun `ensure challengetype is properly set when using the deprecated sessioninformation`() {
        val sessionInfo = FaceLivenessSessionInformation(
            1f,
            1f,
            "FaceMovementAndLightChallenge_1.0.0",
            "3"
        )
        val livenessWebSocket = createLivenessWebSocket(clientSessionInformation = sessionInfo)
        val event = ChallengeEvent(
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            version = "1.0.0"
        )

        val headers = mapOf(
            ":event-type" to "ChallengeEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )

        val data = json.encodeToString(event)
        val encodedByteString = LivenessEventStream.encode(data.toByteArray(), headers).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedByteString)

        assertEquals(FaceLivenessChallengeType.FaceMovementAndLightChallenge, livenessWebSocket.challengeType)
    }

    @Test
    fun `server facemovementandlight challenge event tracked`() {
        val sessionInfo = createClientSessionInformation(listOf(Challenge.FaceMovementAndLightChallenge("2.0.0")))
        val livenessWebSocket = createLivenessWebSocket(clientSessionInformation = sessionInfo)
        val event = ChallengeEvent(
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            version = "2.0.0"
        )

        val headers = mapOf(
            ":event-type" to "ChallengeEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )

        val data = json.encodeToString(event)
        val encodedByteString = LivenessEventStream.encode(data.toByteArray(), headers).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedByteString)

        assertEquals(FaceLivenessChallengeType.FaceMovementAndLightChallenge, livenessWebSocket.challengeType)
    }

    @Test
    fun `server facemovement challenge event tracked`() {
        val sessionInfo = createClientSessionInformation(listOf(Challenge.FaceMovementChallenge("1.0.0")))
        val livenessWebSocket = createLivenessWebSocket(clientSessionInformation = sessionInfo)

        val event = ChallengeEvent(
            challengeType = FaceLivenessChallengeType.FaceMovementChallenge,
            version = "1.0.0"
        )

        val headers = mapOf(
            ":event-type" to "ChallengeEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )

        val data = json.encodeToString(event)
        val encodedByteString = LivenessEventStream.encode(data.toByteArray(), headers).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedByteString)

        assertEquals(FaceLivenessChallengeType.FaceMovementChallenge, livenessWebSocket.challengeType)
    }

    @Test
    fun `server facemovementandlight session event tracked`() {
        val sessionInfo = createClientSessionInformation(listOf(Challenge.FaceMovementAndLightChallenge("2.0.0")))
        val livenessWebSocket = createLivenessWebSocket(clientSessionInformation = sessionInfo)

        val challengeEvent = ChallengeEvent(
            challengeType = FaceLivenessChallengeType.FaceMovementAndLightChallenge,
            version = "2.0.0"
        )
        val challengeHeaders = mapOf(
            ":event-type" to "ChallengeEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val challengeData = json.encodeToString(challengeEvent)
        val encodedChallengeHeadersByteString =
            LivenessEventStream.encode(challengeData.toByteArray(), challengeHeaders).array().toByteString()
        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedChallengeHeadersByteString)

        assertEquals(FaceLivenessChallengeType.FaceMovementAndLightChallenge, livenessWebSocket.challengeType)

        val event = ServerSessionInformationEvent(
            sessionInformation = SessionInformation(
                challenge = ServerChallenge(
                    faceMovementAndLightChallenge = FaceMovementAndLightServerChallenge(
                        ovalParameters = OvalParameters(1.0f, 2.0f, .5f, .7f),
                        lightChallengeType = LightChallengeType.SEQUENTIAL,
                        challengeConfig = ChallengeConfig(
                            1.0f,
                            1.1f,
                            1.2f,
                            1.3f,
                            1.4f,
                            1.5f,
                            1.6f,
                            1.7f,
                            1.8f,
                            1.9f,
                            10
                        ),
                        colorSequences = listOf(
                            ColorSequence(FreshnessColor(listOf(0, 1, 2)), 4.0f, 5.0f)
                        )
                    )
                )
            )
        )
        val sessionResponse = LivenessWebSocket.SessionResponse(
            event.sessionInformation,
            FaceLivenessChallengeType.FaceMovementAndLightChallenge
        )
        val headers = mapOf(
            ":event-type" to "ServerSessionInformationEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )

        val data = json.encodeToString(event)
        val encodedByteString = LivenessEventStream.encode(data.toByteArray(), headers).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedByteString)

        verify { onSessionResponseReceived.accept(sessionResponse) }
    }

    @Test
    fun `server facemovement session event tracked`() {
        val sessionInfo = createClientSessionInformation(listOf(Challenge.FaceMovementChallenge("1.0.0")))
        val livenessWebSocket = createLivenessWebSocket(clientSessionInformation = sessionInfo)

        val challengeEvent = ChallengeEvent(
            challengeType = FaceLivenessChallengeType.FaceMovementChallenge,
            version = "1.0.0"
        )
        val challengeHeaders = mapOf(
            ":event-type" to "ChallengeEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )
        val challengeData = json.encodeToString(challengeEvent)
        val encodedChallengeHeadersByteString =
            LivenessEventStream.encode(challengeData.toByteArray(), challengeHeaders).array().toByteString()
        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedChallengeHeadersByteString)

        assertEquals(FaceLivenessChallengeType.FaceMovementChallenge, livenessWebSocket.challengeType)

        val event = ServerSessionInformationEvent(
            sessionInformation = SessionInformation(
                challenge = ServerChallenge(
                    faceMovementChallenge = FaceMovementServerChallenge(
                        ovalParameters = OvalParameters(1.0f, 2.0f, .5f, .7f),
                        challengeConfig = ChallengeConfig(
                            1.0f,
                            1.1f,
                            1.2f,
                            1.3f,
                            1.4f,
                            1.5f,
                            1.6f,
                            1.7f,
                            1.8f,
                            1.9f,
                            10
                        )
                    )
                )
            )
        )
        val sessionResponse = LivenessWebSocket.SessionResponse(
            event.sessionInformation,
            FaceLivenessChallengeType.FaceMovementChallenge
        )
        val sessionHeaders = mapOf(
            ":event-type" to "ServerSessionInformationEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )

        val data = json.encodeToString(event)
        val encodedByteString = LivenessEventStream.encode(data.toByteArray(), sessionHeaders).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedByteString)

        verify { onSessionResponseReceived.accept(sessionResponse) }
    }

    @Test
    fun `unknown event-type ignored`() {
        val webSocket = mockk<WebSocket>(relaxed = true)
        val livenessWebSocket = createLivenessWebSocket()
        livenessWebSocket.webSocket = webSocket
        val event = UnknownEvent()
        val headers = mapOf(
            ":event-type" to "UnknownEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )

        val data = json.encodeToString(event)
        val encodedByteString = LivenessEventStream.encode(data.toByteArray(), headers).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedByteString)

        verify(exactly = 0) { onSessionResponseReceived.accept(any()) }
        verify(exactly = 0) { onErrorReceived.accept(any()) }
        verify(exactly = 0) { webSocket.close(any(), any()) }
    }

    @Test
    fun `unknown exception-type closes websocket`() {
        val webSocket = mockk<WebSocket>(relaxed = true)
        val livenessWebSocket = createLivenessWebSocket()
        livenessWebSocket.webSocket = webSocket
        val event = UnknownEvent()
        val headers = mapOf(
            ":exception-type" to "UnknownException",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )

        val data = json.encodeToString(event)
        val encodedByteString = LivenessEventStream.encode(data.toByteArray(), headers).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedByteString)

        verify(exactly = 0) { onSessionResponseReceived.accept(any()) }
        verify(exactly = 0) { onErrorReceived.accept(any()) }
        verify(exactly = 1) { webSocket.close(any(), any()) }
    }

    @Test
    fun `disconnect event stops websocket`() {
        val livenessWebSocket = createLivenessWebSocket()
        livenessWebSocket.webSocket = mockk()
        val event = DisconnectionEvent(1)
        val headers = mapOf(
            ":exception-type" to "DisconnectionEvent",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )

        val data = json.encodeToString(event)
        val encodedByteString = LivenessEventStream.encode(data.toByteArray(), headers).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedByteString)

        verify { livenessWebSocket.webSocket!!.close(1000, any()) }
    }

    @Test
    fun `web socket error closes websocket`() {
        val livenessWebSocket = createLivenessWebSocket()
        livenessWebSocket.webSocket = mockk()
        val event = ValidationException("ValidationException")
        val headers = mapOf(
            ":exception-type" to "ValidationException",
            ":content-type" to "application/json",
            ":message-type" to "event"
        )

        val data = json.encodeToString(event)
        val encodedByteString = LivenessEventStream.encode(data.toByteArray(), headers).array().toByteString()

        livenessWebSocket.webSocketListener.onMessage(mockk(), encodedByteString)

        verify { livenessWebSocket.webSocket!!.close(1000, any()) }
    }

    @Test
    fun `web socket user agent with null UI version`() {
        val livenessWebSocket = createLivenessWebSocket(livenessVersion = null)
        livenessWebSocket.webSocket = mockk()

        val version = BuildConfig.VERSION_NAME
        val os = Build.VERSION.SDK_INT
        val baseline = "amplify-android:$version md/unknown/robolectric md/locale/en_UNKNOWN os/Android/$os " +
            "md/device/robolectric md/device-manufacturer/unknown api/rekognitionstreaming/$version"
        assertEquals(livenessWebSocket.getUserAgent(), baseline)
    }

    @Test
    fun `web socket user agent with blank UI version`() {
        val livenessWebSocket = createLivenessWebSocket(livenessVersion = " ")
        livenessWebSocket.webSocket = mockk()

        val version = BuildConfig.VERSION_NAME
        val os = Build.VERSION.SDK_INT
        val baseline = "amplify-android:$version md/unknown/robolectric md/locale/en_UNKNOWN os/Android/$os " +
            "md/device/robolectric md/device-manufacturer/unknown api/rekognitionstreaming/$version"
        assertEquals(livenessWebSocket.getUserAgent(), baseline)
    }

    @Test
    fun `web socket user agent includes added UI version`() {
        val livenessWebSocket = createLivenessWebSocket(livenessVersion = "1.1.1")
        livenessWebSocket.webSocket = mockk()

        val version = BuildConfig.VERSION_NAME
        val os = Build.VERSION.SDK_INT
        val baseline = "amplify-android:$version md/unknown/robolectric md/locale/en_UNKNOWN os/Android/$os " +
            "md/device/robolectric md/device-manufacturer/unknown api/rekognitionstreaming/$version"
        val additional = "api/liveness/1.1.1"
        assertEquals(livenessWebSocket.getUserAgent(), "$baseline $additional")
    }

    @Test
    fun `web socket detects clock skew from server response`() {
        val livenessWebSocket = createLivenessWebSocket()
        mockkConstructor(WebSocket::class)
        val socket: WebSocket = mockk {
            every { close(any(), any()) } returns true
        }
        livenessWebSocket.webSocket = socket
        val sdf = SimpleDateFormat(LivenessWebSocket.datePattern, Locale.US)

        // server responds saying time is actually 1 hour in the future
        val oneHour = 1000 * 3600
        val futureDate = sdf.format(Date(Date().time + oneHour))
        val response: Response = mockk()
        every { response.headers }.returns(Headers.headersOf("Date", futureDate))
        every { response.header("Date") }.returns(futureDate)
        livenessWebSocket.webSocketListener.onOpen(socket, response)

        // now we should restart the websocket with an adjusted time
        val openLatch = CountDownLatch(2)
        val latchingListener = LatchingWebSocketResponseListener(
            livenessWebSocket.webSocketListener,
            openLatch = openLatch
        )
        livenessWebSocket.webSocketListener = latchingListener

        server.enqueue(MockResponse().withWebSocketUpgrade(ServerWebSocketListener()))
        server.start()

        livenessWebSocket.webSocketError = PredictionsException(
            "invalid signature",
            InvalidSignatureException("invalid signature"),
            "invalid signature"
        )
        livenessWebSocket.webSocketListener.onClosed(mockk(), 1011, "closing")

        openLatch.await(3, TimeUnit.SECONDS)

        assertTrue(livenessWebSocket.webSocket != null)
        val reconnectRequest = livenessWebSocket.webSocket!!.request()

        // make sure that followup request sends offset date
        val sdfGMT = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        sdfGMT.timeZone = TimeZone.getTimeZone("GMT")
        val sentDate = reconnectRequest.url.queryParameter("X-Amz-Date") ?.let { sdfGMT.parse(it) }
        val diff = abs(Date().time - sentDate?.time!!)
        assert(oneHour - 10000 < diff && diff < oneHour + 10000)

        // also make sure that followup request is valid
        assertTrue(
            reconnectRequest.url.queryParameter("X-Amz-Credential")!!.endsWith("//rekognition/aws4_request")
        )
        assertEquals("299", reconnectRequest.url.queryParameter("X-Amz-Expires"))
        assertEquals("host", reconnectRequest.url.queryParameter("X-Amz-SignedHeaders"))
        assertEquals("AWS4-HMAC-SHA256", reconnectRequest.url.queryParameter("X-Amz-Algorithm"))
    }

    private fun createClientSessionInformation(challengeVersions: List<Challenge>) = FaceLivenessSessionInformation(
        videoWidth = 1f,
        videoHeight = 1f,
        region = "region",
        preCheckViewEnabled = true,
        attemptCount = 1,
        challengeVersions = challengeVersions
    )

    private fun createLivenessWebSocket(
        livenessVersion: String? = null,
        clientSessionInformation: FaceLivenessSessionInformation? = null
    ) = LivenessWebSocket(
        credentialsProvider,
        server.url("/").toString(),
        "",
        clientSessionInformation ?: defaultSessionInformation,
        livenessVersion,
        onSessionResponseReceived,
        onErrorReceived,
        onComplete
    )
}

class LatchingWebSocketResponseListener(
    private val webSocketListener: WebSocketListener,
    private val openLatch: CountDownLatch = CountDownLatch(1)
) : WebSocketListener() {

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        webSocketListener.onClosed(webSocket, code, reason)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocketListener.onClosing(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        webSocketListener.onFailure(webSocket, t, response)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        webSocketListener.onMessage(webSocket, text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        webSocketListener.onMessage(webSocket, bytes)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocketListener.onOpen(webSocket, response)
        openLatch.countDown()
    }
}

@Serializable
internal data class UnknownEvent(val name: String = "")

class ServerWebSocketListener : WebSocketListener() {
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {}
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {}
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {}
    override fun onMessage(webSocket: WebSocket, text: String) {}
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {}
    override fun onOpen(webSocket: WebSocket, response: Response) {}
}
