/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model.query.predicate;

import com.amplifyframework.core.model.query.QuerySortBy;
import com.amplifyframework.core.model.query.QuerySortOrder;

/**
 * Represents a property in a model with methods for chaining conditions.
 */
public final class QueryField {
    private final String fieldName;

    /**
     * Constructs a new QueryField for a given model property.
     * This would not be used by the developer but rather is called from the static factory method.
     * @param fieldName the model property this QueryField represents
     */
    private QueryField(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Public factory method to create a new QueryField from the name of the property in the model.
     * @param fieldName the model property this QueryField represents
     * @return a new QueryField which represents the given model property
     */
    public static QueryField field(String fieldName) {
        return new QueryField(fieldName);
    }

    /**
     * Generates a new equality comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the equality condition
     */
    public QueryPredicateOperation<Object> eq(Object value) {
        return new QueryPredicateOperation<>(fieldName, new EqualQueryOperator(value));
    }

    /**
     * Generates a new not equals comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the not equal condition
     */
    public QueryPredicateOperation<Object> ne(Object value) {
        return new QueryPredicateOperation<>(fieldName, new NotEqualQueryOperator(value));
    }

    /**
     * Generates a new greater or equal comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @param <T> Comparable data type of the field
     * @return an operation object representing the greater or equal condition
     */
    public <T extends Comparable<T>> QueryPredicateOperation<T> ge(T value) {
        return new QueryPredicateOperation<>(fieldName, new GreaterOrEqualQueryOperator<>(value));
    }

    /**
     * Generates a new greater than comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @param <T> Comparable data type of the field
     * @return an operation object representing the greater than condition
     */
    public <T extends Comparable<T>> QueryPredicateOperation<T> gt(T value) {
        return new QueryPredicateOperation<>(fieldName, new GreaterThanQueryOperator<>(value));
    }

    /**
     * Generates a new less or equal comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @param <T> Comparable data type of the field
     * @return an operation object representing the less or equal condition
     */
    public <T extends Comparable<T>> QueryPredicateOperation<T> le(T value) {
        return new QueryPredicateOperation<>(fieldName, new LessOrEqualQueryOperator<>(value));
    }

    /**
     * Generates a new less than comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @param <T> Comparable data type of the field
     * @return an operation object representing the less than condition
     */
    public <T extends Comparable<T>> QueryPredicateOperation<T> lt(T value) {
        return new QueryPredicateOperation<>(fieldName, new LessThanQueryOperator<>(value));
    }

    /**
     * Generates a new between comparison object to compare this field to the specified range of values.
     * @param start the value to be used for the start of the range
     * @param end the value to be used for the end of the range
     * @param <T> Comparable data type of the field
     * @return an operation object representing the between condition
     */
    public <T extends Comparable<T>> QueryPredicateOperation<T> between(T start, T end) {
        return new QueryPredicateOperation<>(fieldName, new BetweenQueryOperator<>(start, end));
    }

    /**
     * Generates a new beginsWith comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the beginsWith condition
     */
    public QueryPredicateOperation<String> beginsWith(String value) {
        return new QueryPredicateOperation<>(fieldName, new BeginsWithQueryOperator(value));
    }

    /**
     * Generates a new contains comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the contains condition
     */
    public QueryPredicateOperation<String> contains(String value) {
        return new QueryPredicateOperation<>(fieldName, new ContainsQueryOperator(value));
    }

    /**
     * Generates a new sort object specifying a field that should be sorted in ascending order for a query.
     *
     * @return a QuerySortBy object, representing a field that should be sorted in ascending order for a query.
     */
    public QuerySortBy ascending() {
        return new QuerySortBy(fieldName, QuerySortOrder.ASCENDING);
    }

    /**
     * Generates a new sort object specifying a field that should be sorted in descending order for a query.
     *
     * @return a QuerySortBy object, representing a field that should be sorted in descending order for a query.
     */
    public QuerySortBy descending() {
        return new QuerySortBy(fieldName, QuerySortOrder.DESCENDING);
    }
}
