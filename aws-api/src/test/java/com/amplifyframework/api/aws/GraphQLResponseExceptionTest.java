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

package com.amplifyframework.api.aws;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for {@link GraphQLResponseException}.
 */
public final class GraphQLResponseExceptionTest {

    /**
     * Tests parsing a typical AppSync error response with errorType and message.
     * @throws JSONException if JSON construction fails
     */
    @Test
    public void parsesTypicalAppSyncError() throws JSONException {
        // Arrange
        String jsonString = "{"
            + "\"errors\": [{"
            + "  \"errorType\": \"UnauthorizedException\","
            + "  \"message\": \"You are not authorized to make this call.\""
            + "}]"
            + "}";
        JSONObject json = new JSONObject(jsonString);

        // Act
        GraphQLResponseException exception = new GraphQLResponseException(json);

        // Assert
        assertNotNull(exception);
        assertEquals("UnauthorizedException: You are not authorized to make this call. (code: null)", 
            exception.getMessage());
        
        List<GraphQLResponseException.GraphQLError> errors = exception.getErrors();
        assertEquals(1, errors.size());
        
        GraphQLResponseException.GraphQLError error = errors.get(0);
        assertEquals("UnauthorizedException", error.getErrorType());
        assertEquals("You are not authorized to make this call.", error.getMessage());
        assertNull(error.getErrorCode());
        assertEquals("UnauthorizedException: You are not authorized to make this call. (code: null)", 
            error.toString());
    }

    /**
     * Tests parsing an AppSync error with errorCode field.
     * @throws JSONException if JSON construction fails
     */
    @Test
    public void parsesErrorWithErrorCode() throws JSONException {
        // Arrange
        String jsonString = "{"
            + "\"errors\": [{"
            + "  \"errorType\": \"UnauthorizedException\","
            + "  \"message\": \"You are not authorized to make this call.\","
            + "  \"errorCode\": 401"
            + "}]"
            + "}";
        JSONObject json = new JSONObject(jsonString);

        // Act
        GraphQLResponseException exception = new GraphQLResponseException(json);

        // Assert
        List<GraphQLResponseException.GraphQLError> errors = exception.getErrors();
        assertEquals(1, errors.size());
        
        GraphQLResponseException.GraphQLError error = errors.get(0);
        assertEquals("UnauthorizedException", error.getErrorType());
        assertEquals("You are not authorized to make this call.", error.getMessage());
        assertEquals(Integer.valueOf(401), error.getErrorCode());
        assertEquals("UnauthorizedException: You are not authorized to make this call. (code: 401)", 
            error.toString());
    }

    /**
     * Tests parsing multiple errors in the errors array.
     * @throws JSONException if JSON construction fails
     */
    @Test
    public void parsesMultipleErrors() throws JSONException {
        // Arrange
        String jsonString = "{"
            + "\"errors\": ["
            + "  {"
            + "    \"errorType\": \"UnauthorizedException\","
            + "    \"message\": \"First error\""
            + "  },"
            + "  {"
            + "    \"errorType\": \"ValidationException\","
            + "    \"message\": \"Second error\","
            + "    \"errorCode\": 400"
            + "  }"
            + "]"
            + "}";
        JSONObject json = new JSONObject(jsonString);

        // Act
        GraphQLResponseException exception = new GraphQLResponseException(json);

        // Assert
        List<GraphQLResponseException.GraphQLError> errors = exception.getErrors();
        assertEquals(2, errors.size());
        
        assertEquals("UnauthorizedException", errors.get(0).getErrorType());
        assertEquals("First error", errors.get(0).getMessage());
        
        assertEquals("ValidationException", errors.get(1).getErrorType());
        assertEquals("Second error", errors.get(1).getMessage());
        assertEquals(Integer.valueOf(400), errors.get(1).getErrorCode());
    }

    /**
     * Tests that exception message includes all errors when multiple are present.
     * @throws JSONException if JSON construction fails
     */
    @Test
    public void messageIncludesAllErrors() throws JSONException {
        // Arrange
        String jsonString = "{"
            + "\"errors\": ["
            + "  {"
            + "    \"errorType\": \"FirstError\","
            + "    \"message\": \"This should be in the message\""
            + "  },"
            + "  {"
            + "    \"errorType\": \"SecondError\","
            + "    \"message\": \"This should also be in the message\""
            + "  }"
            + "]"
            + "}";
        JSONObject json = new JSONObject(jsonString);

        // Act
        GraphQLResponseException exception = new GraphQLResponseException(json);

        // Assert - message should contain both errors separated by semicolon
        String message = exception.getMessage();
        assertEquals("FirstError: This should be in the message (code: null); " +
            "SecondError: This should also be in the message (code: null)", message);
    }

    /**
     * Tests parsing error with missing optional fields.
     * @throws JSONException if JSON construction fails
     */
    @Test
    public void handlesPartialErrorFields() throws JSONException {
        // Arrange - only message, no errorType or errorCode
        String jsonString = "{"
            + "\"errors\": [{"
            + "  \"message\": \"Something went wrong\""
            + "}]"
            + "}";
        JSONObject json = new JSONObject(jsonString);

        // Act
        GraphQLResponseException exception = new GraphQLResponseException(json);

        // Assert
        List<GraphQLResponseException.GraphQLError> errors = exception.getErrors();
        assertEquals(1, errors.size());
        
        GraphQLResponseException.GraphQLError error = errors.get(0);
        assertNull(error.getErrorType());
        assertEquals("Something went wrong", error.getMessage());
        assertNull(error.getErrorCode());
        assertEquals("null: Something went wrong (code: null)", error.toString());
    }

    /**
     * Tests parsing error with empty errors array throws JSONException.
     */
    @Test
    public void throwsOnEmptyErrorsArray() {
        // Arrange
        String jsonString = "{\"errors\": []}";

        // Act & Assert - empty errors array is invalid per GraphQL spec
        assertThrows(JSONException.class, () -> {
            JSONObject json = new JSONObject(jsonString);
            new GraphQLResponseException(json);
        });
    }

    /**
     * Tests that missing errors field throws JSONException.
     */
    @Test
    public void throwsOnMissingErrorsField() {
        // Arrange
        String jsonString = "{\"data\": null}";

        // Act & Assert
        assertThrows(JSONException.class, () -> {
            JSONObject json = new JSONObject(jsonString);
            new GraphQLResponseException(json);
        });
    }

    /**
     * Tests that malformed JSON throws JSONException.
     */
    @Test
    public void throwsOnMalformedJson() {
        // Arrange
        String jsonString = "{\"errors\": \"not an array\"}";

        // Act & Assert
        assertThrows(JSONException.class, () -> {
            JSONObject json = new JSONObject(jsonString);
            new GraphQLResponseException(json);
        });
    }

    /**
     * Tests GraphQLError toString with only errorType.
     * @throws JSONException if JSON construction fails
     */
    @Test
    public void errorToStringWithOnlyErrorType() throws JSONException {
        // Arrange
        String jsonString = "{"
            + "\"errors\": [{"
            + "  \"errorType\": \"SomeError\""
            + "}]"
            + "}";
        JSONObject json = new JSONObject(jsonString);

        // Act
        GraphQLResponseException exception = new GraphQLResponseException(json);

        // Assert
        assertEquals("SomeError: null (code: null)", exception.getErrors().get(0).toString());
    }

    /**
     * Tests GraphQLError toString with errorType and errorCode but no message.
     * @throws JSONException if JSON construction fails
     */
    @Test
    public void errorToStringWithTypeAndCode() throws JSONException {
        // Arrange
        String jsonString = "{"
            + "\"errors\": [{"
            + "  \"errorType\": \"SomeError\","
            + "  \"errorCode\": 500"
            + "}]"
            + "}";
        JSONObject json = new JSONObject(jsonString);

        // Act
        GraphQLResponseException exception = new GraphQLResponseException(json);

        // Assert
        assertEquals("SomeError: null (code: 500)", exception.getErrors().get(0).toString());
    }
}
