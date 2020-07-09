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

import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import java.util.Arrays;

/**
 * Query DSL for query predicates.
 */
public final class Where {

    private Where() {}

    /**
     * Factory method that builds a default <code>QueryOptions</code> that has no predicate and
     * no pagination set (i.e. <em>all</em> results).
     * @return default QueryOptions
     */
    public static QueryOptions matchesAll() {
        return new QueryOptions();
    }

    /**
     * Factory method that builds the options with the given {@link QueryPredicate}.
     * @param queryPredicate the query conditions.
     * @return options with a given predicate.
     */
    public static QueryOptions matches(@NonNull final QueryPredicate queryPredicate) {
        return new QueryOptions(queryPredicate, null, null);
    }

    /**
     * Factory method that builds the options with a predicate matching the model id and the
     * pagination set to the first result only.
     *
     * @param modelId model identifier.
     * @return options with proper predicate and pagination to match a model by its id.
     */
    public static QueryOptions id(@NonNull final String modelId) {
        return matches(QueryField.field("id").eq(modelId)).paginated(Page.firstResult());
    }

    /**
     * Factory method that builds the options with the given {@link QueryPaginationInput}.
     *
     * @param paginationInput the pagination details
     * @return options with the given sortBy fields.
     */
    public static QueryOptions paginated(@NonNull final QueryPaginationInput paginationInput) {
        return new QueryOptions(null, paginationInput, null);
    }

    /**
     * Factory method that builds the options with the given {@link QuerySortBy} arguments.
     *
     * @param sortBy a varargs list of QuerySortBy options, in the order in which they are to be applied.
     * @return options with the given sortBy fields.
     */
    public static QueryOptions sorted(@NonNull final QuerySortBy... sortBy) {
        return new QueryOptions(null, null, Arrays.asList(sortBy));
    }
}
