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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.Model;

import java.util.Objects;

/**
 * Contains error object from GraphQL server response from
 * an attempt to publish a mutation. This object will be passed
 * back to the user via {@link DataStoreErrorHandler} to be
 * appropriately handled by custom logic.
 * @param <M> type of model that caused publication error.
 */
public final class DataStoreError<M extends Model> {
    private final GraphQLResponse.Error error;
    private final M local;
    private final M remote;

    /**
     * Constructs a new error instance containing details regarding
     * a failed publication to the cloud and involved models.
     * @param error the error response from the server
     * @param local the local model pending mutation
     * @param remote the remote model if applicable
     */
    public DataStoreError(
            @NonNull GraphQLResponse.Error error,
            @Nullable M local,
            @Nullable M remote
    ) {
        this.error = Objects.requireNonNull(error);
        this.local = local;
        this.remote = remote;
    }

    /**
     * Gets the error from GraphQL server response.
     * @return the GraphQL response error.
     */
    @NonNull
    public GraphQLResponse.Error getError() {
        return error;
    }

    /**
     * Gets the local item that was pending mutation.
     * @return the local model.
     */
    @Nullable
    public M getLocal() {
        return local;
    }

    /**
     * Gets the remote item in the server if provided by the
     * error payload.
     * @return the remote model.
     */
    @Nullable
    public M getRemote() {
        return remote;
    }
}
