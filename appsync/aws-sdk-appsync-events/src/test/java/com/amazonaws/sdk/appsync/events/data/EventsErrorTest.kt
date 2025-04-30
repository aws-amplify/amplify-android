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
import org.junit.Test

class EventsErrorTest {

    private val knownErrorTypes = mapOf(
        "UnauthorizedException" to UnauthorizedException::class,
        "BadRequestException" to BadRequestException::class,
        "MaxSubscriptionsReachedError" to MaxSubscriptionsReachedException::class,
        "LimitExceededError" to RateLimitExceededException::class,
        "ResourceNotFound" to ResourceNotFoundException::class,
        "UnsupportedOperation" to UnsupportedOperationException::class,
        "InvalidInputError" to InvalidInputException::class,
    )

    @Test
    fun `test all known error types with provided message`() {
        val expectedMessage = "Test Provided Message"
        val fallbackMessage = "Test Fallback Message"

        knownErrorTypes.forEach { (errorType, expectedClass) ->
            val error = EventsError(errorType, expectedMessage)
            val exception = error.toEventsException(fallbackMessage)

            exception.message shouldBe expectedMessage
            exception::class shouldBe expectedClass
        }
    }

    @Test
    fun `test all known error types with fallback message`() {
        val expectedMessage = null
        val fallbackMessage = "Test Fallback Message"

        knownErrorTypes.forEach { (errorType, expectedClass) ->
            val error = EventsError(errorType, expectedMessage)
            val exception = error.toEventsException(fallbackMessage)

            exception.message shouldBe fallbackMessage
            exception::class shouldBe expectedClass
        }
    }

    @Test
    fun `test unknown error type with provided message`() {
        val expectedMessage = "Test Provided Message"
        val fallbackMessage = "Test Fallback Message"

        val error = EventsError("UnknownError", expectedMessage)
        val exception = error.toEventsException(fallbackMessage)

        exception.message shouldBe "UnknownError: $expectedMessage"
        exception::class shouldBe EventsException::class
    }

    @Test
    fun `test unknown error type with fallback message`() {
        val expectedMessage = null
        val fallbackMessage = "Test Fallback Message"

        val error = EventsError("UnknownError", expectedMessage)
        val exception = error.toEventsException(fallbackMessage)

        exception.message shouldBe "UnknownError: $fallbackMessage"
        exception::class shouldBe EventsException::class
    }

    @Test
    fun `events errors chooses first error with provided message`() {
        val expectedMessage = "Test Provided Message"
        val fallbackMessage = "Test Fallback Message"
        val errors = EventsErrors(
            listOf(
                EventsError("UnauthorizedException", expectedMessage),
                EventsError("RateLimitExceededException", expectedMessage)
            )
        )

        val exception = errors.toEventsException(fallbackMessage)

        exception::class shouldBe UnauthorizedException::class
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `events errors chooses first error with fallback message`() {
        val fallbackMessage = "Test Fallback Message"
        val errors = EventsErrors(
            listOf(
                EventsError("UnauthorizedException", null),
                EventsError("RateLimitExceededException", null)
            )
        )

        val exception = errors.toEventsException(fallbackMessage)

        exception::class shouldBe UnauthorizedException::class
        exception.message shouldBe fallbackMessage
    }
}
