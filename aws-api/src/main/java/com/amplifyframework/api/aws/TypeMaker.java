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

package com.amplifyframework.api.aws;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * Wrapper utility class to build parameterized Type objects.  Helps to minimize leaking usage of Gson's TypeToken
 * throughout the codebase.
 */
public final class TypeMaker {

    private TypeMaker() {
        throw new UnsupportedOperationException("No instances allowed.");
    }

    static Type getParameterizedType(Type type, Type typeArg) {
        return TypeToken.getParameterized(type, typeArg).getType();
    }

    static Type getParameterizedType(Type type, Type typeArg, Type typeArgArg) {
        return getParameterizedType(type, getParameterizedType(typeArg, typeArgArg));
    }

}
