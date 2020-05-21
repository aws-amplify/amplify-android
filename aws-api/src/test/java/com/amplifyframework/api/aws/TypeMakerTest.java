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

import com.amplifyframework.api.graphql.GraphQLResponse;

import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;

/**
 * Tests for TypeMaker utility class.
 */
public class TypeMakerTest {
    /**
     * Validates making a parameterized type.
     */
    @Test
    public void validateMakingParameterizedType() {
        Type expectedType = TypeToken.getParameterized(Iterable.class, Todo.class).getType();
        Type actualType = TypeMaker.getParameterizedType(Iterable.class, Todo.class);
        assertEquals(expectedType, actualType);
    }

    /**
     * Validates making a parameterized type where the parameter itself is a parameterized type as well.
     */
    @Test
    public void validateMakingParameterizedTypeOfParameterizedType() {
        Type iterableType = TypeToken.getParameterized(Iterable.class, Todo.class).getType();
        Type expectedType = TypeToken.getParameterized(GraphQLResponse.class, iterableType).getType();
        Type actualType = TypeMaker.getParameterizedType(GraphQLResponse.class, Iterable.class, Todo.class);
        assertEquals(expectedType, actualType);
    }
}
