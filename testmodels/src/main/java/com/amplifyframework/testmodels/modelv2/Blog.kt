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
import com.amplifyframework.core.model.annotations.HasMany
import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.core.model.annotations.ModelField

@ModelConfig(pluralName = "Blogs")
data class Blog(
    @ModelField(targetType = "ID")
    val id: String,

    @ModelField(targetType = "Post")
    @HasMany(associatedWith = "blog", type = Post::class)
    val posts: List<Post>
) : Model {

    class Path(name: String = "root", isCollection: Boolean = false, parent: PropertyPath? = null) :
        ModelPath<Blog>(name, isCollection, modelType = Blog::class.java, parent = parent) {

        val id = string("id")
        val title = string("title")
        val posts by lazy { Post.Path("posts", isCollection = true, parent = this) }
    }

    companion object {
        val rootPath = Path()
    }

}
