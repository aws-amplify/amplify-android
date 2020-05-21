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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.Page;
import com.amplifyframework.core.model.Model;

final class AppSyncPage<T extends Model> extends Page<T> {
    private final GraphQLRequest<Page<T>> requestForNextPage;
    private final Iterable<T> items;

    AppSyncPage(@NonNull Iterable<T> items, @NonNull GraphQLRequest<Page<T>> request) {
        this.requestForNextPage = request;
        this.items = items;
    }

    @Override
    public Iterable<T> getItems() {
        return items;
    }

    @Override
    public boolean hasNextPage() {
        return requestForNextPage != null;
    }

    @Override
    public GraphQLRequest<Page<T>> getRequestForNextPage() {
        return requestForNextPage;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        AppSyncPage<?> page = (AppSyncPage<?>) thatObject;

        return ObjectsCompat.equals(items, page.items) &&
                ObjectsCompat.equals(requestForNextPage, page.requestForNextPage);
    }

    @Override
    public int hashCode() {
        int result = items.hashCode();
        result = 31 * result + (requestForNextPage != null ? requestForNextPage.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GraphQLResponse.Error{" +
                "items=\'" + items + "\'" +
                ", requestForNextPage=\'" + requestForNextPage + "\'" +
                '}';
    }
}
