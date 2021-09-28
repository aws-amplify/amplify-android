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

package com.amplifyframework.core.model.query;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A data structure that provides a query construction mechanism that consolidates query-related
 * options (e.g. predicates and sorting) and allows consumers to build queries in a fluent way.
 */
public final class ObserveQueryOptions {
    private final QueryPredicate queryPredicate;
    private final List<QuerySortBy> sortBy;

    /**
     * This class should be created using the factory methods such as {@link Where#matchesAll()}
     * and {@link Where#matches(QueryPredicate)}.
     * @param queryPredicate query predicate.
     * @param sortBy sort by.
     */
    public ObserveQueryOptions(@Nullable QueryPredicate queryPredicate,
                                @Nullable List<QuerySortBy> sortBy) {
        this.queryPredicate = queryPredicate == null ? QueryPredicates.all() : queryPredicate;
        this.sortBy = sortBy;
    }

    /***
     * Observe query options.
     */
    public ObserveQueryOptions() {
        this(null, null);
    }

    /**
     * Returns an immutable copy of the current query options with the given {@code queryPredicate}.
     *
     * @param queryPredicate predicate.
     * @return current options with an updated {@code queryPredicate}.
     */
    @NonNull
    public ObserveQueryOptions matches(@NonNull final QueryPredicate queryPredicate) {
        return new ObserveQueryOptions(Objects.requireNonNull(queryPredicate), sortBy);
    }

    /**
     * Returns an immutable copy of the current query options with the given {@code sortBy} arguments.
     *
     * @param querySortBy sorting information.
     * @return current options with an updated {@code sortBy}.
     */
    public ObserveQueryOptions sorted(@NonNull final QuerySortBy... querySortBy) {
        return new ObserveQueryOptions(queryPredicate, Arrays.asList(Objects.requireNonNull(querySortBy)));
    }

    /**
     * Returns the {@code queryPredicate} property.
     * @return the {@code queryPredicate} property.
     */
    @NonNull
    public QueryPredicate getQueryPredicate() {
        return queryPredicate;
    }

    /**
     * Returns the {@code sortBy} property.
     * @return the {@code sortBy} property.
     */
    @Nullable
    public List<QuerySortBy> getSortBy() {
        return sortBy;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ObserveQueryOptions)) {
            return false;
        }
        ObserveQueryOptions that = (ObserveQueryOptions) object;
        return ObjectsCompat.equals(queryPredicate, that.queryPredicate) &&
                ObjectsCompat.equals(sortBy, that.sortBy);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(queryPredicate, sortBy);
    }

    @NonNull
    @Override
    public String toString() {
        return "QueryOptions{" +
                "queryPredicate=" + queryPredicate +
                ", sortBy=" + sortBy +
                '}';
    }
}
