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

package com.amplifyframework.core.model;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.customprimarykey.Comment;
import com.amplifyframework.testmodels.customprimarykey.Post;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.register(new HashSet<>(Collections.singletonList(BlogOwner.class)));
        Blog blog = Blog.builder()
                .name("A neat blog")
                .owner(BlogOwner.builder()
                        .name("Joe Swanson")
                        .build())
                .build();
        ModelSchema schema = ModelSchema.fromModelClass(Blog.class);
        Map<String, Object> actual = ModelConverter.toMap(blog, schema);

        Map<String, Object> expected = new HashMap<>();
        expected.put("updatedAt", null);
        expected.put("id", blog.getId());
        expected.put("name", "A neat blog");
        expected.put("createdAt", null);
        expected.put("owner", SerializedModel.builder()
                .modelSchema(schemaRegistry.getModelSchemaForModelClass(BlogOwner.class))
                .serializedData(Collections.singletonMap("id", blog.getOwner().getId()))
                .build());

        assertEquals(expected, actual);
    }

    /**
     * Verify that a Java model with children converted to a Map returns the expected value.
     * @throws AmplifyException On failure to derive ModelSchema
     */
    @Test public void toMapForSerializedModelWithChildrenAndCustomPrimaryKeyReturnsExpectedMap()
            throws AmplifyException {
        SchemaRegistry schemaRegistry = SchemaRegistry.instance();
        schemaRegistry.register(new HashSet<>(Arrays.asList(Post.class, Comment.class)));
        HashMap<String, Object> postMap = new HashMap<String, Object>();
        postMap.put("id", "testId");
        postMap.put("title", "new post");
        SerializedModel post = SerializedModel.builder()
                .modelSchema(schemaRegistry.getModelSchemaForModelClass(Post.class))
                .serializedData(postMap)
                .build();
        HashMap<String, Object> commentMap = new HashMap<String, Object>();
        commentMap.put("title", "A neat comment");
        commentMap.put("content", "neat comment");
        commentMap.put("likes", 1);
        commentMap.put("post", post);
        SerializedModel comment = SerializedModel.builder()
                .modelSchema(schemaRegistry.getModelSchemaForModelClass(Comment.class))
                .serializedData(commentMap)
                .build();
        ModelSchema schema = ModelSchema.fromModelClass(Comment.class);
        Map<String, Object> actual = ModelConverter.toMap(comment, schema);
        assertEquals(comment.getSerializedData(), actual);
    }
}
