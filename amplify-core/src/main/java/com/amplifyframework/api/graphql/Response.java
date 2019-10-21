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

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for GraphQL response containing both
 * response data and error information.
 * @param <T> queried data type
 */
public final class Response<T> {
    private final T data;
    private final List<Error> errors;

    /**
     * Constructs a wrapper for graphql response.
     * @param data response data with user-defined cast type
     * @param errors list of error responses as defined
     *               by graphql doc
     */
    public Response(@Nullable T data, @Nullable List<Error> errors) {
        this.data = data;
        this.errors = new ArrayList<>();
        if (errors != null) {
            this.errors.addAll(errors);
        }
    }

    /**
     * Gets the response data.
     * @return data returned from query
     */
    public T getData() {
        return data;
    }

    /**
     * Gets the error response.
     * @return wrapper containing error details
     */
    public List<Error> getErrors() {
        return errors;
    }

    /**
     * Checks that data was returned.
     * @return true if data exists, false otherwise
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Checks that errors were generated.
     * @return true if error exists, false otherwise
     */
    public boolean hasErrors() {
        return errors.size() > 0;
    }

    @SuppressWarnings({"NeedBraces", "EqualsReplaceableByObjectsCall"})
    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) return true;
        if (thatObject == null || getClass() != thatObject.getClass()) return false;

        Response<?> response = (Response<?>) thatObject;

        if (data != null ? !data.equals(response.data) : response.data != null) return false;
        return errors != null ? errors.equals(response.errors) : response.errors == null;
    }

    @SuppressWarnings({"NeedBraces", "MagicNumber"})
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
        public Error(String message) {
            this.message = message;
        }

        /**
         * Gets the error message.
         * @return error message
         */
        public String getMessage() {
            return message;
        }

        @SuppressWarnings("NeedBraces")
        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) return true;
            if (thatObject == null || getClass() != thatObject.getClass()) return false;

            Error error = (Error) thatObject;

            return message != null ? message.equals(error.message) : error.message == null;
        }

        @SuppressWarnings("NeedBraces")
        @Override
        public int hashCode() {
            return message != null ? message.hashCode() : 0;
        }
    }
}

