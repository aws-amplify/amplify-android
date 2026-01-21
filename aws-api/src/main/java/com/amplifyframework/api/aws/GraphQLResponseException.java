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
            Integer errorCode = errorObj.has("errorCode") ? errorObj.getInt("errorCode") : null;
            errorList.add(new GraphQLError(
                errorObj.optString("errorType", null),
                errorObj.optString("message", null),
                errorCode
            ));
        }
        return errorList;
    }
    
    /**
     * Represents a single GraphQL error from the errors array.
     */
    public static final class GraphQLError {
        private final String errorType;
        private final String message;
        private final Integer errorCode;
        
        GraphQLError(@Nullable String errorType, @Nullable String message, @Nullable Integer errorCode) {
            this.errorType = errorType;
            this.message = message;
            this.errorCode = errorCode;
        }
        
        /**
         * Gets the error type (AWS AppSync extension).
         * @return The error type, or null if not present
         */
        public String getErrorType() {
            return errorType;
        }
        
        /**
         * Gets the error message.
         * @return The error message, or null if not present
         */
        public String getMessage() {
            return message;
        }
        
        /**
         * Gets the error code.
         * @return The error code, or null if not present
         */
        public Integer getErrorCode() {
            return errorCode;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %s (code: %s)", errorType, message, errorCode);
        }
    }
}
