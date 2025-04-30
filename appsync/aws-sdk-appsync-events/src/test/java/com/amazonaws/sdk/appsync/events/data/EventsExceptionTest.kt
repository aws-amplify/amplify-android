/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.sdk.appsync.events.data

import io.kotest.matchers.shouldBe
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Test

class EventsExceptionTest {

    @Test
    fun `EventsException stays events exception`() {
        val exception = EventsException("test")

        val returnedException = exception.toEventsException()

        returnedException shouldBe exception
    }

    @Test
    fun `unknown exception becomes EventsException`() {
        val originalExceptionType = IllegalStateException::class
        val originalException = IllegalStateException("test")

        val returnedException = originalException.toEventsException()

        returnedException::class shouldBe EventsException::class
        returnedException.cause!!::class shouldBe originalExceptionType
    }

    @Test
    fun `networking errors become NetworkException`() {
        val unknownHostException = UnknownHostException("test")
        val socketTimeoutException = SocketTimeoutException("test")
        val expectedExceptionType = NetworkException::class

        val eventsExceptionFromUnknownHost = unknownHostException.toEventsException()
        val eventsExceptionFromSocketTimeout = socketTimeoutException.toEventsException()

        eventsExceptionFromUnknownHost::class shouldBe expectedExceptionType
        eventsExceptionFromSocketTimeout::class shouldBe expectedExceptionType

        eventsExceptionFromUnknownHost.cause shouldBe unknownHostException
        eventsExceptionFromSocketTimeout.cause shouldBe socketTimeoutException
    }
}
