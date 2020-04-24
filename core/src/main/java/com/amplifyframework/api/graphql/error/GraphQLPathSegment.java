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

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

/**
 * If a GraphQLResponse contains an error that can be associated with a particular field in the
 * requested GraphQL document, it will contain an entry with the key, path.  This entry should be a
 * list of GraphQLPathSegments starting at the root of the response and ending with the field
 * associated with the error.  This allows clients to determine whether a null result is intentional
 * or caused by a runtime error.  Path segments that represent fields should be strings, and path
 * segments that represent list indices should be 0‐indexed integers.
 * https://spec.graphql.org/June2018/#sec-Errors
 */
public final class GraphQLPathSegment {
    private final Object value;

    public GraphQLPathSegment(int value) {
        this.value = Integer.valueOf(value);
    }

    public GraphQLPathSegment(@NonNull String value) {
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
     * @throws ClassCastException if the value contained is not a String.
     */
    @NonNull
    public String getAsString() {
        if (isString()) {
            return (String) value;
        }
        throw new ClassCastException("Not a String: " + value.getClass().getSimpleName());
    }

    /**
     * Convenience method to get this element as an int.
     *
     * @return get this element as an int
     * @throws ClassCastException if the value contained is not an Integer.
     */
    public int getAsInt() {
        if (isInteger()) {
            return ((Integer) value).intValue();
        }
        throw new ClassCastException("Not an int: " + value.getClass().getSimpleName());
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        GraphQLPathSegment segment = (GraphQLPathSegment) thatObject;

        if (isString() && segment.isString()) {
            return ObjectsCompat.equals(getAsString(), segment.getAsString());
        }
        if (isInteger() && segment.isInteger()) {
            return ObjectsCompat.equals(getAsInt(), segment.getAsInt());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
