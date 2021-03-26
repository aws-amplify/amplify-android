/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ModelConverterTest {

    /**
     * Verify that a Java model converted to a Map returns the expected value.
     * @throws AmplifyException On failure to derive ModelSchema
     */
    @Test
    public void toMapForModelReturnsExpectedMap() throws AmplifyException {
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Joe Swanson")
            .build();
        ModelSchema schema = ModelSchema.fromModelClass(BlogOwner.class);
        Map<String, Object> actual = ModelConverter.toMap(blogOwner, schema);

        Map<String, Object> expected = new HashMap<>();
        expected.put("id", blogOwner.getId());
        expected.put("createdAt", null);
        expected.put("name", "Joe Swanson");
        expected.put("updatedAt", null);
        expected.put("wea", null);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a Java model with children converted to a Map returns the expected value.
     * @throws AmplifyException On failure to derive ModelSchema
     */
    @Test public void toMapForModelWithChildrenReturnsExpectedMap() throws AmplifyException {
        Blog blog = Blog.builder()
                .name("A neat blog")
                .owner(BlogOwner.builder()
                        .name("Joe Swanson")
                        .build())
                .build();
        ModelSchema schema = ModelSchema.fromModelClass(Blog.class);
        Map<String, Object> actual = ModelConverter.toMap(blog, schema);

        Map<String, Object> expected = new HashMap<>();
        expected.put("id", blog.getId());
        expected.put("name", "A neat blog");
        expected.put("owner", SerializedModel.builder()
                .serializedData(Collections.singletonMap("id", blog.getOwner().getId()))
                .modelSchema(null)
                .build());
        assertEquals(expected, actual);
    }
}
