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

import com.amplifyframework.api.aws.SelectionSetDepth.Companion.onlyIncluded
import com.amplifyframework.api.graphql.QueryType
import com.amplifyframework.testmodels.modelv2.Comment
import com.amplifyframework.testmodels.modelv2.Post
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun assertSelectionSetEquals(expected: String, actual: SelectionSet) {
    assertEquals(expected.trimIndent(), actual.toString("").trim())
}

@RunWith(RobolectricTestRunner::class)
class SelectionSetIncludesTest {

    /**
     * - Given:
     *     - a `Model` schema
     * - When:
     *     - the model schema comes from the type `Comment`
     *     - the belongTo `post` association is present in the schema
     *     but not included in the selection set
     * - Then:
     *     - check if the generated selection set includes only the
     *     `post` primary keys (i.e. the foreign key needed to associate both models)
     */
    @Test
    fun `it should create a selection set of a comment with default options`() {
        val selectionSet = SelectionSet.builder()
            .operation(QueryType.GET)
            .modelClass(Comment::class.java)
            .requestOptions(onlyIncluded())
            .build()

        assertSelectionSetEquals(
            """
            {
              content
              id
              post {
                id
              }
            }
            """,
            selectionSet
        )
    }

    /**
     * - Given:
     *     - a `Model` schema
     * - When:
     *     - the model schema comes from the type `Post`
     *     - the belongTo `author` association is present in the schema
     *     but not included in the selection set
     *     - the belongTo `blog` association is present in the schema
     *     but not included in the selection set
     * - Then:
     *     - check if the generated selection set includes only the
     *     `post` and `authors` primary keys (i.e. the foreign key needed to associate both models)
     */
    @Test fun `it should create a selection set of a post`() {
        val post = Post.rootPath
        val selectionSet = SelectionSet.builder()
            .operation(QueryType.GET)
            .modelClass(Post::class.java)
            .requestOptions(onlyIncluded())
            .build()

        assertSelectionSetEquals(
            """
            {
              author {
                id
              }
              blog {
                id
              }
              id
              title
            }
            """,
            selectionSet
        )
    }

    /**
     * - Given:
     *     - a `Model` schema
     * - When:
     *     - the model schema comes from the type `Post`
     *     - the belongTo `author` association is present in the schema
     *     but not included in the selection set
     *     - the belongTo `blog` association is present in the schema
     *     but not included in the selection set
     *     - the hasMany `comments` association is included
     * - Then:
     *     - check if the comments selection set is added to the schema
     *     - check if the generated selection set includes only the
     *     `post` and `authors` primary keys (i.e. the foreign key needed to associate both models)
     */
    @Test fun `it should create a selection set of a post including its comments`() {
        val post = Post.rootPath
        val selectionSet = SelectionSet.builder()
            .operation(QueryType.GET)
            .modelClass(Post::class.java)
            .requestOptions(onlyIncluded())
            .includeAssociations(post.comments)
            .build()

        assertSelectionSetEquals(
            """
            {
              author {
                id
              }
              blog {
                id
              }
              comments {
                items {
                  content
                  id
                  post {
                    id
                  }
                }
              }
              id
              title
            }
            """,
            selectionSet
        )
    }


    /**
     * - Given:
     *     - a `Model` schema
     * - When:
     *     - the model schema comes from the type `Post`
     *     - the belongTo `author` association is included
     *     - the hasMany `comments` association is included
     *     - the belongTo `blog` association is present in the schema
     *     but not included in the selection set
     * - Then:
     *     - check if the comments selection set is added to the schema
     *     - check if the generated selection set includes only the
     *     `post` and `authors` primary keys (i.e. the foreign key needed to associate both models)
     */
    @Test fun `it should create a selection set of a post including its comments and author`() {
        val post = Post.rootPath
        val selectionSet = SelectionSet.builder()
            .operation(QueryType.GET)
            .modelClass(Post::class.java)
            .requestOptions(onlyIncluded())
            .includeAssociations(post.author, post.comments)
            .build()

        assertSelectionSetEquals(
            """
            {
              author {
                id
                name
              }
              blog {
                id
              }
              comments {
                items {
                  content
                  id
                  post {
                    id
                  }
                }
              }
              id
              title
            }
            """,
            selectionSet
        )
    }
}