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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.annotations.InternalAmplifyApi;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;

/**
 * A Base AWS GraphQLOperation that also takes an apiName to allow LazyModel support
 * @param <R> The type of data contained in the GraphQLResponse.
 */
@InternalAmplifyApi
public abstract class AWSGraphQLOperation<R> extends GraphQLOperation<R> {
    private String apiName;

    /**
     * Constructs a new instance of a GraphQLOperation.
     *
     * @param graphQLRequest  A GraphQL request
     * @param responseFactory an implementation of ResponseFactory
     */
    public AWSGraphQLOperation(
            @NonNull GraphQLRequest<R> graphQLRequest,
            @NonNull GraphQLResponse.Factory responseFactory,
            @Nullable String apiName
    ) {
        super(graphQLRequest, responseFactory);
        this.apiName = apiName;
    }

    @Override
    protected GraphQLResponse<R> wrapResponse(String jsonResponse) throws ApiException {
        return buildResponse(jsonResponse);
    }

    // This method should be used in place of GraphQLOperation.wrapResponse. In order to pass
    // apiName, we had to stop using the default GraphQLResponse.Factory buildResponse method
    // as there was no place to inject api name for adding to LazyModel
    private GraphQLResponse<R> buildResponse(String jsonResponse) throws ApiException {
        if (!(responseFactory instanceof GsonGraphQLResponseFactory)) {
            throw new ApiException("Amplify encountered an error while deserializing an object. " +
                    "GraphQLResponse.Factory was not of type GsonGraphQLResponseFactory",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION);
        }

        try {
            return ((GsonGraphQLResponseFactory) responseFactory)
                    .buildResponse(getRequest(), jsonResponse, apiName);
        } catch (ClassCastException cce) {
            throw new ApiException("Amplify encountered an error while deserializing an object",
                    AmplifyException.TODO_RECOVERY_SUGGESTION);
        }
    }
}
