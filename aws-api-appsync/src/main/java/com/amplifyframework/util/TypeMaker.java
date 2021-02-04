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

package com.amplifyframework.util;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Utility class to build parameterized Type objects.  Helps to minimize leaking usage of Gson's TypeToken
 * throughout the codebase.
 */
public final class TypeMaker {
    private TypeMaker() {
        throw new UnsupportedOperationException("No instances allowed.");
    }

    /**
     * Creates a parameterized type from the provided type args.  At least one type argument is required.
     * @param types types to be converted into a single parameterized type, in the order they are provided.
     * @return Type, as a parameterized representation of the provided types array.
     * @throws IllegalArgumentException if no types are passed.
     */
    public static Type getParameterizedType(Type... types) throws IllegalArgumentException {
        if (types.length == 0) {
            throw new IllegalArgumentException("At least one Type must be passed as an argument");
        } else if (types.length == 1) {
            return types[0];
        } else if (types.length == 2) {
            return TypeToken.getParameterized(types[0], types[1]).getType();
        } else {
            Type typeArg = getParameterizedType(Arrays.copyOfRange(types, 1, types.length));
            return TypeToken.getParameterized(types[0], typeArg).getType();
        }
    }
}
