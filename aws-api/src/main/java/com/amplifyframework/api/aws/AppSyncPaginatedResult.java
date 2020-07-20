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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.PaginatedResult;

final class AppSyncPaginatedResult<T> extends PaginatedResult<T> {
    private final GraphQLRequest<PaginatedResult<T>> requestForNextResult;
    private final Iterable<T> items;

    AppSyncPaginatedResult(@NonNull Iterable<T> items,
                           @Nullable GraphQLRequest<PaginatedResult<T>> requestForNextResult) {
        this.requestForNextResult = requestForNextResult;
        this.items = items;
    }

    @Override
    public Iterable<T> getItems() {
        return items;
    }

    @Override
    public boolean hasNextResult() {
        return requestForNextResult != null;
    }

    @Override
    public GraphQLRequest<PaginatedResult<T>> getRequestForNextResult() {
        return requestForNextResult;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        AppSyncPaginatedResult<?> page = (AppSyncPaginatedResult<?>) thatObject;

        return ObjectsCompat.equals(items, page.items) &&
                ObjectsCompat.equals(requestForNextResult, page.requestForNextResult);
    }

    @Override
    public int hashCode() {
        int result = items.hashCode();
        result = 31 * result + (requestForNextResult != null ? requestForNextResult.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AppSyncPage{" +
                "items=\'" + items + "\'" +
                ", requestForNextPage=\'" + requestForNextResult + "\'" +
                '}';
    }
}
