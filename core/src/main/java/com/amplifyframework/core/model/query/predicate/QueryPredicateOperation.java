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

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.FieldFinder;

import java.util.Arrays;
import java.util.Collections;

/**
 * Represents an individual comparison operation on a model field.
 * @param <T> Data type of the field being evaluated
 */
public final class QueryPredicateOperation<T> implements QueryPredicate {
    private final String field;
    private final QueryOperator<T> operator;

    /**
     * Create a new comparison operation with the field to examine and the comparison to perform on it.
     * @param field the name of the Java property in the model representing the field to perform this comparison on
     * @param operator the comparison to perform on it
     */
    QueryPredicateOperation(@NonNull String field,
                            @NonNull QueryOperator<T> operator) {
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
    public QueryOperator<T> operator() {
        return operator;
    }

    /**
     * Return a group connecting this operation with another predicate with an AND type.
     * @param predicate the predicate to connect to
     * @return a group connecting this operation with another predicate with an AND type
     */
    public QueryPredicateGroup and(QueryPredicate predicate) {
        return new QueryPredicateGroup(QueryPredicateGroup.Type.AND, Arrays.asList(this, predicate));
    }

    /**
     * Return a group connecting this operation with another group/operation with an OR type.
     * @param predicate the group/operation to connect to
     * @return a group connecting this operation with another group/operation with an OR type
     */
    public QueryPredicateGroup or(QueryPredicate predicate) {
        return new QueryPredicateGroup(QueryPredicateGroup.Type.OR, Arrays.asList(this, predicate));
    }

    /**
     * Return a group negating the given operation.
     * @param predicate the operation to negate
     * @return a group negating the given operation
     */
    public static QueryPredicateGroup not(QueryPredicateOperation<?> predicate) {
        return new QueryPredicateGroup(QueryPredicateGroup.Type.NOT, Collections.singletonList(predicate));
    }

    /**
     * Evaluate the operation on the specific field of the
     * provided object.
     * @param object The object to evaluate against
     * @return Evaluated result of this operation on
     *          associated field
     * @throws IllegalArgumentException when the provided field
     *          has a data type that cannot be evaluated
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean evaluate(@NonNull Object object) throws IllegalArgumentException {
        try {
            T fieldValue = (T) FieldFinder.extractFieldValue(object, field);
            return operator.evaluate(fieldValue);
        } catch (ClassCastException castException) {
            throw new IllegalArgumentException(field + " field inside " +
                    "provided object cannot be evaluated by the operator " +
                    "type: " + operator.type().name(),
                    castException);
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            QueryPredicateOperation<?> op = (QueryPredicateOperation) obj;

            return ObjectsCompat.equals(field(), op.field()) &&
                    ObjectsCompat.equals(operator(), op.operator());
        }
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                field(),
                operator()
        );
    }

    @Override
    public String toString() {
        return "QueryPredicateOperation { " +
            "field: " + field() +
            ", operator: " + operator() +
            " }";
    }
}
