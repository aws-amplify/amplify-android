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

package com.amplifyframework.api.okhttp;

import com.amplifyframework.api.graphql.GraphQLCallback;
import com.amplifyframework.api.graphql.GraphQLOperation;

/**
 * An operation to enqueue a GraphQL query to OkHttp client.
 * @param <T> Casted type of GraphQL query result
 */
public final class OkHttpGraphQLOperation<T> extends GraphQLOperation {
    private final GraphQLCallback<T> callback;

    /**
     * Constructs a new OkHttpGraphQLOperation.
     * @param callback callback to invoke when response is available
     */
    public OkHttpGraphQLOperation(GraphQLCallback<T> callback) {
        this.callback = callback;
    }

    @Override
    public void start() {
        //TODO: Move OkHttpApiPlugin.enqueue() to here
    }
}
