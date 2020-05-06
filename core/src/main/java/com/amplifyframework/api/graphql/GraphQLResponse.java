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

import com.amplifyframework.api.ApiException;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper for GraphQL response containing both
 * response data and error information.
 * @param <T> queried data type
 */
public final class GraphQLResponse<T> {
    private final T data;
    private final List<Error> errors;
    private final String nextToken;

    /**
     * Constructs a wrapper for GraphQL response.
     * @param data response data with user-defined cast type
     * @param errors list of error responses as defined by GraphQL doc
     */
    public GraphQLResponse(@Nullable T data, @Nullable List<Error> errors) {
        this(data, errors, null);
    }

    /**
     * Constructs a wrapper for GraphQL response.
     * @param data response data with user-defined cast type
     * @param errors list of error responses as defined by GraphQL doc
     * @param nextToken token for retrieving the next batch of results if there are any, otherwise null.
     */
    public GraphQLResponse(@Nullable T data, @Nullable List<Error> errors, @Nullable String nextToken) {
        this.data = data;
        this.errors = new ArrayList<>();
        if (errors != null) {
            this.errors.addAll(errors);
        }
        this.nextToken = nextToken;
    }

    /**
     * Gets the error response.
     * @return wrapper containing error details
     */
    @NonNull // The list is non-null, but hopefully empty!
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

    /**
     * Gets the token for fetching more results, if there are any.
     * @return nextToken
     */
    @Nullable
    public String getNextToken() {
        return nextToken;
    }

    /**
     * Checks if a nextToken exists, indicating more results can be fetched.
     * @return true if nextToken is not null, false otherwise
     */
    public boolean hasNextToken() {
        return nextToken != null;
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

    @Override
    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + (errors != null ? errors.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GraphQLResponse{" +
                "data=\'" + data + "\'" +
                ", errors=\'" + errors + "\'" +
                '}';
    }

    /**
     * Error object that models GraphQL error.
     * See https://graphql.github.io/graphql-spec/June2018/#sec-Response-Format
     */
    public static final class Error {
        private final String message;
        private final List<GraphQLLocation> locations;
        private final List<GraphQLPathSegment> path;
        private final Map<String, Object> extensions;

        /**
         * Constructs error response in accordance with GraphQL specs.
         * @param message error message
         * @param locations list of locations describing the syntax element
         * @param path The key path of the response field with error.
         * @param extensions additional error map, reserved for implementors.
         */
        public Error(@NonNull String message,
                     @Nullable List<GraphQLLocation> locations,
                     @Nullable List<GraphQLPathSegment> path,
                     @Nullable Map<String, Object> extensions) {
            this.message = Objects.requireNonNull(message);
            this.locations = locations;
            this.path = path;
            this.extensions = extensions;
        }

        /**
         * Gets the error message.
         *
         * @return error message
         */
        @NonNull
        public String getMessage() {
            return message;
        }

        /**
         * Gets the list of locations where each location describes the beginning of an associated
         * syntax element.
         *
         * @return locations
         */
        @Nullable
        public List<GraphQLLocation> getLocations() {
            return Immutable.of(locations);
        }

        /**
         * Gets the key path of the response field which experienced the error.  This allows clients
         * to identify whether a null result is intentional or caused by a runtime error.  The
         * values are either strings or 0-index integers.
         *
         * @return path
         */
        @Nullable
        public List<GraphQLPathSegment> getPath() {
            return Immutable.of(path);
        }

        /**
         * Returns additional error information of type Map&lt;String, Object&gt;.  Reserved for GraphQL
         * implementors to add details however they see fit.  No additional restrictions on its
         * contents.
         *
         * @return extensions
         */
        @Nullable
        public Map<String, Object> getExtensions() {
            return Immutable.of(extensions);
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

            return ObjectsCompat.equals(message, error.message) &&
                   ObjectsCompat.equals(locations, error.locations) &&
                   ObjectsCompat.equals(path, error.path) &&
                   ObjectsCompat.equals(extensions, error.extensions);

        }

        @Override
        public int hashCode() {
            int result = message.hashCode();
            result = 31 * result + (locations != null ? locations.hashCode() : 0);
            result = 31 * result + (path != null ? path.hashCode() : 0);
            result = 31 * result + (extensions != null ? extensions.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "GraphQLResponse.Error{" +
                    "message=\'" + message + "\'" +
                    ", locations=\'" + locations + "\'" +
                    ", path=\'" + path + "\'" +
                    ", extensions=\'" + extensions + "\'" +
                    '}';
        }
    }

    /**
     * A factory which generates a strongly-typed response
     * object from a string that was returned from a GraphQL API.
     */
    public interface Factory {
        /**
         * Builds a response containing a single data object from JSON returned by an API.
         * @param apiResponseJson
         *        Response from the endpoint, containing a string response
         * @param classToCast
         *        The class type to which the JSON string should be
         *        interpreted
         * @param <T> The type of the data field in the response object
         * @return An instance of the casting class which models the data
         *         provided in the response JSON string
         * @throws ApiException If the class provided mismatches the data
         */
        <T> GraphQLResponse<T> buildSingleItemResponse(String apiResponseJson, Class<T> classToCast)
            throws ApiException;

        /**
         * Builds a response containing a list of data objects from JSON returned by an API.
         * @param apiResponseJson
         *        Response from the endpoint, containing a string response
         * @param classToCast
         *        The class type to which the JSON string should be
         *        interpreted
         * @param <T> The type of the elements in the data field list in the response object
         * @return An instance of the casting class which models the data
         *         provided in the response JSON string
         * @throws ApiException If the class provided mismatches the data
         */
        <T> GraphQLResponse<Iterable<T>> buildSingleArrayResponse(String apiResponseJson, Class<T> classToCast)
            throws ApiException;
    }
}
