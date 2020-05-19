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

import androidx.core.util.ObjectsCompat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A request against a GraphQL endpoint.
 * @param <R> The type of data contained in the GraphQLResponse that results from this request.
 */
public final class GraphQLRequest<R> {
    private final String document;
    private final Map<String, Object> variables;
    private final List<String> fragments;
    private final Type responseType;
    private final VariablesSerializer variablesSerializer;

    /**
     * Constructor for GraphQLRequest with
     * specification for type of API call.
     * @param document query document to process
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     */
    public GraphQLRequest(
            String document,
            Type responseType,
            VariablesSerializer variablesSerializer
    ) {
        this(document, new HashMap<>(), responseType, variablesSerializer);
    }

    /**
     * Constructor for GraphQLRequest with
     * specification for type of API call.
     * @param document query document to process
     * @param variables variables to be added
     * @param responseType
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     */
    public GraphQLRequest(
            String document,
            Map<String, Object> variables,
            Type responseType,
            VariablesSerializer variablesSerializer
    ) {
        this.document = document;
        this.variables = variables;
        this.fragments = new ArrayList<>();
        this.responseType = responseType;
        this.variablesSerializer = variablesSerializer;
    }

    public <R> GraphQLRequest(GraphQLRequest<R> request) {
        this.document = request.document;
        this.variables = new HashMap<>(request.variables);
        this.fragments = new ArrayList<>(request.fragments);
        this.responseType = request.responseType;
        this.variablesSerializer = request.variablesSerializer;
    }

    public <R> GraphQLRequest<R> copy() {
        return new GraphQLRequest<R>(this);
    }

    /**
     * Processes query parameters into a query string to
     * be used as HTTP request body.
     * @return processed query string
     */
    public String getContent() {
        final StringBuilder completeQuery = new StringBuilder();
        final StringBuilder realQuery = new StringBuilder();

        completeQuery.append("{\"query\":")
                .append("\"");

        realQuery.append(document
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
        );

        for (String fragment : fragments) {
            realQuery
                    .append("fragment ")
                    .append(fragment);
        }

        completeQuery.append(realQuery)
                .append("\"")
                .append(",")
                .append("\"variables\":");

        if (variables.isEmpty()) {
            completeQuery.append("null");
        } else {
            completeQuery.append(variablesSerializer.serialize(this.variables));
        }

        completeQuery.append("}");
        String contentString = completeQuery.toString();

        while (contentString.contains("\\\\")) {
            contentString = contentString.replace("\\\\", "\\");
        }

        return contentString + "\n";
    }

    /**
     * Attaches variable key-value pair.
     * @param key variable name
     * @param value variable value
     * @return this query object for chaining
     */
    public GraphQLRequest<R> putVariable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    /**
     * Attaches a fragment.
     * @param fragment fragment
     * @return this query object for chaining
     */
    public GraphQLRequest<R> addFragment(String fragment) {
        fragments.add(fragment);
        return this;
    }

    /**
     * Returns the Type that should be used for deserializing the response (e.g. GraphQLRequest<Iterable<Post>>>)
     * @return response type
     */
    public Type getResponseType() {
        return responseType;
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

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        GraphQLRequest request = (GraphQLRequest) thatObject;

        return ObjectsCompat.equals(document, request.document) &&
                ObjectsCompat.equals(fragments, request.fragments) &&
                ObjectsCompat.equals(responseType, request.responseType) &&
                ObjectsCompat.equals(variables, request.variables) &&
                ObjectsCompat.equals(variablesSerializer, request.variablesSerializer);
    }

    @Override
    public int hashCode() {
        int result = document.hashCode();
        result = 31 * result + (fragments != null ? fragments.hashCode() : 0);
        result = 31 * result + (responseType != null ? responseType.hashCode() : 0);
        result = 31 * result + (variables != null ? variables.hashCode() : 0);
        result = 31 * result + (variablesSerializer != null ? variablesSerializer.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GraphQLRequest{" +
                "document=\'" + document + "\'" +
                ", fragments=\'" + fragments + "\'" +
                ", responseType=\'" + responseType + "\'" +
                ", variables=\'" + variables + "\'" +
                ", variablesSerializer=\'" + variablesSerializer + "\'" +
                '}';
    }
}