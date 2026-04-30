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
import com.amazonaws.appsync.test.R
import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.api.graphql.model.ModelSubscription
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.foundation.result.getOrThrow
import com.amplifyframework.testmodels.personcar.MaritalStatus
import com.amplifyframework.testmodels.personcar.Person
import com.amplifyframework.testmodels.ratingsblog.Blog
import com.amplifyframework.testmodels.ratingsblog.Post
import com.amplifyframework.testmodels.ratingsblog.PostEditor
import com.amplifyframework.testmodels.ratingsblog.Rating
import com.amplifyframework.testmodels.ratingsblog.User
import com.amplifyframework.testmodels.teamproject.Projectfields
import com.amplifyframework.testmodels.teamproject.Team
import com.amplifyframework.testutils.DeviceFarmTestBase
import com.amplifyframework.testutils.ModelAssert
import com.amplifyframework.testutils.Resources
import java.text.SimpleDateFormat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Integration test that verifies [AmplifyAppSyncClient] can perform the same codegen-based
 * GraphQL operations as the existing AWSApiPlugin. Mirrors the test scenarios from
 * [com.amplifyframework.api.aws.CodeGenerationInstrumentationTest] in the :aws-api module.
 */
@OptIn(ExperimentalAmplifyApi::class, InternalAmplifyApi::class)
class CodeGenerationInstrumentationTest : DeviceFarmTestBase() {

    companion object {
        private lateinit var client: AmplifyAppSyncClient

        @JvmStatic
        @BeforeClass
        fun setUp() {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val config = Resources.readAsJson(context, R.raw.appsync_client_config)
            client = AmplifyAppSyncClient(
                AmplifyAppSyncClient.Configuration {
                    endpoint = config.getString("endpoint")
                    authorization = AppSyncAuthorization.Single(
                        AppSyncClientAuthorizer.ApiKey(config.getString("apiKey"))
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

    // ── Tests ───────────────────────────────────────────────────────────

    /**
     * Mutates an object, and then queries for its value back. Asserts that the two values are the same.
     * This tests our ability to generate GraphQL queries at runtime, from model primitives,
     * for both queries and mutations. The query also tests functionality of the QueryPredicate filter.
     */
    @Test
    fun queryMatchesMutationResult() = runTest {
        val david = Person.builder()
            .firstName("David")
            .lastName("Daudelin")
            .age(29)
            .dob(Temporal.Date(SimpleDateFormat("MM/dd/yyyy").parse("07/25/1990")!!))
            .relationship(MaritalStatus.married)
            .build()

        val createdPerson = client.mutate(ModelMutation.create(david)).getOrThrow().data
        ModelAssert.assertEqualsIgnoringTimestamps(david, createdPerson)

        // Do NOT ignore createdAt/updatedAt fields here to confirm that synced items have same values.
        val queriedPerson = client.query(ModelQuery.get(Person::class.java, createdPerson.id)).getOrThrow().data
        assertEquals(createdPerson, queriedPerson)
    }

    /**
     * Tests the code generation for LIST query with a predicate.
     */
    @Test
    fun queryListWithPredicate() = runTest {
        val matchingPeople = client.query(
            ModelQuery.list(
                Person::class.java,
                Person.LAST_NAME.eq("Daudelin")
                    .and(
                        Person.FIRST_NAME.eq("David")
                            .or(Person.FIRST_NAME.eq("Sarah"))
                    )
            )
        ).getOrThrow().data

        for (person in matchingPeople) {
            assertTrue(person.firstName in listOf("David", "Sarah"))
            assertEquals("Daudelin", person.lastName)
        }
    }

    /**
     * Tests that a subscription can receive an event when a create mutation takes place.
     */
    @Test
    fun subscribeReceivesMutationEvent() = runTest {
        val johnDoe = Person.builder()
            .firstName("John")
            .lastName("Doe")
            .build()

        val connected = CompletableDeferred<Unit>()
        val dataDeferred = async(Dispatchers.Default) {
            withTimeout(30_000) {
                client.subscribe(ModelSubscription.onCreate(Person::class.java))
                    .onEach { if (it is SubscriptionEvent.Connection.Connected) connected.complete(Unit) }
                    .filterIsInstance<SubscriptionEvent.Data<Person>>()
                    .first()
                    .response
                    .data
            }
        }

        connected.await()
        client.mutate(ModelMutation.create(johnDoe)).getOrThrow()

        val receivedPerson = dataDeferred.await()
        assertEquals(johnDoe.firstName, receivedPerson.firstName)
        assertEquals(johnDoe.lastName, receivedPerson.lastName)
    }

    /**
     * Creates an object and tries to update it with a false condition on the existing data which should fail to
     * ensure mutation condition filtering works. Then it sends a correct condition to delete which should succeed.
     */
    @Test
    fun mutationFailsInvalidConditionAndPassesCorrectCondition() = runTest {
        val person = Person.builder()
            .firstName("David")
            .lastName("Daudelin")
            .age(29)
            .relationship(MaritalStatus.married)
            .build()
        client.mutate(ModelMutation.create(person)).getOrThrow()

        // Update with wrong condition — expect errors in the response
        val updateResponse = client.mutate(
            ModelMutation.update(person.newBuilder().age(30).build(), Person.LAST_NAME.eq("Dandelion"))
        ).getOrThrow()
        assertTrue(updateResponse.hasErrors())
        assertTrue(updateResponse.errors[0].toString().contains("ConditionalCheckFailedException"))

        // Clean up
        client.mutate(ModelMutation.delete(Person.justId(person.resolveIdentifier()))).getOrThrow()
    }

    /**
     * For a model having a [BelongsTo] relationship to another model, validate
     * successful query, mutations, subscription.
     */
    @Test
    fun belongsToRelationship() = runTest {
        val connected = CompletableDeferred<Unit>()
        val dataDeferred = async(Dispatchers.Default) {
            withTimeout(30_000) {
                client.subscribe(ModelSubscription.onCreate(Projectfields::class.java))
                    .onEach { if (it is SubscriptionEvent.Connection.Connected) connected.complete(Unit) }
                    .filterIsInstance<SubscriptionEvent.Data<Projectfields>>()
                    .first()
                    .response
                    .data
            }
        }

        // Wait for subscription to be established
        connected.await()

        // Create a team
        val team = Team.builder().name("AWS Mobile SDK").build()
        client.mutate(ModelMutation.create(team)).getOrThrow()

        // Create a Projectfields referencing that team
        val projectfields = Projectfields.builder()
            .name("API Codegen")
            .team(Team.justId(team.id))
            .build()
        client.mutate(ModelMutation.create(projectfields)).getOrThrow()

        // Validate that subscription received the newly created projectfields with the correct team
        val projectfieldsOnSubscription = dataDeferred.await()
        assertEquals("API Codegen", projectfieldsOnSubscription.name)
        assertEquals(team, projectfieldsOnSubscription.team)

        // Query — the referenced team should match
        val queriedProjectfields = client.query(
            ModelQuery.get(Projectfields::class.java, projectfieldsOnSubscription.id)
        ).getOrThrow().data
        assertEquals(team, queriedProjectfields.team)
    }

    /**
     * Tests the code generation for HAS_MANY relationship.
     */
    @Test
    fun hasManyRelationship() = runTest {
        // Create a blog
        val blog = Blog.builder()
            .name("All Things Amplify")
            .tags(listOf("amazon", "amplify", "framework", "software"))
            .build()
        assertEquals(blog, client.mutate(ModelMutation.create(blog)).getOrThrow().data)

        // Create a post, associated to that blog
        val post = Post.builder().title("Test 1").blog(blog).build()
        val createdPost = client.mutate(ModelMutation.create(post)).getOrThrow().data

        // Validate that created post has same fields
        assertEquals(post.id, createdPost.id)
        assertEquals(post.title, createdPost.title)
        assertEquals(post.blog.id, createdPost.blog.id)
        assertEquals(post.blog.name, createdPost.blog.name)
        assertEquals(post.blog.tags, createdPost.blog.tags)

        // Get the blog, and ensure that posts are associated to it on the endpoint
        val queriedBlog = client.query(ModelQuery.get(Blog::class.java, blog.id)).getOrThrow().data
        val firstPost = queriedBlog.posts[0]
        assertEquals(post.id, firstPost.id)
        assertEquals(post.title, firstPost.title)
    }

    /**
     * Tests the code generation for HAS_ONE relationship.
     */
    @Test
    fun hasOneRelationship() = runTest {
        // Create a blog
        val blog = Blog.builder().name("Necessary blog for post").build()
        client.mutate(ModelMutation.create(blog)).getOrThrow()

        // Associate a post to that blog
        val post = Post.builder().title("Test post").blog(blog).build()
        client.mutate(ModelMutation.create(post)).getOrThrow()

        // Make a rating for the post
        val rating = Rating.builder().stars(5).post(post).build()
        val createdRating = client.mutate(ModelMutation.create(rating)).getOrThrow().data

        // Validate that rating that exists on the endpoint refers to the original post
        assertEquals(post, createdRating.post)
    }

    /**
     * Tests the code generation for a Many to Many relationship simulated through two HasMany relationships.
     */
    @Test
    fun manyToManyRelationship() = runTest {
        // Create a blog
        val blog = Blog.builder().name("Necessary blog for post").build()
        client.mutate(ModelMutation.create(blog)).getOrThrow()

        // Which contains a post
        val post = Post.builder().title("Test post").blog(blog).build()
        client.mutate(ModelMutation.create(post)).getOrThrow()

        // Create a user
        val user = User.builder().username("Patches46").build()
        client.mutate(ModelMutation.create(user)).getOrThrow()

        // The user is an editor of the created post
        val editor = PostEditor.builder().post(post).editor(user).build()
        client.mutate(ModelMutation.create(editor)).getOrThrow()

        // Now, see what was actually setup on the endpoint, by querying for post and user
        val queriedPost = client.query(ModelQuery.get(Post::class.java, post.id)).getOrThrow().data
        val queriedUser = client.query(ModelQuery.get(User::class.java, user.id)).getOrThrow().data

        // Validate that associations are setup correctly on the objects returned from endpoint
        assertEquals(1, queriedPost.editors.size)
        assertEquals(user, queriedPost.editors[0].editor)
        assertEquals(1, queriedUser.posts.size)
        assertEquals(post.title, queriedUser.posts[0].post.title)
        assertEquals(post.id, queriedUser.posts[0].post.id)
    }
}
