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

public final class DataStoreError<M extends Model> {
    private final GraphQLResponse.Error error;
    private final M local;
    private final M remote;

    public DataStoreError(
            @NonNull GraphQLResponse.Error error,
            @Nullable M local,
            @Nullable M remote
    ) {
        this.error = Objects.requireNonNull(error);
        this.local = local;
        this.remote = remote;
    }

    @NonNull
    public GraphQLResponse.Error getError() {
        return error;
    }

    @Nullable
    public M getLocal() {
        return local;
    }

    @Nullable
    public M getRemote() {
        return remote;
    }
}
