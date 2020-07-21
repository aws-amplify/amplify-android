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

/**
 * A data structure that provides a query construction mechanism that consolidates all query-related
 * options (e.g. predicates, pagination, etc) and allows consumers to build queries in a fluent way.
 */
public final class QueryOptions {
    private QueryPredicate queryPredicate;
    private QueryPaginationInput paginationInput;
    private List<QuerySortBy> sortBy;

    /**
     * This class should be created using the factory methods such as {@link Where#matchesAll()}
     * and {@link Where#matches(QueryPredicate)}.
     */
    QueryOptions(
            @Nullable QueryPredicate queryPredicate,
            @Nullable QueryPaginationInput paginationInput,
            @Nullable List<QuerySortBy> sortBy
    ) {
        this.queryPredicate = queryPredicate == null ? QueryPredicates.all() : queryPredicate;
        this.paginationInput = paginationInput;
        this.sortBy = sortBy;
    }

    QueryOptions() {
        this(null, null, null);
    }

    /**
     * Update the current options with a given {@code queryPredicate}.
     *
     * @param queryPredicate predicate.
     * @return current options with an updated {@code queryPredicate}.
     */
    @NonNull
    public QueryOptions matches(@NonNull final QueryPredicate queryPredicate) {
        this.queryPredicate = queryPredicate;
        return this;
    }

    /**
     * Updates the current options with a given {@code paginationInput}.
     *
     * @param paginationInput pagination information.
     * @return current options with an updated {@code paginationInput}.
     * @see Page#startingAt(int)
     * @see Page#firstPage()
     */
    @NonNull
    public QueryOptions paginated(@NonNull final QueryPaginationInput paginationInput) {
        this.paginationInput = paginationInput;
        return this;
    }

    /**
     * Updates the current options with a given {@code sortBy}.
     *
     * @param sortBy sorting information
     * @return current options with an updated {@code sortBy}.
     */
    public QueryOptions sorted(@NonNull final QuerySortBy... sortBy) {
        this.sortBy = Arrays.asList(sortBy);
        return this;
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
     * Returns the {@code paginationInput} property.
     * @return the {@code paginationInput} property.
     */
    @Nullable
    public QueryPaginationInput getPaginationInput() {
        return paginationInput;
    }

    /**
     * Returns the {@code sortInput} property.
     * @return the {@code sortInput} property.
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
        if (!(object instanceof QueryOptions)) {
            return false;
        }
        QueryOptions that = (QueryOptions) object;
        return ObjectsCompat.equals(queryPredicate, that.queryPredicate) &&
                ObjectsCompat.equals(paginationInput, that.paginationInput) &&
                ObjectsCompat.equals(sortBy, that.sortBy);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(queryPredicate, paginationInput, sortBy);
    }

    @NonNull
    @Override
    public String toString() {
        return "QueryOptions{" +
                "queryPredicate=" + queryPredicate +
                ", paginationInput=" + paginationInput +
                ", sortBy=" + sortBy +
                '}';
    }
}
