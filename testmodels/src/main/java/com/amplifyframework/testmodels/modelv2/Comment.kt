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
package com.amplifyframework.testmodels.modelv2

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelPath
import com.amplifyframework.core.model.PropertyPath
import com.amplifyframework.core.model.annotations.BelongsTo
import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.core.model.annotations.ModelField

@ModelConfig(pluralName = "Comments")
data class Comment(
    @ModelField(targetType = "ID")
    val id: String,

    @ModelField(isRequired = true)
    val content: String,

    @ModelField(targetType = "Post", isRequired = true)
    @BelongsTo(targetName = "comments", type = Post::class)
    val post: Post
) : Model {

    class Path(name: String = "root", isCollection: Boolean = false, parent: PropertyPath? = null) :
        ModelPath<Comment>(name, isCollection, modelType = Comment::class.java, parent = parent) {

        val id = string("id")
        val content = string("content")
        val post by lazy { Post.Path("posts", parent = this) }
    }

    companion object {
        val rootPath = Path()
    }
}
