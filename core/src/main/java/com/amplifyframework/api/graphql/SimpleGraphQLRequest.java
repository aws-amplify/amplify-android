/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.HashMap;
import java.util.Map;

/**
 * A basic implementation of GraphQLRequest, which takes a document String, and variables Map.
 * @param <R> Type of R, the data contained in the GraphQLResponse expected from this request
 */
public final class SimpleGraphQLRequest<R> extends GraphQLRequest<R> {
    private final String document;
    private final Map<String, Object> variables;

    /**
     * Constructor for SimpleGraphQLRequest.
     * @param document document String for request
     * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     */
    public SimpleGraphQLRequest(
            String document,
            Type responseType,
            VariablesSerializer variablesSerializer
    ) {
        this(document, new HashMap<>(), responseType, variablesSerializer);
    }

    /**
     * Constructor for SimpleGraphQLRequest.
     * @param document query document to process
     * @param variables variables to be added
     * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     */
    public SimpleGraphQLRequest(
            String document,
            Map<String, Object> variables,
            Type responseType,
            VariablesSerializer variablesSerializer
    ) {
        super(responseType, variablesSerializer);
        this.variables = variables;
        this.document = document;
    }

    /**
     * Copy constructor for a SimpleGraphQLRequest.
     * @param request SimpleGraphQLRequest to be copied
     * @param <R> The type of data contained in the GraphQLResponse expected from this request.
     */
    public <R> SimpleGraphQLRequest(SimpleGraphQLRequest<R> request) {
        super(request);
        this.variables = new HashMap<>(request.variables);
        this.document = request.document;
    }

    /**
     * Returns a copy of the SimpleGraphQLRequest instance.
     * @param <R> The type of data contained in the GraphQLResponse expected from this request.
     * @return Copy of the SimpleGraphQLRequest object
     */
    @Override
    public <R> SimpleGraphQLRequest<R> copy() {
        return new SimpleGraphQLRequest<R>(this);
    }

    @Override
    public String getQuery() {
        return this.document;
    }

    @Override
    public Map<String, Object> getVariables() {
        return this.variables;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        if (!super.equals(object)) {
            return false;
        }

        SimpleGraphQLRequest<?> that = (SimpleGraphQLRequest<?>) object;
        return ObjectsCompat.equals(document, that.document) &&
                ObjectsCompat.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(super.hashCode(), document, variables);
    }
}
