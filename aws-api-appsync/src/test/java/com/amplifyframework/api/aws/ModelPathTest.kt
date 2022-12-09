/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws

import com.amplifyframework.core.model.ModelException
import com.amplifyframework.core.model.getRootPath
import com.amplifyframework.testmodels.modelv2.Post
import com.amplifyframework.testmodels.todo.Todo
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class ModelPathTest {

    /**
     * - Given:
     *     - a model with a generated `rootPath` static property
     * - When:
     *     - the extension method `getRootPath()` is called
     * - Then:
     *     - the method should successfully return the reference to the `rootPath`
     */
    @Test fun `it should get a reference to the rootPath successfully`() {
        assertEquals(Post.rootPath, Post::class.java.getRootPath())
    }

    /**
     * - Given:
     *     - a model v1 _without_ the generated `rootPath` static property
     * - When:
     *     - the extension method `getRootPath()` is called
     * - Then:
     *     - the method should throw a `ModelException.PropertyPathNotFound`
     *     indicating the `rootPath` is not present.
     */
    @Test fun `it should thrown an exception when the rootPath is missing`() {
        assertThrows(ModelException.PropertyPathNotFound::class.java) {
            Todo::class.java.getRootPath()
        }
    }

    /**
     * - Given:
     *     - A property path from `Post`, `post.comments.id`
     * - When:
     *     - The `getKeyPath()` method is called with no arguments
     * - Then:
     *     - The output should be `comments.id`
     */
    @Test fun `it should return the path as a string successfully`() {
        val post = Post.rootPath
        assertEquals("comments.id", post.comments.id.getKeyPath())
    }

    /**
     * - Given:
     *   - A property path from `Post`, `post.comments.id`
     * - When:
     *   - The `getKeyPath()` method is called with `includesRoot = true`
     * - Then:
     *   - The output should be `root.comments.id`
     */
    @Test fun `it should return the path with the root node as a string successfully`() {
        val post = Post.rootPath
        assertEquals("root.comments.id", post.comments.id.getKeyPath(includesRoot = true))
    }

}
