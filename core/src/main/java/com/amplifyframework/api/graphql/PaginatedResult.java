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

package com.amplifyframework.api.graphql;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Represents a page of results returned from an API.  Specifically, contains the list of non-null items in the page,
 * as well as a GraphQLRequest which can be used to obtain the next page.
 *
 * @param <T> Type of the items in the list.
 */
public final class PaginatedResult<T> implements Iterable<T> {

    private final GraphQLRequest<PaginatedResult<T>> requestForNextResult;
    private final Iterable<T> items;

    /**
     * Creates a PaginatedResult.
     *
     * @param items                Iterable&lt;T&gt; of the items from the response.
     * @param requestForNextResult a GraphQLRequest to obtain the next page of results, or null if no next page.
     */
    public PaginatedResult(@NonNull Iterable<T> items,
                           @Nullable GraphQLRequest<PaginatedResult<T>> requestForNextResult) {
        this.requestForNextResult = requestForNextResult;
        this.items = StreamSupport.stream(items.spliterator(), false)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of items obtained from an API query.
     *
     * @return Iterable of Model objects
     */
    public Iterable<T> getItems() {
        return items;
    }

    /**
     * Returns whether a subsequent page of results is available from the API.
     *
     * @return boolean whether a subsequent page is available
     */
    public boolean hasNextResult() {
        return requestForNextResult != null;
    }

    /**
     * Returns a GraphQLRequest which can be used to obtain the next page of results.  The request itself is identical
     * to the GraphQLRequest used to obtain the current page of results, except that pagination metadata is added.
     *
     * @return GraphQLRequest to obtain the next page of results
     */
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

        PaginatedResult<?> paginatedResult = (PaginatedResult<?>) thatObject;
        return ObjectsCompat.equals(requestForNextResult, paginatedResult.requestForNextResult) &&
                ObjectsCompat.equals(items, paginatedResult.items);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(requestForNextResult, items);
    }

    @Override
    public String toString() {
        return "PaginatedResult{" +
                "requestForNextResult=" + requestForNextResult +
                ", items=" + items +
                '}';
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
    
    @Override
    public void forEach(@NonNull Consumer<? super T> action) {
        items.forEach(action);
    }

    @NonNull
    @Override
    public Spliterator<T> spliterator() {
        return items.spliterator();
    }
}
