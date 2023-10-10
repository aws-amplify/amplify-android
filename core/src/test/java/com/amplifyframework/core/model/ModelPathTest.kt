/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model

import com.amplifyframework.testmodels.lazy.Post
import com.amplifyframework.testmodels.todo.Todo
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelPathTest {

    @Test
    fun get_path_directly_from_model() {
        val expectedMetadata = PropertyPathMetadata("root", false, null)

        val postPath = Post.rootPath

        assertEquals(expectedMetadata, postPath.getMetadata())
        assertEquals(Post::class.java, postPath.getModelType())
    }

    @Test
    fun get_path_from_model_path() {
        val expectedMetadata = PropertyPathMetadata("root", false, null)

        val actualPath = ModelPath.getRootPath(Post::class.java)

        assertEquals(Post::class.java, actualPath.getModelType())
        assertEquals(expectedMetadata, actualPath.getMetadata())
    }

    @Test
    fun includes_provides_list_of_relationships() {
        val postPath = Post.rootPath
        val expectedRelationships = listOf(postPath.blog, postPath.comments)

        val acutalRelationships = includes(postPath.blog, postPath.comments)

        assertEquals(expectedRelationships, acutalRelationships)
    }

    @Test(expected = ModelException.PropertyPathNotFound::class)
    fun get_root_path_fails_on_non_lazy_supported_model() {
        ModelPath.getRootPath(Todo::class.java)
    }
}
