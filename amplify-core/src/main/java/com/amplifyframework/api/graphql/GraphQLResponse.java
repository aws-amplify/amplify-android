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

import com.amplifyframework.api.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for GraphQL response containing both
 * response data and error information.
 * @param <T> queried data type
 */
public final class GraphQLResponse<T> extends Response<T> {
    private final List<Error> errors;

    /**
     * Constructs a wrapper for graphql response.
     * @param data response data with user-defined cast type
     * @param errors list of error responses as defined
     *               by graphql doc
     */
    public GraphQLResponse(@Nullable T data, @Nullable List<Error> errors) {
        super(data);
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

    @SuppressWarnings({"NeedBraces", "EqualsReplaceableByObjectsCall"})
    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) return true;
        if (thatObject == null || getClass() != thatObject.getClass()) return false;

        GraphQLResponse<?> response = (GraphQLResponse<?>) thatObject;

        if (getData() != null ? !getData().equals(response.getData()) : response.getData() != null) return false;
        return errors != null ? errors.equals(response.errors) : response.errors == null;
    }

    @SuppressWarnings({"NeedBraces", "MagicNumber"})
    @Override
    public int hashCode() {
        int result = getData() != null ? getData().hashCode() : 0;
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

        @SuppressWarnings({"NeedBraces", "EqualsReplaceableByObjectsCall"})
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

