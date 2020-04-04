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

import androidx.core.util.ObjectsCompat;

/**
 * Represents a contains condition with a target value for comparison.
 */
public final class ContainsQueryOperator extends QueryOperator<String> {
    private final String value;

    /**
     * Constructs a contains condition.
     * @param value the value to be used in the comparison
     */
    ContainsQueryOperator(String value) {
        super(Type.CONTAINS);
        this.value = value;
    }

    /**
     * Returns the value to be used in the comparison.
     * @return the value to be used in the comparison
     */
    public Object value() {
        return value;
    }

    /**
     * Returns true if the the provided field value contains
     * the value associated with this operator.
     * @param field the field value to operate on
     * @return evaluated result of the operator
     */
    @Override
    public boolean evaluate(String field) {
        return field.contains(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            ContainsQueryOperator op = (ContainsQueryOperator) obj;

            return ObjectsCompat.equals(type(), op.type()) &&
                    ObjectsCompat.equals(value(), op.value());
        }
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                type(),
                value()
        );
    }

    @Override
    public String toString() {
        return "ContainsQueryOperator { " +
            "type: " + type() +
            ", value: " + value() +
            " }";
    }
}
