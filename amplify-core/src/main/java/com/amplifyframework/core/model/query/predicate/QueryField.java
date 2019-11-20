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

/**
 * Represents a property in a model with methods for chaining conditions.
 */
public final class QueryField {
    private String fieldName;

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
     * Generates a new beginsWith comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the beginsWith condition
     */
    public QueryPredicateOperation beginsWith(Object value) {
        return new QueryPredicateOperation(fieldName, QueryOperatorFactory.beginsWith(value));
    }

    /**
     * Generates a new between comparison object to compare this field to the specified range of values.
     * @param start the value to be used for the start of the range
     * @param end the value to be used for the end of the range
     * @return an operation object representing the between condition
     */
    public QueryPredicateOperation between(Object start, Object end) {
        return new QueryPredicateOperation(fieldName, QueryOperatorFactory.between(start, end));
    }

    /**
     * Generates a new contains comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the contains condition
     */
    public QueryPredicateOperation contains(Object value) {
        return new QueryPredicateOperation(fieldName, QueryOperatorFactory.contains(value));
    }

    /**
     * Generates a new equality comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the equality condition
     */
    public QueryPredicateOperation eq(Object value) {
        return new QueryPredicateOperation(fieldName, QueryOperatorFactory.equalTo(value));
    }

    /**
     * Generates a new greater or equal comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the greater or equal condition
     */
    public QueryPredicateOperation ge(Object value) {
        return new QueryPredicateOperation(fieldName, QueryOperatorFactory.greaterOrEqual(value));
    }

    /**
     * Generates a new greater than comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the greater than condition
     */
    public QueryPredicateOperation gt(Object value) {
        return new QueryPredicateOperation(fieldName, QueryOperatorFactory.greaterThan(value));
    }

    /**
     * Generates a new less or equal comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the less or equal condition
     */
    public QueryPredicateOperation le(Object value) {
        return new QueryPredicateOperation(fieldName, QueryOperatorFactory.lessOrEqual(value));
    }

    /**
     * Generates a new less than comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the less than condition
     */
    public QueryPredicateOperation lt(Object value) {
        return new QueryPredicateOperation(fieldName, QueryOperatorFactory.lessThan(value));
    }

    /**
     * Generates a new not equals comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the not equal condition
     */
    public QueryPredicateOperation ne(Object value) {
        return new QueryPredicateOperation(fieldName, QueryOperatorFactory.notEqual(value));
    }
}
