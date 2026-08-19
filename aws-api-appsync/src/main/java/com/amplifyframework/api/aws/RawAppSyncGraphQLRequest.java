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

import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.SimpleGraphQLRequest;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

/**
 * A {@link GraphQLRequest} built from a raw GraphQL document, which can specify the
 * {@link AuthorizationType} used to authorize it.
 *
 * <p>This is the equivalent of {@link SimpleGraphQLRequest} for APIs that define more than one
 * authorization mode. {@link SimpleGraphQLRequest} always uses the API's default authorization type;
 * this request type lets you select any of the modes configured for the API, per request:</p>
 *
 * <pre>
 * Amplify.API.query(
 *     new RawAppSyncGraphQLRequest&lt;&gt;(
 *         "query GetByUser { getByUser }",
 *         String.class,
 *         serializer,
 *         AuthorizationType.AMAZON_COGNITO_USER_POOLS
 *     ),
 *     response -&gt; { /* ... *&#47; },
 *     error -&gt; { /* ... *&#47; }
 * );
 * </pre>
 *
 * <p>Because a raw document has no associated model schema, a request of this type does not
 * participate in automatic multi-auth resolution from {@code @auth} rules. The authorization type
 * given here is used as-is.</p>
 *
 * @param <R> Type of R, the data contained in the GraphQLResponse expected from this request
 */
public final class RawAppSyncGraphQLRequest<R> extends GraphQLRequest<R> implements AuthorizedGraphQLRequest {
    private final String document;
    private final Map<String, Object> variables;
    private final AuthorizationType authorizationType;

    /**
     * Constructor for RawAppSyncGraphQLRequest.
     * @param document document String for request
     * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     * @param authorizationType the authorization type to use for this request, or null for the API default
     */
    public RawAppSyncGraphQLRequest(
            String document,
            Type responseType,
            VariablesSerializer variablesSerializer,
            @Nullable AuthorizationType authorizationType
    ) {
        this(document, Collections.emptyMap(), responseType, variablesSerializer, authorizationType);
    }

    /**
     * Constructor for RawAppSyncGraphQLRequest.
     * @param document query document to process
     * @param variables variables to be added
     * @param responseType Type of R, the data contained in the GraphQLResponse expected from this request
     * @param variablesSerializer an object which can take a map of variables and serialize it properly
     * @param authorizationType the authorization type to use for this request, or null for the API default
     */
    public RawAppSyncGraphQLRequest(
            String document,
            Map<String, Object> variables,
            Type responseType,
            VariablesSerializer variablesSerializer,
            @Nullable AuthorizationType authorizationType
    ) {
        super(responseType, variablesSerializer);
        this.document = document;
        this.variables = variables;
        this.authorizationType = authorizationType;
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
    @Nullable
    public AuthorizationType getAuthorizationType() {
        return this.authorizationType;
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

        RawAppSyncGraphQLRequest<?> that = (RawAppSyncGraphQLRequest<?>) object;
        return ObjectsCompat.equals(document, that.document) &&
                ObjectsCompat.equals(variables, that.variables) &&
                ObjectsCompat.equals(authorizationType, that.authorizationType);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(super.hashCode(), document, variables, authorizationType);
    }

    @Override
    public String toString() {
        return "RawAppSyncGraphQLRequest{" +
                "document=\'" + document + "\'" +
                ", variables=\'" + variables + "\'" +
                ", authorizationType=\'" + authorizationType + "\'" +
                "}";
    }
}
