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

package com.amplifyframework.api.aws

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.api.aws.test.R
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.model.LazyModelList
import com.amplifyframework.core.model.LazyModelReference
import com.amplifyframework.core.model.LoadedModelList
import com.amplifyframework.core.model.LoadedModelReference
import com.amplifyframework.core.model.PaginationToken
import com.amplifyframework.core.model.includes
import com.amplifyframework.datastore.generated.model.HasManyChild
import com.amplifyframework.datastore.generated.model.HasManyChild.HasManyChildIdentifier
import com.amplifyframework.datastore.generated.model.HasManyChildPath
import com.amplifyframework.datastore.generated.model.Parent
import com.amplifyframework.datastore.generated.model.ParentPath
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.PostPath
import com.amplifyframework.datastore.generated.model.Project
import com.amplifyframework.datastore.generated.model.ProjectPath
import com.amplifyframework.datastore.generated.model.Team
import com.amplifyframework.datastore.generated.model.TeamPath
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class GraphQLLazyQueryInstrumentationTest {

    companion object {

        const val PARENT1_ID = "GraphQLLazyQueryInstrumentationTest-Parent"
        const val PARENT2_ID = "GraphQLLazyQueryInstrumentationTest-Parent2"
        const val HAS_ONE_CHILD1_ID = "GraphQLLazyQueryInstrumentationTest-HasOneChild1"
        const val HAS_ONE_CHILD2_ID = "GraphQLLazyQueryInstrumentationTest-HasOneChild2"
        @JvmStatic
        @BeforeClass
        fun setUp() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val config = AmplifyConfiguration.fromConfigFile(context, R.raw.amplifyconfigurationlazy)
            Amplify.addPlugin(AWSApiPlugin())
            Amplify.configure(config, context)
        }
    }

    // run this method once to populate all the data necessary to run the tests
//    private suspend fun populate() {
//        val hasOneChild = HasOneChild.builder()
//            .content("Child1")
//            .id("GraphQLLazyQueryInstrumentationTest-HasOneChild1")
//            .build()
//        Amplify.API.mutate(ModelMutation.create(hasOneChild))
//
//        val parent = Parent.builder().parentChildId(hasOneChild.id).id("GraphQLLazyQueryInstrumentationTest-Parent").build()
//        Amplify.API.mutate(ModelMutation.create(parent))
//
//        val hasOneChild2 = HasOneChild.builder()
//            .content("Child2")
//            .id("GraphQLLazyQueryInstrumentationTest-HasOneChild2")
//            .build()
//        Amplify.API.mutate(ModelMutation.create(hasOneChild2))
//
//        val parent2 = Parent.builder().parentChildId(hasOneChild2.id).id("GraphQLLazyQueryInstrumentationTest-Parent2").build()
//        Amplify.API.mutate(ModelMutation.create(parent2))
//
//        for(i in 0 until 1001) {
//            val hasManyChild = HasManyChild.builder()
//                .content("Child$i")
//                .id("GraphQLLazyQueryInstrumentationTest-HasManyChild$i")
//                .parent(parent)
//                .build()
//            Amplify.API.mutate(ModelMutation.create(hasManyChild))
//        }
//
//        val parentNoChildren = Parent.builder().id("GraphQLLazyQueryInstrumentationTest.ParentWithNoChildren").build()
//        Amplify.API.mutate(ModelMutation.create(parentNoChildren))
//
//        val hasManyChild = HasManyChild.builder()
//        .content("ChildNoParent")
//        .id("GraphQLLazyQueryInstrumentationTest.HasManyChildNoParent")
//        .build()
//
//        Amplify.API.mutate(ModelMutation.create(hasManyChild))
//
//        val project = Project.builder()
//            .projectId("GraphQLLazyQueryInstrumentationTest-Parent1")
//            .name("Project 1")
//            .build()
//        val projectFromResponse = Amplify.API.mutate(ModelMutation.create(project)).data
//
//        val team = Team.builder()
//            .teamId("GraphQLLazyQueryInstrumentationTest-Team1")
//            .name("Team 1")
//            .project(project)
//            .build()
//        Amplify.API.mutate(ModelMutation.create(team))
//
//
//        val updateProject = projectFromResponse.copyOfBuilder()
//            .projectTeamName("Team 1")
//            .projectTeamTeamId("GraphQLLazyQueryInstrumentationTest-Team1")
//            .build()
//        Amplify.API.mutate(ModelMutation.update(updateProject))
//
//        val blog = Blog.builder()
//            .blogId("GraphQLLazyQueryInstrumentationTest-Blog1")
//            .name("Blog 1")
//            .build()
//        val post = Post.builder()
//            .postId("GraphQLLazyQueryInstrumentationTest-Post1")
//            .title("Post 1")
//            .blog(blog)
//            .build()
//        val comment = Comment.builder()
//            .commentId("GraphQLLazyQueryInstrumentationTest-Comment1")
//            .content("Comment 1")
//            .post(post)
//            .build()
//        Amplify.API.mutate(ModelMutation.create(blog))
//        Amplify.API.mutate(ModelMutation.create(post))
//        Amplify.API.mutate(ModelMutation.create(comment))
//    }

    @Test
    fun query_parent_no_includes() = runTest {
        // GIVEN
        val request = ModelQuery[Parent::class.java, Parent.ParentIdentifier(PARENT1_ID)]

        // WHEN
        val responseParent = Amplify.API.query(request).data

        // THEN
        assertEquals(HAS_ONE_CHILD1_ID, responseParent.parentChildId)
        (responseParent.child as? LazyModelReference)?.fetchModel()?.let { child ->
            assertEquals(HAS_ONE_CHILD1_ID, child.id)
            assertEquals("Child1", child.content)
        } ?: fail("Response child was null or not a LazyModelReference")

        val children = responseParent.children as LazyModelList
        var children1HasNextPage = true
        var children1NextToken: PaginationToken? = null
        var children1Count = 0
        while (children1HasNextPage) {
            val page = children.fetchPage(children1NextToken)
            children1HasNextPage = page.hasNextPage
            children1NextToken = page.nextToken
            children1Count += page.items.size
        }
        assertEquals(1001, children1Count)
    }

    @Test
    fun query_parent_with_includes() = runTest {
        // GIVEN
        val request = ModelQuery.get<Parent, ParentPath>(
            Parent::class.java, Parent.ParentIdentifier(PARENT1_ID)
        ) {
            includes(it.child, it.children)
        }

        // WHEN
        val responseParent = Amplify.API.query(request).data

        // THEN
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
    fun query_list_with_no_includes() = runTest {

        val request = ModelQuery.list(
            Parent::class.java,
            Parent.ID.beginsWith("GraphQLLazyQueryInstrumentationTest-Parent")
        )

        // WHEN
        val paginatedResult = Amplify.API.query(request).data

        assertFalse(paginatedResult.hasNextResult())

        // THEN
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
        var children1HasNextPage = true
        var children1NextToken: PaginationToken? = null
        var children1Count = 0
        while (children1HasNextPage) {
            val page = childrenFromParent1.fetchPage(children1NextToken)
            children1HasNextPage = page.hasNextPage
            children1NextToken = page.nextToken
            children1Count += page.items.size
        }
        assertEquals(1001, children1Count)

        assertEquals(HAS_ONE_CHILD2_ID, parent2.parentChildId)
        assertEquals(PARENT2_ID, parent2.id)
        (parent2.child as? LazyModelReference)?.fetchModel()?.let { child ->
            assertEquals(HAS_ONE_CHILD2_ID, child.id)
            assertEquals("Child2", child.content)
        } ?: fail("Response child was null or not a LazyModelReference")

        val childrenFromParent2 = parent2.children as LazyModelList
        var children2HasNextPage = true
        var children2NextToken: PaginationToken? = null
        var children2Count = 0
        while (children2HasNextPage) {
            val page = childrenFromParent2.fetchPage(children2NextToken)
            children2HasNextPage = page.hasNextPage
            children2NextToken = page.nextToken
            children2Count += page.items.size
        }
        assertEquals(0, children2Count)
    }

    @Test
    fun query_list_with_includes() = runTest {

        val request = ModelQuery.list<Parent, ParentPath>(
            Parent::class.java,
            Parent.ID.beginsWith("GraphQLLazyQueryInstrumentationTest-Parent")
        ) {
            includes(it.child, it.children)
        }

        // WHEN
        val paginatedResult = Amplify.API.query(request).data

        // THEN
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
        // GIVEN
        val request = ModelQuery.get<Parent, ParentPath>(
            Parent::class.java, Parent.ParentIdentifier("GraphQLLazyQueryInstrumentationTest.ParentWithNoChildren")
        ) {
            includes(it.child, it.children)
        }

        // WHEN
        val responseParent = Amplify.API.query(request).data

        // THEN
        assertNull(responseParent.parentChildId)
        (responseParent.child as? LoadedModelReference)?.let { childRef ->
            assertNull(childRef.value)
        } ?: fail("Response child was null or not a LoadedModelReference")

        val children = responseParent.children as LoadedModelList
        assertEquals(0, children.items.size)
    }

    @Test
    fun query_parent_with_no_child_no_includes() = runTest {
        // GIVEN
        val request = ModelQuery[
            Parent::class.java,
            Parent.ParentIdentifier("GraphQLLazyQueryInstrumentationTest.ParentWithNoChildren")
        ]

        // WHEN
        val responseParent = Amplify.API.query(request).data

        // THEN
        assertNull(responseParent.parentChildId)
        (responseParent.child as? LoadedModelReference)?.let { childRef ->
            assertNull(childRef.value)
        } ?: fail("Response child was null or not a LoadedModelReference")

        val children = responseParent.children as LazyModelList
        assertEquals(0, children.fetchPage().items.size)
    }

    @Test
    fun query_child_belongsTo_parent_with_no_includes() = runTest {
        // GIVEN
        val request = ModelQuery[
            HasManyChild::class.java,
            HasManyChildIdentifier("GraphQLLazyQueryInstrumentationTest-HasManyChild1")
        ]

        // WHEN
        val hasManyChild = Amplify.API.query(request).data

        // THEN
        (hasManyChild.parent as? LazyModelReference)?.let { parentRef ->
            assertEquals(PARENT1_ID, parentRef.fetchModel()!!.id)
        } ?: fail("Response child was null or not a LazyModelReference")
    }

    @Test
    fun query_child_belongsTo_parent_with_includes() = runTest {
        // GIVEN
        val request = ModelQuery.get<HasManyChild, HasManyChildPath>(
            HasManyChild::class.java, HasManyChildIdentifier("GraphQLLazyQueryInstrumentationTest-HasManyChild1")
        ) {
            includes(it.parent)
        }

        // WHEN
        val hasManyChild = Amplify.API.query(request).data

        // THEN
        (hasManyChild.parent as? LoadedModelReference)?.let { parentRef ->
            assertEquals(PARENT1_ID, parentRef.value!!.id)
        } ?: fail("Response child was null or not a LoadedModelReference")
    }

    @Test
    fun query_child_belongsTo_null_parent_with_no_includes() = runTest {
        // GIVEN
        val request = ModelQuery[
            HasManyChild::class.java,
            HasManyChildIdentifier("GraphQLLazyQueryInstrumentationTest.HasManyChildNoParent")
        ]

        // WHEN
        val hasManyChild = Amplify.API.query(request).data

        // THEN
        (hasManyChild.parent as? LoadedModelReference)?.let { parentRef ->
            assertEquals(null, parentRef.value)
        } ?: fail("Response child was null or not a LoadedModelReference")
    }

    @Test
    fun query_list_child_belongsTo_null_parent_with_no_includes() = runTest {
        // GIVEN
        val request = ModelQuery.list(
            HasManyChild::class.java,
            HasManyChild.ID.beginsWith("GraphQLLazyQueryInstrumentationTest.HasManyChildNoParent")
        )

        // WHEN
        val hasManyChildren = Amplify.API.query(request).data.toList()

        // THEN
        assertEquals(1, hasManyChildren.size)
        (hasManyChildren[0].parent as? LoadedModelReference)?.let { parentRef ->
            assertEquals(null, parentRef.value)
        } ?: fail("Response child was null or not a LoadedModelReference")
    }

    @Test
    fun query_child_belongsTo_null_parent_with_includes() = runTest {
        // GIVEN
        val request = ModelQuery.list<HasManyChild, HasManyChildPath>(
            HasManyChild::class.java,
            HasManyChild.ID.beginsWith("GraphQLLazyQueryInstrumentationTest.HasManyChildNoParent")
        ) {
            includes(it.parent)
        }

        // WHEN
        val hasManyChildren = Amplify.API.query(request).data.toList()

        // THEN
        assertEquals(1, hasManyChildren.size)
        (hasManyChildren[0].parent as? LoadedModelReference)?.let { parentRef ->
            assertEquals(null, parentRef.value)
        } ?: fail("Response child was null or not a LoadedModelReference")
    }

    @Test
    fun query_project_and_pull_hasOne_team_no_includes() = runTest {
        // GIVEN
        val expectedProjectId = "GraphQLLazyQueryInstrumentationTest-Parent1"
        val expectedProjectName = "Project 1"
        val expectedTeamId = "GraphQLLazyQueryInstrumentationTest-Team1"
        val expectedTeamName = "Team 1"
        val projectRequest = ModelQuery[
            Project::class.java,
            Project.ProjectIdentifier(expectedProjectId, expectedProjectName)
        ]

        // WHEN
        val projectFromResponse = Amplify.API.query(projectRequest).data

        // THEN
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
        // GIVEN
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

        // WHEN
        val projectFromResponse = Amplify.API.query(projectRequest).data

        // THEN
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
        // GIVEN
        val expectedProjectId = "GraphQLLazyQueryInstrumentationTest-Parent1"
        val expectedProjectName = "Project 1"
        val expectedTeamId = "GraphQLLazyQueryInstrumentationTest-Team1"
        val expectedTeamName = "Team 1"
        val teamRequest = ModelQuery[
            Team::class.java,
            Team.TeamIdentifier(expectedTeamId, expectedTeamName)
        ]

        // WHEN
        val teamFromResponse = Amplify.API.query(teamRequest).data

        // THEN
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
        // GIVEN
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

        // WHEN
        val teamFromResponse = Amplify.API.query(projectRequest).data

        // THEN
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
        // GIVEN
        val expectedBlogName = "Blog 1"
        val expectedPostId = "GraphQLLazyQueryInstrumentationTest-Post1"
        val expectedPostTitle = "Post 1"
        val expectedCommentConent = "Comment 1"

        // WHEN
        val request = ModelQuery.get<Post, PostPath>(
            Post::class.java,
            Post.PostIdentifier(expectedPostId, expectedPostTitle)
        ) {
            includes(it.blog.posts.blog.posts.comments, it.comments.post.blog.posts.comments)
        }
        val post = Amplify.API.query(request).data

        // THEN

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
        assertEquals(expectedCommentConent, l5Comments[0].content)

        // Scenario 2: it.comments.post.blog.posts.comments
        val s2l1Comments = (post.comments as LoadedModelList).items
        assertEquals(1, s2l1Comments.size)
        assertEquals(expectedCommentConent, s2l1Comments[0].content)
        val l2Post = (s2l1Comments[0].post as LoadedModelReference).value!!
        assertEquals(expectedPostTitle, l2Post.title)
        val s2l3Blog = (l2Posts[0].blog as LoadedModelReference).value!!
        assertEquals(expectedBlogName, s2l3Blog.name)
        val s2l4Posts = (s2l3Blog.posts as LoadedModelList).items
        assertEquals(1, s2l4Posts.size)
        assertEquals(expectedPostTitle, s2l4Posts[0].title)
        val s2l5Comments = (s2l4Posts[0].comments as LoadedModelList).items
        assertEquals(1, s2l5Comments.size)
        assertEquals(expectedCommentConent, s2l5Comments[0].content)
    }

    @Test
    fun query_multiple_lazy_loads_no_includes() = runTest {
        // GIVEN
        val expectedBlogName = "Blog 1"
        val expectedPostId = "GraphQLLazyQueryInstrumentationTest-Post1"
        val expectedPostTitle = "Post 1"
        val expectedCommentConent = "Comment 1"

        // WHEN
        val request = ModelQuery[
            Post::class.java,
            Post.PostIdentifier(expectedPostId, expectedPostTitle)
        ]
        val post = Amplify.API.query(request).data

        // THEN

        // Scenario 1: Start loads from lazy reference of blog
        val s1l1Blog = (post.blog as LazyModelReference).fetchModel()!!
        assertEquals(expectedBlogName, s1l1Blog.name)
        val s1l2Posts = (s1l1Blog.posts as LazyModelList).fetchPage().items
        assertEquals(1, s1l2Posts.size)
        assertEquals(expectedPostTitle, s1l2Posts[0].title)
        val s1l3Blog = (s1l2Posts[0].blog as LazyModelReference).fetchModel()!!
        assertEquals(expectedBlogName, s1l3Blog.name)
        val s1l3Comments = (s1l2Posts[0].comments as LazyModelList).fetchPage().items
        assertEquals(1, s1l3Comments.size)
        assertEquals(expectedCommentConent, s1l3Comments[0].content)

        // Scenario 1: Start loads from model list of comments
        val s2l1Comments = (post.comments as LazyModelList).fetchPage().items
        assertEquals(1, s2l1Comments.size)
        assertEquals(expectedCommentConent, s2l1Comments[0].content)
        val s2l2Post = (s1l3Comments[0].post as LazyModelReference).fetchModel()!!
        assertEquals(expectedPostTitle, s2l2Post.title)
        val s2l3Blog = (s2l2Post.blog as LazyModelReference).fetchModel()!!
        assertEquals(expectedBlogName, s2l3Blog.name)
        val s2l3Comments = (s2l2Post.comments as LazyModelList).fetchPage().items
        assertEquals(1, s2l3Comments.size)
        assertEquals(expectedCommentConent, s2l3Comments[0].content)
    }
}
