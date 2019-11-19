/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.graphql;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper for GraphQL response containing both
 * response data and error information.
 * @param <T> queried data type
 */
public final class GraphQLResponse<T> {
    private final T data;
    private final List<Error> errors;

    /**
     * Constructs a wrapper for graphql response.
     * @param data response data with user-defined cast type
     * @param errors list of error responses as defined
     *               by graphql doc
     */
    public GraphQLResponse(@Nullable T data, @Nullable List<Error> errors) {
        this.data = data;
        this.errors = new ArrayList<>();
        if (errors != null) {
            this.errors.addAll(errors);
        }
    }

    /**
     * Gets the error response.
     * @return wrapper containing error details
     */
    public List<Error> getErrors() {
        return errors;
    }

    /**
     * Checks that errors were generated.
     * @return true if error exists, false otherwise
     */
    public boolean hasErrors() {
        return errors.size() > 0;
    }

    /**
     * Gets the data sent back by API.
     * @return API response body
     */
    public T getData() {
        return data;
    }

    /**
     * Checks that data was returned.
     * @return true if data exists, false otherwise
     */
    public boolean hasData() {
        return data != null;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        GraphQLResponse<?> that = (GraphQLResponse<?>) thatObject;

        if (!ObjectsCompat.equals(data, that.data)) {
            return false;
        }
        return ObjectsCompat.equals(errors, that.errors);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + (errors != null ? errors.hashCode() : 0);
        return result;
    }

    /**
     * Error object that models GraphQL error.
     * See https://graphql.github.io/graphql-spec/June2018/#sec-Response-Format
     */
    public static final class Error {
        private final String message;

        /**
         * Constructs error response in accordance with GraphQL specs.
         * @param message error message
         */
        public Error(@NonNull String message) {
            this.message = Objects.requireNonNull(message);
        }

        /**
         * Gets the error message.
         * @return error message
         */
        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Error error = (Error) thatObject;

            return ObjectsCompat.equals(message, error.message);
        }

        @Override
        public int hashCode() {
            return message != null ? message.hashCode() : 0;
        }
    }

    /**
     * A factory generate strongly-typed response
     * objects from a string that was returned from a GraphQL API.
     */
    public interface Factory {
        /**
         * Builds a response object from a string response from a API.
         * @param apiResponseJson
         *        Response from the endpoint, containing a string response
         * @param classToCast
         *        The class type to which the JSON string should be
         *        interpreted
         * @param <T> The type of the data field in the response object
         * @return An instance of the casting class which models the data
         *         provided in the response JSON string
         */
        <T> GraphQLResponse<T> buildResponse(String apiResponseJson, Class<T> classToCast);
    }
}
