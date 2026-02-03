/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test

/**
 * Unit tests for [GraphQLResponseException].
 */
class GraphQLResponseExceptionTest {

    /**
     * Tests parsing a typical AppSync error response with errorType and message.
     */
    @Test
    fun parsesTypicalAppSyncError() {
        // Arrange
        val jsonString = """
            {
              "errors": [{
                "errorType": "UnauthorizedException",
                "message": "You are not authorized to make this call."
              }]
            }
        """.trimIndent()
        val json = JSONObject(jsonString)

        // Act
        val exception = GraphQLResponseException(json)

        // Assert
        exception.shouldNotBeNull()
        exception.message.shouldBe("UnauthorizedException: You are not authorized to make this call.")

        val errors = exception.errors
        errors.size.shouldBe(1)

        val error = errors[0]
        error.errorType.shouldBe("UnauthorizedException")
        error.message.shouldBe("You are not authorized to make this call.")
        error.toString().shouldBe("UnauthorizedException: You are not authorized to make this call.")
    }

    /**
     * Tests parsing multiple errors in the errors array.
     */
    @Test
    fun parsesMultipleErrors() {
        // Arrange
        val jsonString = """
            {
              "errors": [
                {
                  "errorType": "UnauthorizedException",
                  "message": "First error"
                },
                {
                  "errorType": "ValidationException",
                  "message": "Second error"
                }
              ]
            }
        """.trimIndent()
        val json = JSONObject(jsonString)

        // Act
        val exception = GraphQLResponseException(json)

        // Assert
        val errors = exception.errors
        errors.size.shouldBe(2)

        errors[0].errorType.shouldBe("UnauthorizedException")
        errors[0].message.shouldBe("First error")

        errors[1].errorType.shouldBe("ValidationException")
        errors[1].message.shouldBe("Second error")
    }

    /**
     * Tests that exception message includes all errors when multiple are present.
     */
    @Test
    fun messageIncludesAllErrors() {
        // Arrange
        val jsonString = """
            {
              "errors": [
                {
                  "errorType": "FirstError",
                  "message": "This should be in the message"
                },
                {
                  "errorType": "SecondError",
                  "message": "This should also be in the message"
                }
              ]
            }
        """.trimIndent()
        val json = JSONObject(jsonString)

        // Act
        val exception = GraphQLResponseException(json)

        // Assert - message should contain both errors separated by semicolon
        val message = exception.message
        message.shouldBe(
            "FirstError: This should be in the message; " +
                "SecondError: This should also be in the message"
        )
    }

    /**
     * Tests parsing error with missing optional fields.
     */
    @Test
    fun handlesPartialErrorFields() {
        // Arrange - only message, no errorType
        val jsonString = """
            {
              "errors": [{
                "message": "Something went wrong"
              }]
            }
        """.trimIndent()
        val json = JSONObject(jsonString)

        // Act
        val exception = GraphQLResponseException(json)

        // Assert
        val errors = exception.errors
        errors.size.shouldBe(1)

        val error = errors[0]
        error.errorType.shouldBeNull()
        error.message.shouldBe("Something went wrong")
        error.toString().shouldBe("null: Something went wrong")
    }

    /**
     * Tests parsing error with empty errors array throws JSONException.
     */
    @Test
    fun throwsOnEmptyErrorsArray() {
        // Arrange
        val jsonString = """{"errors": []}"""

        // Act & Assert - empty errors array is invalid per GraphQL spec
        shouldThrow<JSONException> {
            val json = JSONObject(jsonString)
            GraphQLResponseException(json)
        }
    }

    /**
     * Tests that missing errors field throws JSONException.
     */
    @Test
    fun throwsOnMissingErrorsField() {
        // Arrange
        val jsonString = """{"data": null}"""

        // Act & Assert
        shouldThrow<JSONException> {
            val json = JSONObject(jsonString)
            GraphQLResponseException(json)
        }
    }

    /**
     * Tests that malformed JSON throws JSONException.
     */
    @Test
    fun throwsOnMalformedJson() {
        // Arrange
        val jsonString = """{"errors": "not an array"}"""

        // Act & Assert
        shouldThrow<JSONException> {
            val json = JSONObject(jsonString)
            GraphQLResponseException(json)
        }
    }

    /**
     * Tests GraphQLError toString with only errorType.
     */
    @Test
    fun errorToStringWithOnlyErrorType() {
        // Arrange
        val jsonString = """
            {
              "errors": [{
                "errorType": "SomeError"
              }]
            }
        """.trimIndent()
        val json = JSONObject(jsonString)

        // Act
        val exception = GraphQLResponseException(json)

        // Assert
        exception.errors[0].toString().shouldBe("SomeError: null")
    }
}
