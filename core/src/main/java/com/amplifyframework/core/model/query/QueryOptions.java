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

import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import static com.amplifyframework.core.model.query.QueryPaginationInput.firstResult;

/**
 * A data structure that provides a query construction mechanism that consolidates all query-related
 * options (e.g. predicates, pagination, etc) and allows consumers to build queries in a fluent way.
 */
public final class QueryOptions {

    private QueryPredicate queryPredicate;
    private QueryPaginationInput paginationInput;

    /**
     * This class should be created using the factory methods {@link #all()} and {@link #where(QueryPredicate)}.
     */
    private QueryOptions() {}

    /**
     * Factory method that builds a default <code>QueryOptions</code> that has no predicate and
     * no pagination set (i.e. <em>all</em> results).
     * @return default QueryOptions
     */
    public static QueryOptions all() {
        return new QueryOptions();
    }

    /**
     *
     * @param queryPredicate
     * @return
     */
    public static QueryOptions where(QueryPredicate queryPredicate) {
        final QueryOptions options = new QueryOptions();
        options.queryPredicate = queryPredicate;
        return options;
    }

    /**
     *
     * @param modelId
     * @return
     */
    public static QueryOptions byId(String modelId) {
        return where(QueryField.field("id").eq(modelId)).paginated(firstResult());
    }

    /**
     *
     * @param paginationInput
     * @return
     */
    public QueryOptions paginated(QueryPaginationInput paginationInput) {
        this.paginationInput = paginationInput;
        return this;
    }

    public QueryPredicate getQueryPredicate() {
        return queryPredicate;
    }

    public QueryPaginationInput getPaginationInput() {
        return paginationInput;
    }

}
