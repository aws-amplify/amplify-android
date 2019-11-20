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
 * A single source for developers to see what types of comparison operators they can create.
 */
public final class QueryOperatorFactory {

    /**
     * Private constructor to prevent developers from instantiating this factory.
     */
    private QueryOperatorFactory() { }

    /**
     * Returns a begins with comparison operator.
     * @param value the value to be compared
     * @return an operator object representing the begins with condition
     */
    public static BeginsWithQueryOperator beginsWith(Object value) {
        return new BeginsWithQueryOperator(value);
    }

    /**
     * Returns a between comparison operator.
     * @param start the value to be compared for the start of the range
     * @param end the value to be compared for the end of the range
     * @return an operator object representing the between condition
     */
    public static BetweenQueryOperator between(Object start, Object end) {
        return new BetweenQueryOperator(start, end);
    }

    /**
     * Returns a contains comparison operator.
     * @param value the value to be compared
     * @return an operator object representing the contains condition
     */
    public static ContainsQueryOperator contains(Object value) {
        return new ContainsQueryOperator(value);
    }

    /**
     * Returns an equality comparison operator.
     * @param value the value to be compared
     * @return an operator object representing the equality condition
     */
    public static EqualQueryOperator equalTo(Object value) {
        return new EqualQueryOperator(value);
    }

    /**
     * Returns a greater or equal comparison operator.
     * @param value the value to be compared
     * @return an operator object representing the greater or equal condition
     */
    public static GreaterOrEqualQueryOperator greaterOrEqual(Object value) {
        return new GreaterOrEqualQueryOperator(value);
    }

    /**
     * Returns a greater than comparison operator.
     * @param value the value to be compared
     * @return an operator object representing the greater than condition
     */
    public static GreaterThanQueryOperator greaterThan(Object value) {
        return new GreaterThanQueryOperator(value);
    }

    /**
     * Returns a less or equal comparison operator.
     * @param value the value to be compared
     * @return an operator object representing the less or equal condition
     */
    public static LessOrEqualQueryOperator lessOrEqual(Object value) {
        return new LessOrEqualQueryOperator(value);
    }

    /**
     * Returns a less than comparison operator.
     * @param value the value to be compared
     * @return an operator object representing the less than condition
     */
    public static LessThanQueryOperator lessThan(Object value) {
        return new LessThanQueryOperator(value);
    }

    /**
     * Returns a not equal comparison operator.
     * @param value the value to be compared
     * @return an operator object representing the not equal condition
     */
    public static NotEqualQueryOperator notEqual(Object value) {
        return new NotEqualQueryOperator(value);
    }
}
