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
     * Generates a new equality comparison object to compare this field to the specified value.
     * @param value the value to be compared
     * @return an operation object representing the equality condition
     */
    public QueryPredicateOperation eq(String value) {
        return new QueryPredicateOperation(fieldName, QueryOperatorFactory.equalTo(value));
    }
}
