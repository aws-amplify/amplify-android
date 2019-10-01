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

package com.amplifyframework.api;

/**
 * Abstract class that a plugin implementation of REST API category
 * would extend. This includes the client behavior dictated by
 * {@link GraphQLApiCategoryBehavior} and {@link ApiPlugin}.
 */
public abstract class GraphQLApiPlugin<C, E> extends ApiPlugin<C, E> implements GraphQLApiCategoryBehavior {
    @Override
    public final ApiType getApiType() {
        return ApiType.GRAPHQL;
    }
}
