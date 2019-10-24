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

import com.amplifyframework.api.ApiObservable;
import com.amplifyframework.api.graphql.GraphQLQuery;
import com.amplifyframework.core.stream.Observer;
import com.amplifyframework.core.stream.SubscriptionToken;

import okhttp3.OkHttpClient;

/**
 * An observable created by a GraphQL subscription.
 * @param <T> Casted type of GraphQL response
 */
public final class AWSGraphQLObservable<T> extends ApiObservable<T> {
    private final String endpoint;
    private final OkHttpClient client;
    private final GraphQLQuery query;

    /**
     * Constructs a new AWSGraphQLObservable.
     * @param endpoint API endpoint being hit
     * @param client OkHttp client being used to hit the endpoint
     * @param query GraphQL subscription being queried
     */
    AWSGraphQLObservable(String endpoint,
                         OkHttpClient client,
                         GraphQLQuery query) {
        this.endpoint = endpoint;
        this.client = client;
        this.query = query;
    }

    @Override
    public SubscriptionToken subscribe(Observer<T> observer) {
        SubscriptionToken token = SubscriptionToken.create();
        //TODO: implement subscription
        return token;
    }

    @Override
    public void unsubscribe(SubscriptionToken token) {
        //TODO: implement subscription cancel
    }
}
