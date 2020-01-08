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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiOperation;

/**
 * A GraphQLOperation is an API operation which returns a GraphQLResponse.
 * @param <T> The type of data contained in the GraphQLResponse.
 */
public abstract class GraphQLOperation<T> extends ApiOperation<GraphQLRequest<T>> {
    private final GraphQLResponse.Factory responseFactory;
    private final Class<T> classToCast;

    /**
     * Constructs a new instance of a GraphQLOperation.
     * @param graphQlRequest A GraphQL request
     * @param responseFactory an implementation of ResponseFactory
     */
    public GraphQLOperation(
            @NonNull GraphQLRequest<T> graphQlRequest,
            @NonNull GraphQLResponse.Factory responseFactory
    ) {
        super(graphQlRequest);
        this.responseFactory = responseFactory;
        this.classToCast = graphQlRequest.getModelClass();
    }

    /**
     * Converts a response json string containing a single object to a formatted
     * {@link GraphQLResponse} object that a response consumer can receive.
     * @param jsonResponse json response from API to be converted
     * @return wrapped response object
     * @throws ApiException If the class provided mismatches the data
     */
    protected final GraphQLResponse<T> wrapSingleResultResponse(String jsonResponse) throws ApiException {
        try {
            return responseFactory.buildSingleItemResponse(jsonResponse, classToCast);
        } catch (ClassCastException cce) {
            throw new ApiException("Amplify encountered an error while deserializing an object",
                    AmplifyException.TODO_RECOVERY_SUGGESTION);
        }
    }

    /**
     * Converts a response json string containing a list of objects to a formatted
     * {@link GraphQLResponse} object that a response consumer can receive.
     * @param jsonResponse json response from API to be converted
     * @return wrapped response object
     * @throws ApiException If the class provided mismatches the data
     */
    protected final GraphQLResponse<Iterable<T>> wrapMultiResultResponse(String jsonResponse) throws ApiException {
        try {
            return responseFactory.buildSingleArrayResponse(jsonResponse, classToCast);
        } catch (ClassCastException cce) {
            throw new ApiException("Amplify encountered an error while deserializing an object",
                    AmplifyException.TODO_RECOVERY_SUGGESTION);
        }
    }

    /**
     * Gets the casting class.
     * @return Class to cast.
     */
    protected final Class<T> getClassToCast() {
        return classToCast;
    }
}
