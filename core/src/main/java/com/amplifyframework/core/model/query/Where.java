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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.PrimaryKey;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
        return new QueryOptions(Objects.requireNonNull(queryPredicate), null, null);
    }

    /**
     * This method is @Deprecated please use {@link #identifier(Class, Serializable)}}
     * Factory method that builds the options with a predicate matching the model id and the
     * pagination set to the first result only.
     * @deprecated This method is @Deprecated please use {@link #identifier(Class, Serializable)}}
     * @param modelId model identifier.
     * @return options with proper predicate and pagination to match a model by its id.
     */
    @Deprecated
    public static QueryOptions id(@NonNull final String modelId) {
        final QueryField idField = QueryField.field(PrimaryKey.fieldName());
        return matches(idField.eq(Objects.requireNonNull(modelId)))
                .paginated(Page.firstResult());
    }

    /**
     * Factory method that builds the options with a predicate matching the primary key and the
     * pagination set to the first result only.
     *
     * @param itemClass model class.
     * @param modelPrimaryKey model identifier.
     * @param <T> Extends Model.
     * @return options with proper predicate and pagination to match a model by its id.
     * @throws AmplifyException Throws AmplifyException.
     */
    public static <T extends Model> QueryOptions identifier(@NonNull Class<T> itemClass,
                                                           @NonNull final Serializable modelPrimaryKey)
            throws AmplifyException {
        final ModelSchema schema = ModelSchema.fromModelClass(itemClass);
        final List<String> primaryKeyList = schema.getPrimaryIndexFields();
        QueryOptions queryOptions = null;
        Iterator<String> pkField = primaryKeyList.listIterator();
        final QueryField idField = QueryField.field(itemClass.getSimpleName(), pkField.next());
        if (primaryKeyList.size() == 1 && !(modelPrimaryKey instanceof ModelIdentifier)) {
            queryOptions = matches(idField.eq(Objects.requireNonNull(modelPrimaryKey.toString())));
        } else {
            ModelIdentifier<?> primaryKey = (ModelIdentifier<?>) modelPrimaryKey;
            Iterator<?> sortKeyIterator = primaryKey.sortedKeys().listIterator();
            queryOptions = matches(idField.eq(Objects.requireNonNull(primaryKey.key())));
            while (sortKeyIterator.hasNext()) {
                queryOptions.matches(QueryField.field(itemClass.getSimpleName(), pkField.next())
                        .eq(Objects.requireNonNull(sortKeyIterator.next())));
            }
        }
        return queryOptions.paginated(Page.firstResult());
    }

    /**
     * Factory method that builds the options with the given {@link QueryPaginationInput}.
     *
     * @param paginationInput the pagination details
     * @return options with the given sortBy fields.
     */
    public static QueryOptions paginated(@NonNull final QueryPaginationInput paginationInput) {
        return new QueryOptions(null, Objects.requireNonNull(paginationInput), null);
    }

    /**
     * Factory method that builds the options with the given {@link QuerySortBy} arguments.
     *
     * @param sortBy a varargs list of QuerySortBy options, in the order in which they are to be applied.
     * @return options with the given sortBy fields.
     */
    public static QueryOptions sorted(@NonNull final QuerySortBy... sortBy) {
        return new QueryOptions(null, null, Arrays.asList(Objects.requireNonNull(sortBy)));
    }

    /**
     * Factory method that builds the options with the given {@link QueryPredicate and @link QuerySortBy} arguments.
     * @param queryPredicate the query conditions.
     * @param sortBy a varargs list of QuerySortBy options, in the order in which they are to be applied.
     * @return options with the given sortBy fields.
     */
    public static QueryOptions matchesAndSorts(final QueryPredicate queryPredicate, final List<QuerySortBy> sortBy) {
        return new QueryOptions(queryPredicate, null, sortBy);
    }
}
