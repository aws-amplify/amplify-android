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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.datastore.DataStoreCategoryBehavior;

import java.util.Objects;

/**
 * A data structure representing a model field and an order to sort by (ascending or descending), used to specify the
 * order of results from {@link DataStoreCategoryBehavior#query(Class, QueryOptions, Consumer, Consumer)}.
 *
 * The preferred way to create a QuerySortBy is with the {@link QueryField#ascending()} and
 * {@link QueryField#descending()} helper methods (e.g. Todo.DESCRIPTION.ascending() or Todo.DESCRIPTION.descending())
 */
public final class QuerySortBy {
    private final String field;
    private final QuerySortOrder sortOrder;

    /**
     * Constructor for {@code QuerySortBy}.
     *
     * @param field name of field to sort by.
     * @param sortOrder order to sort by, either ASCENDING or DESCENDING.
     */
    public QuerySortBy(@NonNull String field, @NonNull QuerySortOrder sortOrder) {
        this.field = Objects.requireNonNull(field);
        this.sortOrder = Objects.requireNonNull(sortOrder);
    }

    /**
     * Returns the field to sort by.
     * @return the field to sort by.
     */
    public String getField() {
        return field;
    }

    /**
     * Returns the order to sort by, either ASCENDING or DESCENDING.
     * @return the order to sort by, either ASCENDING or DESCENDING.
     */
    public QuerySortOrder getSortOrder() {
        return sortOrder;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        QuerySortBy that = (QuerySortBy) object;
        return ObjectsCompat.equals(field, that.field) &&
                ObjectsCompat.equals(sortOrder, that.sortOrder);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(field, sortOrder);
    }

    @Override
    public String toString() {
        return "QuerySortBy{" +
                "field='" + field + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
}
