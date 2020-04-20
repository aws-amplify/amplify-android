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

package com.amplifyframework.api.graphql.error;

public class GraphQLPathSegment {
    private final Object value;

    public GraphQLPathSegment(int value) {
        this.value = Integer.valueOf(value);
    }

    public GraphQLPathSegment(String value) {
        this.value = value;
    }

    /**
     * Used before calling getAsInt() to confirm if value is int.
     *
     * @return boolean, whether segment is an int
     */
    public boolean isInteger() {
        return value instanceof Integer;
    }

    /**
     * Used before calling getAsString() to confirm if value is String.
     *
     * @return boolean, whether segment is a String
     */
    public boolean isString() {
        return value instanceof String;
    }

    /**
     * Convenience method to get this element as a String.
     *
     * @return get this element as a String
     * @throws IllegalStateException if the value contained is not a String.
     */
    public String getAsString() {
        if (isString()) {
            return (String) value;
        }
        throw new IllegalStateException("Not a String: " + value.getClass().getSimpleName());
    }

    /**
     * Convenience method to get this element as an int.
     *
     * @return get this element as an int
     * @throws IllegalStateException if the value contained is not an Integer.
     */
    public int getAsInt() {
        if (isInteger()) {
            return ((Integer) value).intValue();
        }
        throw new IllegalStateException("Not an int: " + value.getClass().getSimpleName());
    }
}
