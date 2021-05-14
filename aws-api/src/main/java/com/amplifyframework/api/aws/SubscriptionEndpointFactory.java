/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLResponse;

/**
 * Defines the contract for a factory class that creates instances of {@link SubscriptionEndpoint}.
 */
public interface SubscriptionEndpointFactory {
    /**
     * Given the necessart parameters, an implementation of this function should return an
     * instance of a class that implements the {@link SubscriptionEndpoint} interface.
     * @param apiConfiguration The API configuration.
     * @param responseFactory The appropriate reponse factory for the subscription endpoint.
     * @param authorizer A subscription authorizer to create the auth headers.
     * @return An instance of a class that implements {@link SubscriptionEndpoint}.
     * @throws ApiException If there is an error during creation of the subscription endpoint.
     */
    SubscriptionEndpoint create(
        @NonNull ApiConfiguration apiConfiguration,
        @NonNull GraphQLResponse.Factory responseFactory,
        @NonNull SubscriptionAuthorizer authorizer
    ) throws ApiException;
}
