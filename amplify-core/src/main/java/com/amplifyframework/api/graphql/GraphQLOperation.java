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

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiOperation;
import com.amplifyframework.core.ResultListener;

/**
 * A GraphQLOperation is an API operation which returns a GraphQLResponse.
 * @param <T> The type of data contained in the GraphQLResponse.
 */
public abstract class GraphQLOperation<T> extends ApiOperation<GraphQLRequest> {
    private final GraphQLResponse.Factory responseFactory;
    private final Class<T> classToCast;
    private final ResultListener<GraphQLResponse<T>> responseListener;

    /**
     * Constructs a new instance of a GraphQLOperation.
     * @param graphQlRequest A GraphQL request
     * @param responseFactory an implementation of ResponseFactory
     * @param classToCast class to cast the response to
     * @param responseListener Listens to the outputs of the operation
     */
    public GraphQLOperation(
            @NonNull GraphQLRequest graphQlRequest,
            @NonNull GraphQLResponse.Factory responseFactory,
            @Nullable Class<T> classToCast,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener) {
        super(graphQlRequest);
        this.responseFactory = responseFactory;
        this.classToCast = classToCast;
        this.responseListener = responseListener;
    }

    protected Class<T> getClassToCast() {
        return classToCast;
    }

    /**
     * Gets the listener that was specified when the operation was requested.
     * @return The listener who will receive outputs of the operation
     */
    protected final ResultListener<GraphQLResponse<T>> responseListener() {
        return responseListener;
    }

    /**
     * Check if a response listener is registered.
     * @return true if a response listener exists, false otherwise
     */
    protected final boolean hasResponseListener() {
        return responseListener != null;
    }

    /**
     * Converts a response json string to formatted {@link
     * GraphQLResponse} object that a response listener can receive.
     * @param jsonResponse json response from API to be converted
     * @return wrapped response object
     */
    protected final GraphQLResponse<T> wrapResponse(String jsonResponse) {
        try {
            return responseFactory.buildResponse(jsonResponse, classToCast);
        } catch (ClassCastException cce) {
            throw new ApiException.ObjectSerializationException();
        }
    }
}
