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

import android.text.TextUtils;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Wrap;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

/**
 * A request against a GraphQL endpoint.
 * @param <R> The type of data contained in the GraphQLResponse expected from this request.
 */
public abstract class GraphQLRequest<R> {
    private final Type responseType;
    private final VariablesSerializer variablesSerializer;

    /**
     * Constructor for GraphQLRequest with specification for type of API call.
     * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     */
    public GraphQLRequest(
            Type responseType,
            VariablesSerializer variablesSerializer
    ) {
        this.responseType = responseType;
        this.variablesSerializer = variablesSerializer;
    }

    /**
     * Copy constructor for a GraphQLRequest.
     * @param request GraphQLRequest to be copied
     * @param <R> The type of data contained in the GraphQLResponse expected from this request.
     */
    public <R> GraphQLRequest(GraphQLRequest<R> request) {
        this.responseType = request.responseType;
        this.variablesSerializer = request.variablesSerializer;
    }

    /**
     * Returns a copy of the GraphQLRequest instance.
     * @param <R> The type of data contained in the GraphQLResponse expected from this request.
     * @return Copy of the GraphQLRequest object
     */
    public abstract <R> GraphQLRequest<R> copy();

    /**
     * Returns the GraphQL document which is set as "query" in the request.
     * @return the GraphQL document which is set as "query" in the request.
     */
    public abstract String getQuery();

    /**
     * Returns Map of variables to be serialized and set as "variables" in the request.
     * @return Map of variables to be serialized and set as "variables" in the request.
     */
    public abstract Map<String, Object> getVariables();

    /**
     * Processes query parameters into a query string to
     * be used as HTTP request body.
     * @return processed query string
     */
    public String getContent() {
        String query = getQuery()
                .replace("\"", "\\\"")
                .replace("\n", "\\n");

        String variables = getVariables().isEmpty() ? null : variablesSerializer.serialize(getVariables());

        return Wrap.inBraces(TextUtils.join(", ", Arrays.asList(
            Wrap.inDoubleQuotes("query") + ": " + Wrap.inDoubleQuotes(query),
            Wrap.inDoubleQuotes("variables") + ": " + variables)));
    }

    /**
     * Returns the type of data in the GraphQLResponse expected from this request.
     * @return response type
     */
    public Type getResponseType() {
        return responseType;
    }

    /**
     * Returns the VariablesSerializer for serializing variables on the GraphQLRequest.
     * @return the VariablesSerializer for serializing variables on the GraphQLRequest.
     */
    protected VariablesSerializer getVariablesSerializer() {
        return variablesSerializer;
    }

    /**
     * Any child class which overrides this method should return false if !super.equals(object).
     */
    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        GraphQLRequest<?> request = (GraphQLRequest<?>) thatObject;

        return ObjectsCompat.equals(responseType, request.responseType) &&
                ObjectsCompat.equals(variablesSerializer, request.variablesSerializer);
    }

    /**
     * Any child class which overrides this method should include super.hashCode() as an input for computing hashCode().
     * @return hashCode
     */
    @Override
    public int hashCode() {
        int result = responseType.hashCode();
        result = 31 * result + (variablesSerializer != null ? variablesSerializer.hashCode() : 0);
        return result;
    }

    /**
     * Any child class which overrides this method should append super.toString() to the return value.
     * @return String representation of GraphQLRequest, useful for debugging.
     */
    @Override
    public String toString() {
        return "GraphQLRequest{" +
                ", responseType=\'" + responseType + "\'" +
                ", variablesSerializer=\'" + variablesSerializer + "\'" +
                '}';
    }

    /**
     * An interface defining the method used to serialize a map of variables to go with a request.
     */
    public interface VariablesSerializer {
        /**
         * Takes a map and returns it as a serialized string.
         * @param variables a map of the variables to go with a GraphQL request
         * @return a string of the map properly serialized to be sent
         */
        String serialize(Map<String, Object> variables);
    }
}
