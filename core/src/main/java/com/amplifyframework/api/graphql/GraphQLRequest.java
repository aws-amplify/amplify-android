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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A request against a GraphQL endpoint.
 * @param <T> Class for the model in this operation
 */
public final class GraphQLRequest<T> {
    private final String document;
    private final Map<String, Object> variables;
    private final List<String> fragments;
    private final Class<T> modelClass;
    private final VariablesSerializer variablesSerializer;

    /**
     * Constructor for GraphQLRequest with
     * specification for type of API call.
     * @param document query document to process
     * @param modelClass class instance of model
     *                   to operate on
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     */
    public GraphQLRequest(
            String document,
            Class<T> modelClass,
            VariablesSerializer variablesSerializer
    ) {
        this(document, new HashMap<>(), modelClass, variablesSerializer);
    }

    /**
     * Constructor for GraphQLRequest with
     * specification for type of API call.
     * @param document query document to process
     * @param variables variables to be added
     * @param modelClass class instance of model
     *                   to operate on
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     */
    public GraphQLRequest(
            String document,
            Map<String, Object> variables,
            Class<T> modelClass,
            VariablesSerializer variablesSerializer
    ) {
        this.document = document;
        this.variables = variables;
        this.fragments = new ArrayList<>();
        this.modelClass = modelClass;
        this.variablesSerializer = variablesSerializer;
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
    public GraphQLRequest<T> addVariable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    /**
     * Attaches a fragment.
     * @param fragment fragment
     * @return this query object for chaining
     */
    public GraphQLRequest<T> addFragment(String fragment) {
        fragments.add(fragment);
        return this;
    }

    /**
     * Returns the class of model that this request
     * is operating on.
     * @return the class of entity
     */
    public Class<T> getModelClass() {
        return modelClass;
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
