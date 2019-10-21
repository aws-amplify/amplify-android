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

/**
 * Callback interface specific for GraphQL queries.
 * @param <T> type of data that was queried
 */
public interface GraphQLCallback<T> {
    /**
     * Callback for successful server response.
     * This does not guarantee that no error was encountered
     * by the server. Check for partial errors with
     * `response.hasErrors()`.
     * @param response response object with casted server
     *                 data and graphql errors list
     */
    void onResponse(Response<T> response);

    /**
     * Called when an exception was thrown.
     * @param exception exception thrown by server
     */
    void onError(Exception exception);
}
