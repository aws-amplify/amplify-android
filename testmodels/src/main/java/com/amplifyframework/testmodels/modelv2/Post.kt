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
import com.amplifyframework.core.model.annotations.HasMany
import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.core.model.annotations.ModelField

@ModelConfig(pluralName = "Posts")
data class Post(
    @ModelField(targetType = "ID", isRequired = true) val id: String,
    @ModelField(isRequired = true) val title: String,

    @ModelField(targetType = "Blog", isRequired = true)
    @BelongsTo(targetName = "posts", type = Blog::class)
    val blog: Blog,

    @ModelField(targetType = "Author", isRequired = true)
    @BelongsTo(targetName = "posts", type = Author::class)
    val author: Author,

    @ModelField(targetType = "Comment")
    @HasMany(associatedWith = "author", type = Comment::class)
    val comments: List<Comment> = emptyList()
) : Model {

    class Path(name: String = "root", isCollection: Boolean = false, parent: PropertyPath? = null) :
        ModelPath<Post>(name, isCollection, modelType = Post::class.java, parent = parent) {

        val id = string("id")
        val title = string("title")
        val blog by lazy { Blog.Path("blog", parent = this) }
        val author by lazy { Author.Path("author", parent = this) }
        val comments by lazy { Comment.Path("comments", isCollection = true, parent = this) }
    }

    companion object {
        val rootPath = Path()
    }

}
