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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.datastore.model.SimpleModelProvider;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Comment;
import com.amplifyframework.testmodels.commentsblog.Post;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link TopologicalOrdering} utility.
 */
public final class TopologicalOrderingTest {
    /**
     * Checks the topological ordering of the Blog, Post, and Comment classes.
     * @throws AmplifyException On failure to load models into registry
     */
    @Test
    public void orderingOfBlogPostComment() throws AmplifyException {
        // Load the models into the registry.
        // They are provided intentionally out of topological order.
        final SimpleModelProvider provider =
            SimpleModelProvider.withRandomVersion(Comment.class, Blog.class, BlogOwner.class, Post.class);

        final SchemaRegistry registry = SchemaRegistry.instance();
        registry.clear();
        registry.register(provider.models());

        // Find the schema that the registry created for each of the models.
        ModelSchema commentSchema = findSchema(registry, Comment.class);
        ModelSchema postSchema = findSchema(registry, Post.class);
        ModelSchema blogSchema = findSchema(registry, Blog.class);

        // Act: get a topological ordering of the models.
        TopologicalOrdering topologicalOrdering = TopologicalOrdering.forRegisteredModels(registry, provider);

        // Assert: Blog comes before Post, and Post comes before Comment.
        assertTrue(topologicalOrdering.check(blogSchema).isBefore(postSchema));
        assertTrue(topologicalOrdering.check(postSchema).isBefore(commentSchema));

        // Assert: in other words, Comment is after Post, and Post is after Blog.
        assertTrue(topologicalOrdering.check(commentSchema).isAfter(postSchema));
        assertTrue(topologicalOrdering.check(postSchema).isAfter(blogSchema));
    }

    /**
     * Find a {@link ModelSchema} in an {@link SchemaRegistry}, looking up by the
     * model's {@link Class}.
     * @param registry Model schema registry
     * @param modelClass Class of model
     * @param <T> Type of model
     * @return The ModelSchema for the requested class
     */
    private <T extends Model> ModelSchema findSchema(SchemaRegistry registry, Class<T> modelClass) {
        return registry.getModelSchemaForModelClass(modelClass.getSimpleName());
    }
}
