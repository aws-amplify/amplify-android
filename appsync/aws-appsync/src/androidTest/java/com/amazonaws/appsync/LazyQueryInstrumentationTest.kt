/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazonaws.appsync

import androidx.test.core.app.ApplicationProvider
import com.amazonaws.appsync.extensions.fetchAllPages
import com.amplifyframework.testmodels.lazycpk.Blog
import com.amplifyframework.testmodels.lazycpk.Comment
import com.amplifyframework.testmodels.lazycpk.HasManyChild
import com.amplifyframework.testmodels.lazycpk.HasManyChild.HasManyChildIdentifier
import com.amplifyframework.testmodels.lazycpk.HasManyChildPath
import com.amplifyframework.testmodels.lazycpk.Parent
import com.amplifyframework.testmodels.lazycpk.ParentPath
import com.amplifyframework.testmodels.lazycpk.Post
import com.amplifyframework.testmodels.lazycpk.PostPath
import com.amplifyframework.testmodels.lazycpk.Project
import com.amplifyframework.testmodels.lazycpk.ProjectPath
import com.amplifyframework.testmodels.lazycpk.Team
import com.amplifyframework.testmodels.lazycpk.TeamPath
import com.amazonaws.appsync.test.R
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.model.LazyModelList
import com.amplifyframework.core.model.LazyModelReference
import com.amplifyframework.core.model.LoadedModelList
import com.amplifyframework.core.model.LoadedModelReference
import com.amplifyframework.core.model.includes
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.Resources
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

/**
 * Integration test that verifies [AmplifyAppSyncClient] can perform lazy-loading query
 * operations using codegen models with relationships. Mirrors the test scenarios from
 * [com.amplifyframework.api.aws.GraphQLLazyQueryInstrumentationTest] in the :aws-api module.
 *
 * Uses pre-populated data in the "lazy" AppSync backend. The data was seeded by the
 * populate() method in the original test (commented out there, run once manually).
 */
@OptIn(ExperimentalAmplifyApi::class, InternalAmplifyApi::class)
class LazyQueryInstrumentationTest : DeviceFarmTestBase() {

    companion object {
        val LONG_TIMEOUT = 20.seconds

        const val PARENT1_ID = "GraphQLLazyQueryInstrumentationTest-Parent"
        const val PARENT2_ID = "GraphQLLazyQueryInstrumentationTest-Parent2"
        const val HAS_ONE_CHILD1_ID = "GraphQLLazyQueryInstrumentationTest-HasOneChild1"
        const val HAS_ONE_CHILD2_ID = "GraphQLLazyQueryInstrumentationTest-HasOneChild2"

        private lateinit var client: AmplifyAppSyncClient

        @JvmStatic
        @BeforeClass
        fun setUp() {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val config = Resources.readAsJson(context, R.raw.appsync_client_config)
            client = AmplifyAppSyncClient(
                AmplifyAppSyncClient.Configuration {
                    endpoint = config.getString("lazyEndpoint")
                    authorization = AppSyncAuthorization.Single(
                        AppSyncClientAuthorizer.ApiKey(config.getString("lazyApiKey"))
                    )
                }
            )
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            client.close()
        }
    }

    @Test
    fun query_parent_no_includes() = runTest(timeout = LONG_TIMEOUT) {
        val request = ModelQuery[Parent::class.java, Parent.ParentIdentifier(PARENT1_ID)]

        val responseParent = client.query(request).getOrThrow().data

        assertEquals(HAS_ONE_CHILD1_ID, responseParent.parentChildId)
        (responseParent.child as? LazyModelReference)?.fetchModel()?.let { child ->
            assertEquals(HAS_ONE_CHILD1_ID, child.id)
            assertEquals("Child1", child.content)
        } ?: fail("Response child was null or not a LazyModelReference")

        val children = responseParent.children as LazyModelList
        val fetchedChildren = children.fetchAllPages()
        assertEquals(1001, fetchedChildren.size)
    }

    @Test
    fun query_parent_with_includes() = runTest {
        val request = ModelQuery.get<Parent, ParentPath>(
            Parent::class.java,
            Parent.ParentIdentifier(PARENT1_ID)
        ) {
            includes(it.child, it.children)
        }

        val responseParent = client.query(request).getOrThrow().data

        assertEquals(HAS_ONE_CHILD1_ID, responseParent.parentChildId)
        (responseParent.child as? LoadedModelReference)?.let { childRef ->
            val child = childRef.value!!
            assertEquals(HAS_ONE_CHILD1_ID, child.id)
            assertEquals("Child1", child.content)
        } ?: fail("Response child was null or not a LoadedModelReference")

        val children = responseParent.children as LoadedModelList
        assertEquals(100, children.items.size)
    }

    @Test
    fun query_list_with_no_includes() = runTest(timeout = LONG_TIMEOUT) {
        val request = ModelQuery.list(
            Parent::class.java,
            Parent.ID.beginsWith("GraphQLLazyQueryInstrumentationTest-Parent")
        )

        val paginatedResult = client.query(request).getOrThrow().data

        assertFalse(paginatedResult.hasNextResult())

        val parents = paginatedResult.items.toList()
        assertEquals(2, parents.size)

        val parent2 = parents[0]
        val parent1 = parents[1]

        assertEquals(HAS_ONE_CHILD1_ID, parent1.parentChildId)
        assertEquals(PARENT1_ID, parent1.id)
        (parent1.child as? LazyModelReference)?.fetchModel()?.let { child ->
            assertEquals(HAS_ONE_CHILD1_ID, child.id)
            assertEquals("Child1", child.content)
        } ?: fail("Response child was null or not a LazyModelReference")

        val childrenFromParent1 = parent1.children as LazyModelList
        val fetchedChildrenFromParent1 = childrenFromParent1.fetchAllPages()
        assertEquals(1001, fetchedChildrenFromParent1.size)

        assertEquals(HAS_ONE_CHILD2_ID, parent2.parentChildId)
        assertEquals(PARENT2_ID, parent2.id)
        (parent2.child as? LazyModelReference)?.fetchModel()?.let { child ->
            assertEquals(HAS_ONE_CHILD2_ID, child.id)
            assertEquals("Child2", child.content)
        } ?: fail("Response child was null or not a LazyModelReference")

        val childrenFromParent2 = parent2.children as LazyModelList
        val fetchedChildrenFromParent2 = childrenFromParent2.fetchAllPages()
        assertEquals(0, fetchedChildrenFromParent2.size)
    }

    @Test
    fun query_list_with_includes() = runTest {
        val request = ModelQuery.list<Parent, ParentPath>(
            Parent::class.java,
            Parent.ID.beginsWith("GraphQLLazyQueryInstrumentationTest-Parent")
        ) {
            includes(it.child, it.children)
        }

        val paginatedResult = client.query(request).getOrThrow().data

        assertFalse(paginatedResult.hasNextResult())

        val parents = paginatedResult.items.toList()
        assertEquals(2, parents.size)

        val parent2 = parents[0]
        val parent1 = parents[1]

        assertEquals(HAS_ONE_CHILD1_ID, parent1.parentChildId)
        assertEquals(PARENT1_ID, parent1.id)
        (parent1.child as? LoadedModelReference)?.let { childRef ->
            val child = childRef.value!!
            assertEquals(HAS_ONE_CHILD1_ID, child.id)
            assertEquals("Child1", child.content)
        } ?: fail("Response child was null or not a LoadedModelReference")

        assertEquals(HAS_ONE_CHILD2_ID, parent2.parentChildId)
        assertEquals(PARENT2_ID, parent2.id)
        (parent2.child as? LoadedModelReference)?.let { childRef ->
            val child = childRef.value!!
            assertEquals(HAS_ONE_CHILD2_ID, child.id)
            assertEquals("Child2", child.content)
        } ?: fail("Response child was null or not a LoadedModelReference")

        val children = parent2.children as LoadedModelList
        assertEquals(0, children.items.size)
    }

    @Test
    fun query_parent_with_no_child_with_includes() = runTest {
        val request = ModelQuery.get<Parent, ParentPath>(
            Parent::class.java,
            Parent.ParentIdentifier("GraphQLLazyQueryInstrumentationTest.ParentWithNoChildren")
        ) {
            includes(it.child, it.children)
        }

        val responseParent = client.query(request).getOrThrow().data

        assertNull(responseParent.parentChildId)
        (responseParent.child as? LoadedModelReference)?.let { childRef ->
            assertNull(childRef.value)
        } ?: fail("Response child was null or not a LoadedModelReference")

        val children = responseParent.children as LoadedModelList
        assertEquals(0, children.items.size)
    }

    @Test
    fun query_parent_with_no_child_no_includes() = runTest {
        val request = ModelQuery[
            Parent::class.java,
            Parent.ParentIdentifier("GraphQLLazyQueryInstrumentationTest.ParentWithNoChildren")
        ]

        val responseParent = client.query(request).getOrThrow().data

        assertNull(responseParent.parentChildId)
        (responseParent.child as? LoadedModelReference)?.let { childRef ->
            assertNull(childRef.value)
        } ?: fail("Response child was null or not a LoadedModelReference")

        val children = responseParent.children as LazyModelList
        assertEquals(0, children.fetchAllPages().size)
    }

    @Test
    fun query_child_belongsTo_parent_with_no_includes() = runTest {
        val request = ModelQuery[
            HasManyChild::class.java,
            HasManyChildIdentifier("GraphQLLazyQueryInstrumentationTest-HasManyChild1")
        ]

        val hasManyChild = client.query(request).getOrThrow().data

        (hasManyChild.parent as? LazyModelReference)?.let { parentRef ->
            assertEquals(PARENT1_ID, parentRef.fetchModel()!!.id)
        } ?: fail("Response child was null or not a LazyModelReference")
    }

    @Test
    fun query_child_belongsTo_parent_with_includes() = runTest {
        val request = ModelQuery.get<HasManyChild, HasManyChildPath>(
            HasManyChild::class.java,
            HasManyChildIdentifier("GraphQLLazyQueryInstrumentationTest-HasManyChild1")
        ) {
            includes(it.parent)
        }

        val hasManyChild = client.query(request).getOrThrow().data

        (hasManyChild.parent as? LoadedModelReference)?.let { parentRef ->
            assertEquals(PARENT1_ID, parentRef.value!!.id)
        } ?: fail("Response child was null or not a LoadedModelReference")
    }

    @Test
    fun query_child_belongsTo_null_parent_with_no_includes() = runTest {
        val request = ModelQuery[
            HasManyChild::class.java,
            HasManyChildIdentifier("GraphQLLazyQueryInstrumentationTest.HasManyChildNoParent")
        ]

        val hasManyChild = client.query(request).getOrThrow().data

        (hasManyChild.parent as? LoadedModelReference)?.let { parentRef ->
            assertEquals(null, parentRef.value)
        } ?: fail("Response child was null or not a LoadedModelReference")
    }

    @Test
    fun query_list_child_belongsTo_null_parent_with_no_includes() = runTest {
        val request = ModelQuery.list(
            HasManyChild::class.java,
            HasManyChild.ID.beginsWith("GraphQLLazyQueryInstrumentationTest.HasManyChildNoParent")
        )

        val hasManyChildren = client.query(request).getOrThrow().data.toList()

        assertEquals(1, hasManyChildren.size)
        (hasManyChildren[0].parent as? LoadedModelReference)?.let { parentRef ->
            assertEquals(null, parentRef.value)
        } ?: fail("Response child was null or not a LoadedModelReference")
    }

    @Test
    fun query_child_belongsTo_null_parent_with_includes() = runTest {
        val request = ModelQuery.list<HasManyChild, HasManyChildPath>(
            HasManyChild::class.java,
            HasManyChild.ID.beginsWith("GraphQLLazyQueryInstrumentationTest.HasManyChildNoParent")
        ) {
            includes(it.parent)
        }

        val hasManyChildren = client.query(request).getOrThrow().data.toList()

        assertEquals(1, hasManyChildren.size)
        (hasManyChildren[0].parent as? LoadedModelReference)?.let { parentRef ->
            assertEquals(null, parentRef.value)
        } ?: fail("Response child was null or not a LoadedModelReference")
    }

    @Test
    fun query_project_and_pull_hasOne_team_no_includes() = runTest {
        val expectedProjectId = "GraphQLLazyQueryInstrumentationTest-Parent1"
        val expectedProjectName = "Project 1"
        val expectedTeamId = "GraphQLLazyQueryInstrumentationTest-Team1"
        val expectedTeamName = "Team 1"
        val projectRequest = ModelQuery[
            Project::class.java,
            Project.ProjectIdentifier(expectedProjectId, expectedProjectName)
        ]

        val projectFromResponse = client.query(projectRequest).getOrThrow().data

        assertEquals(expectedProjectId, projectFromResponse.projectId)
        assertEquals(expectedProjectName, projectFromResponse.name)

        (projectFromResponse.team as? LazyModelReference)?.fetchModel()?.let { team ->
            assertEquals(expectedTeamId, team.teamId)
            assertEquals(expectedTeamName, team.name)
            assertTrue(team.project is LazyModelReference)
        } ?: fail("Response child was null or not a LazyModelReference")
    }

    @Test
    fun query_project_and_pull_hasOne_team_includes() = runTest {
        val expectedProjectId = "GraphQLLazyQueryInstrumentationTest-Parent1"
        val expectedProjectName = "Project 1"
        val expectedTeamId = "GraphQLLazyQueryInstrumentationTest-Team1"
        val expectedTeamName = "Team 1"
        val projectRequest = ModelQuery.get<Project, ProjectPath>(
            Project::class.java,
            Project.ProjectIdentifier(expectedProjectId, expectedProjectName)
        ) {
            includes(it.team)
        }

        val projectFromResponse = client.query(projectRequest).getOrThrow().data

        assertEquals(expectedProjectId, projectFromResponse.projectId)
        assertEquals(expectedProjectName, projectFromResponse.name)

        (projectFromResponse.team as? LoadedModelReference)?.value?.let { team ->
            assertEquals(expectedTeamId, team.teamId)
            assertEquals(expectedTeamName, team.name)
            assertTrue(team.project is LazyModelReference)
        } ?: fail("Response child was null or not a LoadedModelReference")
    }

    @Test
    fun query_team_and_pull_belongsTo_project_no_includes() = runTest {
        val expectedProjectId = "GraphQLLazyQueryInstrumentationTest-Parent1"
        val expectedProjectName = "Project 1"
        val expectedTeamId = "GraphQLLazyQueryInstrumentationTest-Team1"
        val expectedTeamName = "Team 1"
        val teamRequest = ModelQuery[
            Team::class.java,
            Team.TeamIdentifier(expectedTeamId, expectedTeamName)
        ]

        val teamFromResponse = client.query(teamRequest).getOrThrow().data

        assertEquals(expectedTeamId, teamFromResponse.teamId)
        assertEquals(expectedTeamName, teamFromResponse.name)

        (teamFromResponse.project as? LazyModelReference)?.fetchModel()?.let { project ->
            assertEquals(expectedProjectId, project.projectId)
            assertEquals(expectedProjectName, project.name)
            assertTrue(project.team is LazyModelReference)
        } ?: fail("Response child was null or not a LazyModelReference")
    }

    @Test
    fun query_team_and_pull_belongsTo_project_includes() = runTest {
        val expectedProjectId = "GraphQLLazyQueryInstrumentationTest-Parent1"
        val expectedProjectName = "Project 1"
        val expectedTeamId = "GraphQLLazyQueryInstrumentationTest-Team1"
        val expectedTeamName = "Team 1"
        val projectRequest = ModelQuery.get<Team, TeamPath>(
            Team::class.java,
            Team.TeamIdentifier(expectedTeamId, expectedTeamName)
        ) {
            includes(it.project)
        }

        val teamFromResponse = client.query(projectRequest).getOrThrow().data

        assertEquals(expectedTeamId, teamFromResponse.teamId)
        assertEquals(expectedTeamName, teamFromResponse.name)

        (teamFromResponse.project as? LoadedModelReference)?.value?.let { project ->
            assertEquals(expectedProjectId, project.projectId)
            assertEquals(expectedProjectName, project.name)
            assertTrue(project.team is LazyModelReference)
        } ?: fail("Response child was null or not a LoadedModelReference")
    }

    @Test
    fun query_with_complex_includes() = runTest {
        val expectedBlogName = "Blog 1"
        val expectedPostId = "GraphQLLazyQueryInstrumentationTest-Post1"
        val expectedPostTitle = "Post 1"
        val expectedCommentContent = "Comment 1"

        val request = ModelQuery.get<Post, PostPath>(
            Post::class.java,
            Post.PostIdentifier(expectedPostId, expectedPostTitle)
        ) {
            includes(it.blog.posts.blog.posts.comments, it.comments.post.blog.posts.comments)
        }
        val post = client.query(request).getOrThrow().data

        // Scenario 1: it.blog.posts.blog.posts.comments
        val l1Blog = (post.blog as LoadedModelReference).value!!
        assertEquals(expectedBlogName, l1Blog.name)
        val l2Posts = (l1Blog.posts as LoadedModelList).items
        assertEquals(1, l2Posts.size)
        assertEquals(expectedPostTitle, l2Posts[0].title)
        val l3Blog = (l2Posts[0].blog as LoadedModelReference).value!!
        assertEquals(expectedBlogName, l3Blog.name)
        val l4Posts = (l3Blog.posts as LoadedModelList).items
        assertEquals(1, l4Posts.size)
        assertEquals(expectedPostTitle, l4Posts[0].title)
        val l5Comments = (l4Posts[0].comments as LoadedModelList).items
        assertEquals(1, l5Comments.size)
        assertEquals(expectedCommentContent, l5Comments[0].content)

        // Scenario 2: it.comments.post.blog.posts.comments
        val s2l1Comments = (post.comments as LoadedModelList).items
        assertEquals(1, s2l1Comments.size)
        assertEquals(expectedCommentContent, s2l1Comments[0].content)
        val l2Post = (s2l1Comments[0].post as LoadedModelReference).value!!
        assertEquals(expectedPostTitle, l2Post.title)
        val s2l3Blog = (l2Posts[0].blog as LoadedModelReference).value!!
        assertEquals(expectedBlogName, s2l3Blog.name)
        val s2l4Posts = (s2l3Blog.posts as LoadedModelList).items
        assertEquals(1, s2l4Posts.size)
        assertEquals(expectedPostTitle, s2l4Posts[0].title)
        val s2l5Comments = (s2l4Posts[0].comments as LoadedModelList).items
        assertEquals(1, s2l5Comments.size)
        assertEquals(expectedCommentContent, s2l5Comments[0].content)
    }

    @Test
    fun query_multiple_lazy_loads_no_includes() = runTest {
        val expectedBlogName = "Blog 1"
        val expectedPostId = "GraphQLLazyQueryInstrumentationTest-Post1"
        val expectedPostTitle = "Post 1"
        val expectedCommentContent = "Comment 1"

        val request = ModelQuery[
            Post::class.java,
            Post.PostIdentifier(expectedPostId, expectedPostTitle)
        ]
        val post = client.query(request).getOrThrow().data

        // Scenario 1: Start loads from lazy reference of blog
        val s1l1Blog = (post.blog as LazyModelReference).fetchModel()!!
        assertEquals(expectedBlogName, s1l1Blog.name)
        val s1l2Posts = (s1l1Blog.posts as LazyModelList).fetchAllPages()
        assertEquals(1, s1l2Posts.size)
        assertEquals(expectedPostTitle, s1l2Posts[0].title)
        val s1l3Blog = (s1l2Posts[0].blog as LazyModelReference).fetchModel()!!
        assertEquals(expectedBlogName, s1l3Blog.name)
        val s1l3Comments = (s1l2Posts[0].comments as LazyModelList).fetchAllPages()
        assertEquals(1, s1l3Comments.size)
        assertEquals(expectedCommentContent, s1l3Comments[0].content)

        // Scenario 2: Start loads from model list of comments
        val s2l1Comments = (post.comments as LazyModelList).fetchAllPages()
        assertEquals(1, s2l1Comments.size)
        assertEquals(expectedCommentContent, s2l1Comments[0].content)
        val s2l2Post = (s1l3Comments[0].post as LazyModelReference).fetchModel()!!
        assertEquals(expectedPostTitle, s2l2Post.title)
        val s2l3Blog = (s2l2Post.blog as LazyModelReference).fetchModel()!!
        assertEquals(expectedBlogName, s2l3Blog.name)
        val s2l3Comments = (s2l2Post.comments as LazyModelList).fetchAllPages()
        assertEquals(1, s2l3Comments.size)
        assertEquals(expectedCommentContent, s2l3Comments[0].content)
    }
}
