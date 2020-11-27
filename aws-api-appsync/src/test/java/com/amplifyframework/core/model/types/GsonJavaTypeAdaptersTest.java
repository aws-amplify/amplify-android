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

package com.amplifyframework.core.model.types;

import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.util.Wrap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link GsonJavaTypeAdapters.ClassTypeAdapterFactory} in isolation.
 */
public final class GsonJavaTypeAdaptersTest {
    private Gson gson;

    /**
     * We'll test the adapter through a Gson instance. Set one up.
     */
    @Before
    public void setup() {
        this.gson = new GsonBuilder()
            .registerTypeAdapterFactory(new GsonJavaTypeAdapters.ClassTypeAdapterFactory())
            .create();
    }

    /**
     * Start with a class, serialize it, deserialize it, and
     * expect the same class back.
     */
    @Test
    public void serializeAndDeserializeClass() {
        String serialized = gson.toJson(BlogOwner.class);
        assertEquals(
            Wrap.inDoubleQuotes("com.amplifyframework.testmodels.commentsblog.BlogOwner"),
            serialized
        );
        Class<?> deserialized = gson.fromJson(serialized, Class.class);
        assertEquals(BlogOwner.class, deserialized);
    }

    /**
     * Validate that primitive type labels (e.g. "int") can be derialized to their
     * corresponding boxed class types.
     */
    @Test
    public void deserializePrimitiveLabel() {
        Map<String, Class<?>> primitiveNameToBoxedType = new HashMap<>();
        primitiveNameToBoxedType.put("int", Integer.class);
        primitiveNameToBoxedType.put("short", Short.class);
        primitiveNameToBoxedType.put("long", Long.class);
        primitiveNameToBoxedType.put("boolean", Boolean.class);
        primitiveNameToBoxedType.put("byte", Byte.class);
        primitiveNameToBoxedType.put("char", Character.class);
        primitiveNameToBoxedType.put("float", Float.class);
        primitiveNameToBoxedType.put("double", Double.class);
        primitiveNameToBoxedType.put("void", Void.class);

        for (Map.Entry<String, Class<?>> entry : primitiveNameToBoxedType.entrySet()) {
            assertEquals(
                "Primitive comprehension failed for " + entry.getKey(),
                entry.getValue(),
                gson.fromJson(entry.getKey(), Class.class)
            );
        }
    }

    /**
     * If a user inadvertently provided a primitive type, we just box it.
     */
    @Test
    public void serializePrimitiveRetrieveClass() {
        Map<Class<?>, Class<?>> primitiveTypeToBoxedType = new HashMap<>();
        primitiveTypeToBoxedType.put(int.class, Integer.class);
        primitiveTypeToBoxedType.put(short.class, Short.class);
        primitiveTypeToBoxedType.put(long.class, Long.class);
        primitiveTypeToBoxedType.put(boolean.class, Boolean.class);
        primitiveTypeToBoxedType.put(byte.class, Byte.class);
        primitiveTypeToBoxedType.put(char.class, Character.class);
        primitiveTypeToBoxedType.put(float.class, Float.class);
        primitiveTypeToBoxedType.put(double.class, Double.class);
        primitiveTypeToBoxedType.put(void.class, Void.class);

        for (Map.Entry<Class<?>, Class<?>> entry : primitiveTypeToBoxedType.entrySet()) {
            String serialized = gson.toJson(entry.getKey());
            assertEquals(
                "Primitive comprehension failed for " + serialized,
                entry.getValue(),
                gson.fromJson(serialized, Class.class)
            );
        }
    }
}
