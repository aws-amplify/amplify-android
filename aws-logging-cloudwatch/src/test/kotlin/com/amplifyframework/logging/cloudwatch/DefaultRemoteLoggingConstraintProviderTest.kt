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
package com.amplifyframework.logging.cloudwatch

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Consumer
import com.amplifyframework.logging.LogLevel
import com.amplifyframework.logging.cloudwatch.models.LoggingConstraints
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultRemoteLoggingConstraintProviderTest {

    private val okHttpClient = mockk<OkHttpClient>()
    private val credentialsProvider = mockk<CredentialsProvider>()
    private lateinit var defaultRemoteLoggingConstraintProvider: DefaultRemoteLoggingConstraintProvider
    private val url = "https://g826tqdqil.execute-api.us-east-1.amazonaws.com/prod/loggingconstraints"
    private val requestSlot = slot<Request>()
    private val mockCall = mockk<Call>(relaxed = true)
    private val mockResponse = mockk<Response>(relaxed = true)

    @Before
    fun setup() = runTest {
        every { mockResponse.close() }.answers { }
        every { mockResponse.body.close() }.answers { }
        coEvery { okHttpClient.newCall(capture(requestSlot)) }.answers { mockCall }
        coEvery { mockCall.execute() }.answers { mockResponse }
        coEvery { credentialsProvider.resolve(any()) }.answers { Credentials("ACCESS_KEY", "SECRET_KEY") }
        defaultRemoteLoggingConstraintProvider = DefaultRemoteLoggingConstraintProvider(
            URL(url),
            "us-east-1",
            okHttpClient = okHttpClient,
            coroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
            credentialsProvider = credentialsProvider
        )
    }

    @Test
    fun `test fetchConfigLogging on success`() = runTest {
        val onSuccessMock = mockk<Consumer<LoggingConstraints>>(relaxed = true)
        val responseSlot = slot<LoggingConstraints>()
        coEvery { onSuccessMock.accept(capture(responseSlot)) }.answers { }
        every { mockResponse.isSuccessful }.answers { true }
        every { mockResponse.body.string() }.answers {
            "{\n" +
                "  \"defaultLogLevel\": \"INFO\"\n" +
                "}"
        }
        val latch = CountDownLatch(1)
        defaultRemoteLoggingConstraintProvider.fetchLoggingConfig({
            onSuccessMock.accept(it)
            latch.countDown()
        }, {})
        latch.await(5, TimeUnit.SECONDS)
        assertTrue(requestSlot.isCaptured)
        assertEquals(url, requestSlot.captured.url.toUrl().toString())
        assertEquals(LoggingConstraints(defaultLogLevel = LogLevel.INFO), responseSlot.captured)
    }

    @Test
    fun `test fetchConfigLogging on failure`() = runTest {
        val onErrorMock = mockk<Consumer<Exception>>(relaxed = true)
        val exceptionSlot = slot<AmplifyException>()
        every { mockResponse.isSuccessful }.answers { false }
        coEvery { onErrorMock.accept(capture(exceptionSlot)) }.answers { }
        every { mockResponse.body.string() }.answers {
            null.toStr()
        }
        val latch = CountDownLatch(1)
        defaultRemoteLoggingConstraintProvider.fetchLoggingConfig({}, {
            onErrorMock.accept(it)
            latch.countDown()
        })
        latch.await(5, TimeUnit.SECONDS)
        assertTrue(requestSlot.isCaptured)
        assertEquals(url, requestSlot.captured.url.toUrl().toString())
        assertEquals(AmplifyException("Failed to fetch remote logging constraints", "null"), exceptionSlot.captured)
    }
}
