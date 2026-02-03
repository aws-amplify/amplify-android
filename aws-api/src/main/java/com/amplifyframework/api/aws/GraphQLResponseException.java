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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception representing a GraphQL error response from AppSync.
 * <p>
 * This exception is thrown when AppSync returns errors during connection establishment
 * for GraphQL queries or subscriptions (e.g., authentication failures, authorization errors).
 * <p>
 * Use {@link GraphQLError#getErrorType()} for programmatic error handling.
 * For information on error types, see
 * <a href="https://docs.aws.amazon.com/appsync/latest/APIReference/CommonErrors.html">
 * AWS AppSync Common Errors</a>.
 *
 * @see GraphQLError
 */
public final class GraphQLResponseException extends IOException {
    private static final long serialVersionUID = 1L;
    
    private final List<GraphQLError> errors;
    
    /**
     * Creates a GraphQLResponseException from a JSON response.
     * @param responseJson The JSON response containing the errors array
     * @throws JSONException if the JSON cannot be parsed or doesn't contain valid error structure
     */
    public GraphQLResponseException(@NonNull JSONObject responseJson) throws JSONException {
        this(parseErrors(responseJson));
    }
    
    private GraphQLResponseException(@NonNull List<GraphQLError> errors) {
        super(buildMessage(errors));
        this.errors = errors;
    }
    
    /**
     * Gets the list of GraphQL errors from the response.
     * @return Unmodifiable list of GraphQL errors
     */
    @NonNull
    public List<GraphQLError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    private static String buildMessage(List<GraphQLError> errors) {
        return errors.stream()
            .map(GraphQLError::toString)
            .collect(java.util.stream.Collectors.joining("; "));
    }
    
    private static List<GraphQLError> parseErrors(JSONObject responseJson) throws JSONException {
        if (!responseJson.has("errors")) {
            throw new JSONException("Response does not contain 'errors' field");
        }
        
        JSONArray errorsArray = responseJson.getJSONArray("errors");
        if (errorsArray.length() == 0) {
            throw new JSONException("Errors array is empty");
        }
        
        List<GraphQLError> errorList = new ArrayList<>();
        for (int i = 0; i < errorsArray.length(); i++) {
            JSONObject errorObj = errorsArray.getJSONObject(i);
            errorList.add(new GraphQLError(
                errorObj.optString("errorType", null),
                errorObj.optString("message", null)
            ));
        }
        return errorList;
    }
    
    /**
     * Represents a single GraphQL error from the errors array.
     * <p>
     * Each error contains:
     * <ul>
     *   <li><b>errorType</b> - The primary error identifier (use this for error handling)</li>
     *   <li><b>message</b> - Human-readable error description</li>
     * </ul>
     */
    public static final class GraphQLError {
        private final String errorType;
        private final String message;
        
        GraphQLError(@Nullable String errorType, @Nullable String message) {
            this.errorType = errorType;
            this.message = message;
        }
        
        /**
         * Gets the error type (AWS AppSync extension).
         * Use this field for programmatic error handling.
         * <p>
         * For information on error types, see
         * <a href="https://docs.aws.amazon.com/appsync/latest/APIReference/CommonErrors.html">
         * AWS AppSync Common Errors</a>.
         *
         * @return The error type, or null if not present
         */
        @Nullable
        public String getErrorType() {
            return errorType;
        }
        
        /**
         * Gets the error message.
         * @return The error message, or null if not present
         */
        @Nullable
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %s", errorType, message);
        }
    }
}
