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
 * Represents a between condition with a starting and ending value for comparison.
 */
public final class BetweenQueryOperator extends QueryOperator {
    private Object start;
    private Object end;

    /**
     * Constructs a between condition.
     * @param start the value to be used for the beginning of the comparison range
     * @param end the value to be used for the end of the comparison range
     */
    public BetweenQueryOperator(Object start, Object end) {
        super(Type.BETWEEN);
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the value to be used for the start of the range in the comparison.
     * @return the value to be used for the start of the range in the comparison.
     */
    public Object start() {
        return start;
    }

    /**
     * Returns the value to be used for the end of the range in the comparison.
     * @return the value to be used for the end of the range in the comparison.
     */
    public Object end() {
        return end;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            BetweenQueryOperator op = (BetweenQueryOperator) obj;

            return ObjectsCompat.equals(type(), op.type()) &&
                    ObjectsCompat.equals(start(), op.start()) &&
                    ObjectsCompat.equals(end(), op.end());
        }
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                type(),
                start(),
                end()
        );
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("BetweenQueryOperator { ")
                .append("type: ")
                .append(type())
                .append(", start: ")
                .append(start)
                .append(", end: ")
                .append(end)
                .append(" }")
                .toString();
    }
}
