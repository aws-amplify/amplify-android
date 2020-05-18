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

/**
 * A data structure that provides a query construction mechanism that consolidates all query-related
 * options (e.g. predicates, pagination, etc) and allows consumers to build queries in a fluent way.
 */
public final class QueryOptions {
    private QueryPredicate queryPredicate;
    private QueryPaginationInput paginationInput;

    /**
     * This class should be created using the factory methods such as {@link Where#matchesAll()}
     * and {@link Where#matches(QueryPredicate)}.
     */
    QueryOptions(
            @Nullable QueryPredicate queryPredicate,
            @Nullable QueryPaginationInput paginationInput
    ) {
        this.queryPredicate = queryPredicate;
        this.paginationInput = paginationInput;
    }

    QueryOptions() {
        this(null, null);
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
     * Returns the {@code queryPredicate} property.
     * @return the {@code queryPredicate} property.
     */
    @Nullable
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof QueryOptions)) {
            return false;
        }
        QueryOptions that = (QueryOptions) object;
        return ObjectsCompat.equals(queryPredicate, that.queryPredicate) &&
                ObjectsCompat.equals(paginationInput, that.paginationInput);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(queryPredicate, paginationInput);
    }

    @Override
    public String toString() {
        return "QueryOptions{" +
                "queryPredicate=" + queryPredicate +
                ", paginationInput=" + paginationInput +
                '}';
    }

}
