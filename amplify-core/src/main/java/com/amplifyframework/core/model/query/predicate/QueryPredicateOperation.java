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
 * Represents an individual comparison operation on a model field.
 */
public class QueryPredicateOperation implements QueryPredicate {
    private String field;
    private QueryOperator operator;

    /**
     * Create a new comparison operation with the field to examine and the comparison to perform on it.
     * @param field the name of the Java property in the model representing the field to perform this comparison on
     * @param operator the comparison to perform on it
     */
    public QueryPredicateOperation(String field, QueryOperator operator) {
        this.field = field;
        this.operator = operator;
    }

    /**
     * Get the name of the field in the Java model to perform this comparison on.
     * @return the name of the field in the Java model to perform this comparison on
     */
    public String field() {
        return field;
    }

    /**
     * Returns the comparison operation to perform on this field.
     * This includes both the type (e.g. EQUAL) and the value to compare the field to (e.g. "ABC")
     * @return the comparison operation to perform on this field
     */
    public QueryOperator operator() {
        return operator;
    }

    /**
     * Return a group connecting this operation with another group/operation with an AND type.
     * @param predicate the group/operation to connect to
     * @return a group connecting this operation with another group/operation with an AND type
     */
    public QueryPredicateGroup and(QueryPredicate predicate) {
        return null;
    }

    /**
     * Return a group connecting this operation with another group/operation with an OR type.
     * @param predicate the group/operation to connect to
     * @return a group connecting this operation with another group/operation with an OR type
     */
    public QueryPredicateGroup or(QueryPredicate predicate) {
        return null;
    }

    /**
     * Return a group negating the given operation.
     * @param predicate the operation to negate
     * @return a group negating the given operation
     */
    public static QueryPredicateGroup not(QueryPredicateOperation predicate) {
        return null;
    }
}
