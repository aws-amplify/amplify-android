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

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests for TypeMaker utility class.
 */
public final class TypeMakerTest {
    /**
     * Validate that an IllegalArgumentException is thrown when no arguments are provided.
     */
    @Test
    public void makeTypeWithNoArguments() {
        assertThrows(IllegalArgumentException.class, TypeMaker::getParameterizedType);
    }
    
    /**
     * Validates making a type.
     */
    @Test
    public void makeTypeWithOneArgument() {
        Type expectedType = BlogOwner.class;
        Type actualType = TypeMaker.getParameterizedType(BlogOwner.class);
        assertEquals(expectedType, actualType);
    }

    /**
     * Validates making a parameterized type.
     */
    @Test
    public void makeParameterizedType() {
        Type expectedType = TypeToken.getParameterized(Iterable.class, BlogOwner.class).getType();
        Type actualType = TypeMaker.getParameterizedType(Iterable.class, BlogOwner.class);
        assertEquals(expectedType, actualType);
    }

    /**
     * Validates making a parameterized type from 3 types.
     */
    @Test
    public void makeParameterizedTypeOfParameterizedType() {
        Type iterableType = TypeToken.getParameterized(Iterable.class, BlogOwner.class).getType();
        Type expectedType = TypeToken.getParameterized(GraphQLResponse.class, iterableType).getType();
        Type actualType = TypeMaker.getParameterizedType(GraphQLResponse.class, Iterable.class, BlogOwner.class);
        assertEquals(expectedType, actualType);
    }

    /**
     * Validates making a parameterized type from 4 types.
     */
    @Test
    public void makeParameterizedTypeOfParameterizedTypeOfParameterizedType() {
        Type iterableType = TypeToken.getParameterized(Iterable.class, BlogOwner.class).getType();
        Type responseType = TypeToken.getParameterized(GraphQLResponse.class, iterableType).getType();
        Type expectedType = TypeToken.getParameterized(GraphQLRequest.class, responseType).getType();
        Type actualType = TypeMaker.getParameterizedType(GraphQLRequest.class,
                GraphQLResponse.class, Iterable.class, BlogOwner.class);
        assertEquals(expectedType, actualType);
    }
}
