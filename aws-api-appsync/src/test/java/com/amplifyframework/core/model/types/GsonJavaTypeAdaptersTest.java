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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link GsonJavaTypeAdapters.ClassTypeAdapterFactory} in isolation.
 */
public final class GsonJavaTypeAdaptersTest {
    /**
     * Start with a class, serialize it, deserialize it, and
     * expect the same class back.
     */
    @Test
    public void serializeAndDeserializeClass() {
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new GsonJavaTypeAdapters.ClassTypeAdapterFactory())
            .create();
        String serialized = gson.toJson(BlogOwner.class);
        assertEquals(
            Wrap.inDoubleQuotes("com.amplifyframework.testmodels.commentsblog.BlogOwner"),
            serialized
        );
        Class<?> deserialized = gson.fromJson(serialized, Class.class);
        assertEquals(BlogOwner.class, deserialized);
    }
}
