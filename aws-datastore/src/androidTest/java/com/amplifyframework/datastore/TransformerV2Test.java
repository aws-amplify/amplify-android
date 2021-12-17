/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.datastore;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.amplifyframework.datastore.DataStoreHubEventFilters.publicationOf;
import static com.amplifyframework.datastore.DataStoreHubEventFilters.receiptOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.appsync.SynchronousAppSync;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.testmodels.transformerv2.*;
import com.amplifyframework.testutils.HubAccumulator;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.random.RandomString;
import com.amplifyframework.testutils.sync.SynchronousApi;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;

public final class TransformerV2Test {
    private static final String AMPLIFY_CONFIG = "amplifyconfigurationv2";
    private static final int TIMEOUT_SECONDS = 120;

    private static SynchronousApi api;
    private static SynchronousAppSync appSync;
    private static SynchronousDataStore dataStore;

    @BeforeClass
    public static void setUp() throws AmplifyException {
        Context context = getApplicationContext();
        StrictMode.enable();

        // Setup an API
        @RawRes int configResourceId = Resources.getRawResourceId(context, AMPLIFY_CONFIG);
        CategoryConfiguration apiCategoryConfiguration =
                AmplifyConfiguration.fromConfigFile(context, configResourceId)
                        .forCategoryType(CategoryType.API);
        ApiCategory apiCategory = new ApiCategory();
        apiCategory.addPlugin(new AWSApiPlugin());
        apiCategory.configure(apiCategoryConfiguration, context);

        // To arrange and verify state, we need to access the supporting AppSync API
        api = SynchronousApi.delegatingTo(apiCategory);
        appSync = SynchronousAppSync.using(AppSyncClient.via(apiCategory));
        DataStoreCategory dataStoreCategory = DataStoreCategoryConfigurator.begin()
                .api(apiCategory)
                .clearDatabase(true)
                .context(context)
                .modelProvider(AmplifyModelProvider.getInstance())
                .resourceId(configResourceId)
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .dataStoreConfiguration(DataStoreConfiguration.defaults())
                .finish();
        dataStore = SynchronousDataStore.delegatingTo(dataStoreCategory);
    }

    @AfterClass
    public static void tearDown() throws DataStoreException {
        if (dataStore != null) {
            try {
                dataStore.clear();
            } catch (Exception error) {
                // ok to ignore since problem encountered during tear down of the test.
            }
        }
    }

    @Test
    public void testCreateModelWithSecondaryIndex() throws AmplifyException {
        // Create customer
        Customer1 customer = Customer1.builder()
                .name(RandomString.string())
                .accountRepresentativeId(RandomString.string())
                .build();
        saveAndWaitForSync(customer);

        // Assert synced
        Customer1 remoteCustomer = api.get(Customer1.class, customer.getId());
        Customer1 localCustomer = dataStore.get(Customer1.class, customer.getId());
        assertEquals(localCustomer, remoteCustomer);
    }

    @Test
    public void testUpdateModelWithSecondaryIndex() throws AmplifyException {
        // Create customer
        Customer1 customer = Customer1.builder()
                .name(RandomString.string())
                .accountRepresentativeId(RandomString.string())
                .build();
        saveAndWaitForSync(customer);

        // Update customer
        Customer1 updatedCustomer = customer.copyOfBuilder()
                .name(RandomString.string())
                .build();
        saveAndWaitForSync(updatedCustomer);

        // Assert synced
        Customer1 remoteCustomer = api.get(Customer1.class, customer.getId());
        Customer1 localCustomer = dataStore.get(Customer1.class, customer.getId());
        assertEquals(localCustomer, remoteCustomer);
        assertEquals(updatedCustomer.getName(), remoteCustomer.getName());
    }

    @Test
    public void testDeleteModelWithSecondaryIndex() throws AmplifyException {
        // Create customer
        Customer1 customer = Customer1.builder()
                .name(RandomString.string())
                .accountRepresentativeId(RandomString.string())
                .build();
        saveAndWaitForSync(customer);

        // Delete customer
        TestObserver<String> onDelete = subscribeOnDelete(Customer1.class);
        deleteAndWaitForPublish(customer);

        // Assert synced
        onDelete.awaitCount(1);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(customer.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Customer1.class, customer.getId()));
    }

    @Test
    public void testModelWithDefaultFieldValue() throws AmplifyException {
        // Create todo
        Todo2 todo = Todo2.builder().build();
        saveAndWaitForSync(todo);

        // Assert synced
        Todo2 remoteTodo = api.get(Todo2.class, todo.getId());
        Todo2 localTodo = dataStore.get(Todo2.class, todo.getId());
        assertEquals(localTodo, remoteTodo);

        // Assert default content
        assertEquals("My new Todo", remoteTodo.getContent());
    }

    @Test
    public void testModelWithDefaultFieldValueOverridden() throws AmplifyException {
        // Create todo
        Todo2 todo = Todo2.builder()
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(todo);

        // Assert synced
        Todo2 remoteTodo = api.get(Todo2.class, todo.getId());
        Todo2 localTodo = dataStore.get(Todo2.class, todo.getId());
        assertEquals(localTodo, remoteTodo);

        // Assert content overriden
        assertEquals(todo.getContent(), remoteTodo.getContent());
    }

    @Test
    public void testCreateModelWithTimestamps() throws AmplifyException {
        // Create todo
        Todo3 todo = Todo3.builder()
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(todo);

        // Assert synced
        Todo3 remoteTodo = api.get(Todo3.class, todo.getId());
        Todo3 localTodo = dataStore.get(Todo3.class, todo.getId());
        assertEquals(localTodo, remoteTodo);

        // Assert that createdOn and updatedOn fields are populated properly
        long oneMinuteAgo = new Date().getTime() - TimeUnit.MINUTES.toMillis(1);
        assertTrue(remoteTodo.getCreatedOn().toDate().getTime() > oneMinuteAgo);
        assertTrue(remoteTodo.getUpdatedOn().toDate().getTime() > oneMinuteAgo);
    }

    @Test
    @Ignore("Update input is missing ID. Known CLI issue.")
    public void testUpdateModelWithTimestamps() throws AmplifyException {
        // Create todo
        Todo3 todo = Todo3.builder()
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(todo);

        // Update todo
        Todo3 updatedTodo = todo.copyOfBuilder()
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(updatedTodo);

        // Assert synced
        Todo3 remoteTodo = api.get(Todo3.class, todo.getId());
        Todo3 localTodo = dataStore.get(Todo3.class, todo.getId());
        assertEquals(localTodo, remoteTodo);
        assertEquals(updatedTodo.getContent(), remoteTodo.getContent());
    }

    @Test
    public void testDeleteModelWithTimestamps() throws AmplifyException {
        // Create todo
        Todo3 todo = Todo3.builder()
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(todo);

        // Delete todo
        TestObserver<String> onDelete = subscribeOnDelete(Todo3.class);
        deleteAndWaitForPublish(todo);

        // Assert synced
        onDelete.awaitCount(1);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(todo.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Todo3.class, todo.getId()));
    }

    @Test
    public void testCreateModelWithImplicitHasOneRelationship() throws AmplifyException {
        // Create project & team
        Project4 projectTemp = Project4.builder()
                .name(RandomString.string())
                .build();
        Team4 team = Team4.builder()
                .name(RandomString.string())
                .build();
        Project4 project = projectTemp.copyOfBuilder()
                .project4TeamId(team.getId())
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Assert synced
        Project4 remoteProject = api.get(Project4.class, project.getId());
        Project4 localProject = dataStore.get(Project4.class, project.getId());
        assertEquals(localProject, remoteProject);

        Team4 remoteTeam = api.get(Team4.class, team.getId());
        Team4 localTeam = dataStore.get(Team4.class, team.getId());
        assertEquals(localTeam, remoteTeam);

        // Assert linked
        assertEquals(team.getId(), remoteProject.getProject4TeamId());
    }

    @Test
    public void testUpdateParentModelWithImplicitHasOneRelationship() throws AmplifyException {
        // Create project & team
        Project4 projectTemp = Project4.builder()
                .name(RandomString.string())
                .build();
        Team4 team = Team4.builder()
                .name(RandomString.string())
                .build();
        Project4 project = projectTemp.copyOfBuilder()
                .project4TeamId(team.getId())
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Update project
        Project4 updatedProject = project.copyOfBuilder()
                .name(RandomString.string())
                .build();
        saveAndWaitForSync(updatedProject);

        // Assert synced
        Project4 remoteProject = api.get(Project4.class, project.getId());
        Project4 localProject = dataStore.get(Project4.class, project.getId());
        assertEquals(localProject, remoteProject);
        assertEquals(updatedProject.getName(), remoteProject.getName());
    }

    @Test
    public void testUpdateChildModelWithImplicitHasOneRelationship() throws AmplifyException {
        // Create project & team
        Project4 projectTemp = Project4.builder()
                .name(RandomString.string())
                .build();
        Team4 team = Team4.builder()
                .name(RandomString.string())
                .build();
        Project4 project = projectTemp.copyOfBuilder()
                .project4TeamId(team.getId())
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Update team
        Team4 updatedTeam = team.copyOfBuilder()
                .name(RandomString.string())
                .build();
        saveAndWaitForSync(updatedTeam);

        // Assert synced
        Team4 remoteTeam = api.get(Team4.class, team.getId());
        Team4 localTeam = dataStore.get(Team4.class, team.getId());
        assertEquals(localTeam, remoteTeam);
        assertEquals(updatedTeam.getName(), remoteTeam.getName());
    }

    @Test
    public void testDeleteModelWithImplicitHasOneRelationship() throws AmplifyException {
        // Create project & team
        Project4 projectTemp = Project4.builder()
                .name(RandomString.string())
                .build();
        Team4 team = Team4.builder()
                .name(RandomString.string())
                .build();
        Project4 project = projectTemp.copyOfBuilder()
                .project4TeamId(team.getId())
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Delete project
        TestObserver<String> onDelete = subscribeOnDelete(Project4.class, Team4.class);
        deleteAndWaitForPublish(project);

        // Assert synced
        onDelete.awaitCount(2);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(project.getId()));
        assertTrue(deletedIds.contains(team.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Project4.class, project.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Team4.class, team.getId()));
    }

    @Test
    public void testCreateModelWithExplicitHasOneRelationship() throws AmplifyException {
        // Create project & team
        Project5 projectTemp = Project5.builder()
                .name(RandomString.string())
                .build();
        Team5 team = Team5.builder()
                .name(RandomString.string())
                .build();
        Project5 project = projectTemp.copyOfBuilder()
                .teamId(team.getId())
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Assert synced
        Project5 remoteProject = api.get(Project5.class, project.getId());
        Project5 localProject = dataStore.get(Project5.class, project.getId());
        assertEquals(localProject, remoteProject);

        Team5 remoteTeam = api.get(Team5.class, team.getId());
        Team5 localTeam = dataStore.get(Team5.class, team.getId());
        assertEquals(localTeam, remoteTeam);

        // Assert linked
        assertEquals(team.getId(), remoteProject.getTeamId());
        assertEquals(remoteTeam, remoteProject.getTeam());
    }

    @Test
    public void testUpdateParentModelWithExplicitHasOneRelationship() throws AmplifyException {
        // Create project & team
        Project5 projectTemp = Project5.builder()
                .name(RandomString.string())
                .build();
        Team5 team = Team5.builder()
                .name(RandomString.string())
                .build();
        Project5 project = projectTemp.copyOfBuilder()
                .teamId(team.getId())
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Update project
        Project5 updatedProject = project.copyOfBuilder()
                .name(RandomString.string())
                .build();
        saveAndWaitForSync(updatedProject);

        // Assert synced
        Project5 remoteProject = api.get(Project5.class, project.getId());
        Project5 localProject = dataStore.get(Project5.class, project.getId());
        assertEquals(localProject, remoteProject);
        assertEquals(updatedProject.getName(), remoteProject.getName());
    }

    @Test
    public void testUpdateChildModelWithExplicitHasOneRelationship() throws AmplifyException {
        // Create project & team
        Project5 projectTemp = Project5.builder()
                .name(RandomString.string())
                .build();
        Team5 team = Team5.builder()
                .name(RandomString.string())
                .build();
        Project5 project = projectTemp.copyOfBuilder()
                .teamId(team.getId())
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Update team
        Team5 updatedTeam = team.copyOfBuilder()
                .name(RandomString.string())
                .build();
        saveAndWaitForSync(updatedTeam);

        // Assert synced
        Team5 remoteTeam = api.get(Team5.class, team.getId());
        Team5 localTeam = dataStore.get(Team5.class, team.getId());
        assertEquals(localTeam, remoteTeam);
        assertEquals(updatedTeam.getName(), remoteTeam.getName());
    }

    @Test
    public void testDeleteModelWithExplicitHasOneRelationship() throws AmplifyException {
        // Create project & team
        Project5 projectTemp = Project5.builder()
                .name(RandomString.string())
                .build();
        Team5 team = Team5.builder()
                .name(RandomString.string())
                .build();
        Project5 project = projectTemp.copyOfBuilder()
                .teamId(team.getId())
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Delete project
        TestObserver<String> onDelete = subscribeOnDelete(Project5.class, Team5.class);
        deleteAndWaitForPublish(project);

        // Assert synced
        onDelete.awaitCount(2);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(project.getId()));
        assertTrue(deletedIds.contains(team.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Project5.class, project.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Team5.class, team.getId()));
    }

    @Test
    public void testCreateModelWithImplicitHasManyRelationship() throws AmplifyException {
        // Create post & comment
        Post6 post = Post6.builder()
                .title(RandomString.string())
                .build();
        Comment6 comment = Comment6.builder()
                .content(RandomString.string())
                .post6CommentsId(post.getId())
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Assert synced
        Post6 remotePost = api.get(Post6.class, post.getId());
        Post6 localPost = dataStore.get(Post6.class, post.getId());
        assertEquals(localPost, remotePost);

        Comment6 remoteComment = api.get(Comment6.class, comment.getId());
        Comment6 localComment = dataStore.get(Comment6.class, comment.getId());
        assertEquals(localComment, remoteComment);

        // Assert linked
        assertEquals(Collections.singletonList(remoteComment), post.getComments());
    }

    @Test
    public void testUpdateParentModelWithImplicitHasManyRelationship() throws AmplifyException {
        // Create post & comment
        Post6 post = Post6.builder()
                .title(RandomString.string())
                .build();
        Comment6 comment = Comment6.builder()
                .content(RandomString.string())
                .post6CommentsId(post.getId())
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Update post
        Post6 updatedPost = post.copyOfBuilder()
                .title(RandomString.string())
                .build();
        saveAndWaitForSync(updatedPost);

        // Assert synced
        Post6 remotePost = api.get(Post6.class, post.getId());
        Post6 localPost = dataStore.get(Post6.class, post.getId());
        assertEquals(localPost, remotePost);
        assertEquals(updatedPost.getTitle(), remotePost.getTitle());
    }

    @Test
    public void testUpdateChildModelWithImplicitHasManyRelationship() throws AmplifyException {
        // Create post & comment
        Post6 post = Post6.builder()
                .title(RandomString.string())
                .build();
        Comment6 comment = Comment6.builder()
                .content(RandomString.string())
                .post6CommentsId(post.getId())
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Update comment
        Comment6 updatedComment = comment.copyOfBuilder()
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(updatedComment);

        // Assert synced
        Comment6 remoteComment = api.get(Comment6.class, comment.getId());
        Comment6 localComment = dataStore.get(Comment6.class, comment.getId());
        assertEquals(localComment, remoteComment);
        assertEquals(updatedComment.getContent(), remoteComment.getContent());
    }

    @Test
    public void testDeleteModelWithImplicitHasManyRelationship() throws AmplifyException {
        // Create post & comment
        Post6 post = Post6.builder()
                .title(RandomString.string())
                .build();
        Comment6 comment = Comment6.builder()
                .content(RandomString.string())
                .post6CommentsId(post.getId())
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Delete post
        TestObserver<String> onDelete = subscribeOnDelete(Post6.class, Comment6.class);
        deleteAndWaitForPublish(post);

        // Assert synced
        onDelete.awaitCount(2);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(post.getId()));
        assertTrue(deletedIds.contains(comment.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Post6.class, post.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Comment6.class, comment.getId()));
    }

    @Test
    public void testCreateModelWithExplicitHasManyRelationship() throws AmplifyException {
        // Create post & comment
        Post7 post = Post7.builder()
                .title(RandomString.string())
                .build();
        Comment7 comment = Comment7.builder()
                .postId(post.getId())
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Assert synced
        Post7 remotePost = api.get(Post7.class, post.getId());
        Post7 localPost = dataStore.get(Post7.class, post.getId());
        assertEquals(localPost, remotePost);

        Comment7 remoteComment = api.get(Comment7.class, comment.getId());
        Comment7 localComment = dataStore.get(Comment7.class, comment.getId());
        assertEquals(localComment, remoteComment);

        // Assert synced
        assertEquals(post.getId(), remoteComment.getPostId());
        assertEquals(Collections.singletonList(remoteComment), remotePost.getComments());
    }

    @Test
    public void testUpdateParentModelWithExplicitHasManyRelationship() throws AmplifyException {
        // Create post & comment
        Post7 post = Post7.builder()
                .title(RandomString.string())
                .build();
        Comment7 comment = Comment7.builder()
                .postId(post.getId())
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Update post
        Post7 updatedPost = post.copyOfBuilder()
                .title(RandomString.string())
                .build();
        saveAndWaitForSync(updatedPost);

        // Assert synced
        Post7 remotePost = api.get(Post7.class, post.getId());
        Post7 localPost = dataStore.get(Post7.class, post.getId());
        assertEquals(localPost, remotePost);
        assertEquals(updatedPost.getTitle(), remotePost.getTitle());
    }

    @Test
    public void testUpdateChildModelWithExplicitHasManyRelationship() throws AmplifyException {
        // Create post & comment
        Post7 post = Post7.builder()
                .title(RandomString.string())
                .build();
        Comment7 comment = Comment7.builder()
                .postId(post.getId())
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Update comment
        Comment7 updatedComment = comment.copyOfBuilder()
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(updatedComment);

        // Assert synced
        Comment7 remoteComment = api.get(Comment7.class, comment.getId());
        Comment7 localComment = dataStore.get(Comment7.class, comment.getId());
        assertEquals(localComment, remoteComment);
        assertEquals(updatedComment.getContent(), remoteComment.getContent());
    }

    @Test
    public void testDeleteModelWithExplicitHasManyRelationship() throws AmplifyException {
        // Create post & comment
        Post7 post = Post7.builder()
                .title(RandomString.string())
                .build();
        Comment7 comment = Comment7.builder()
                .postId(post.getId())
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Delete post
        TestObserver<String> onDelete = subscribeOnDelete(Post7.class, Comment7.class);
        deleteAndWaitForPublish(post);

        // Assert synced
        onDelete.awaitCount(2);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(post.getId()));
        assertTrue(deletedIds.contains(comment.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Post7.class, post.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Comment7.class, comment.getId()));
    }

    @Test
    public void testCreateModelWithManyToManyRelationship() throws AmplifyException {
        // Create post & tag
        Post8 post = Post8.builder()
                .title(RandomString.string())
                .build();
        Tag8 tag = Tag8.builder()
                .label(RandomString.string())
                .build();
        PostTags postTag = PostTags.builder()
                .post8(post)
                .tag8(tag)
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(tag);
        saveAndWaitForSync(postTag);

        // Assert synced
        Post8 remotePost = api.get(Post8.class, post.getId());
        Post8 localPost = dataStore.get(Post8.class, post.getId());
        assertEquals(localPost, remotePost);

        Tag8 remoteTag = api.get(Tag8.class, tag.getId());
        Tag8 localTag = dataStore.get(Tag8.class, tag.getId());
        assertEquals(localTag, remoteTag);

        PostTags remotePostTag = api.get(PostTags.class, postTag.getId());
        PostTags localPostTag = dataStore.get(PostTags.class, postTag.getId());
        assertEquals(localPostTag, remotePostTag);

        // Assert linked
        assertEquals(Collections.singletonList(remotePostTag), remotePost.getTags());
        assertEquals(Collections.singletonList(remotePostTag), remoteTag.getPosts());
        assertEquals(remotePost, remotePostTag.getPost8());
        assertEquals(remoteTag, remotePostTag.getTag8());
    }

    @Test
    public void testUpdateModelWithManyToManyRelationship() throws AmplifyException {
        // Create post & tag
        Post8 post = Post8.builder()
                .title(RandomString.string())
                .build();
        Tag8 tag = Tag8.builder()
                .label(RandomString.string())
                .build();
        PostTags postTag = PostTags.builder()
                .post8(post)
                .tag8(tag)
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(tag);
        saveAndWaitForSync(postTag);

        // Update post
        Post8 updatedPost = post.copyOfBuilder()
                .title(RandomString.string())
                .build();
        saveAndWaitForSync(updatedPost);

        // Assert synced
        Post8 remotePost = api.get(Post8.class, post.getId());
        Post8 localPost = dataStore.get(Post8.class, post.getId());
        assertEquals(localPost, remotePost);
        assertEquals(updatedPost.getTitle(), remotePost.getTitle());

        // Assert associated
        assertEquals(tag.getId(), remotePost.getTags().get(0).getTag8().getId());
    }

    @Test
    public void testDeleteModelWithManyToManyRelationship() throws AmplifyException {
        // Create post & tag
        Post8 post = Post8.builder()
                .title(RandomString.string())
                .build();
        Tag8 tag = Tag8.builder()
                .label(RandomString.string())
                .build();
        PostTags postTag = PostTags.builder()
                .post8(post)
                .tag8(tag)
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(tag);
        saveAndWaitForSync(postTag);

        // Delete post
        TestObserver<String> onDelete = subscribeOnDelete(Post8.class, Tag8.class, PostTags.class);
        deleteAndWaitForPublish(post);

        // Assert synced
        onDelete.awaitCount(2);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(post.getId()));
        assertTrue(deletedIds.contains(postTag.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Post8.class, post.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(PostTags.class, postTag.getId()));

        // Assert tag not deleted
        Tag8 remoteTag = api.get(Tag8.class, tag.getId());
        Tag8 localTag = dataStore.get(Tag8.class, tag.getId());
        assertEquals(localTag, remoteTag);
    }

    @Test
    public void testCreateModelWithImplicitBelongsToRelationship() throws AmplifyException {
        // Create project & team
        Project9 projectTemp = Project9.builder()
                .name(RandomString.string())
                .build();
        Team9 team = Team9.builder()
                .name(RandomString.string())
                .project(projectTemp)
                .build();
        Project9 project = projectTemp.copyOfBuilder()
                .project9TeamId(team.getId())
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Assert synced
        Project9 remoteProject = api.get(Project9.class, project.getId());
        Project9 localProject = dataStore.get(Project9.class, project.getId());
        assertEquals(localProject, remoteProject);

        Team9 remoteTeam = api.get(Team9.class, team.getId());
        Team9 localTeam = dataStore.get(Team9.class, team.getId());
        assertEquals(localTeam, remoteTeam);

        // Assert linked
        assertEquals(remoteTeam, remoteProject.getTeam());
        assertEquals(remoteProject, remoteTeam.getProject());
    }

    @Test
    public void testUpdateModelWithImplicitBelongsToRelationship() throws AmplifyException {
        // Create project & team
        Project9 project = Project9.builder()
                .name(RandomString.string())
                .build();
        Team9 team = Team9.builder()
                .name(RandomString.string())
                .project(project)
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Update project
        Project9 updatedProject = project.copyOfBuilder()
                .name(RandomString.string())
                .build();
        Team9 updatedTeam = team.copyOfBuilder()
                .name(RandomString.string())
                .build();
        saveAndWaitForSync(updatedProject);
        saveAndWaitForSync(updatedTeam);

        // Assert synced
        Project9 remoteProject = api.get(Project9.class, project.getId());
        Project9 localProject = dataStore.get(Project9.class, project.getId());
        assertEquals(localProject, remoteProject);
        assertEquals(updatedProject.getName(), remoteProject.getName());

        Team9 remoteTeam = api.get(Team9.class, team.getId());
        Team9 localTeam = dataStore.get(Team9.class, team.getId());
        assertEquals(localTeam, remoteTeam);
        assertEquals(updatedTeam.getName(), remoteTeam.getName());
    }

    @Test
    public void testDeleteModelWithImplicitBelongsToRelationship() throws AmplifyException {
        // Create project & team
        Project9 project = Project9.builder()
                .name(RandomString.string())
                .build();
        Team9 team = Team9.builder()
                .name(RandomString.string())
                .project(project)
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Delete project
        TestObserver<String> onDelete = subscribeOnDelete(Project9.class, Team9.class);
        deleteAndWaitForPublish(project);

        // Assert synced
        onDelete.awaitCount(2);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(project.getId()));
        assertTrue(deletedIds.contains(team.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Project9.class, project.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Team9.class, team.getId()));
    }

    @Test
    public void testCreateModelWithExplicitBelongsToRelationship() throws AmplifyException {
        // Create project & team
        Project10 projectTemp = Project10.builder()
                .name(RandomString.string())
                .build();
        Team10 team = Team10.builder()
                .name(RandomString.string())
                .project(projectTemp)
                .build();
        Project10 project = projectTemp.copyOfBuilder()
                .project10TeamId(team.getId())
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Assert synced
        Project10 remoteProject = api.get(Project10.class, project.getId());
        Project10 localProject = dataStore.get(Project10.class, project.getId());
        assertEquals(localProject, remoteProject);

        Team10 remoteTeam = api.get(Team10.class, team.getId());
        Team10 localTeam = dataStore.get(Team10.class, team.getId());
        assertEquals(localTeam, remoteTeam);

        // Assert linked
        assertEquals(remoteTeam, remoteProject.getTeam());
        assertEquals(remoteProject, remoteTeam.getProject());
    }

    @Test
    public void testUpdateModelWithExplicitBelongsToRelationship() throws AmplifyException {
        // Create project & team
        Project10 project = Project10.builder()
                .name(RandomString.string())
                .build();
        Team10 team = Team10.builder()
                .name(RandomString.string())
                .project(project)
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Update project
        Project10 updatedProject = project.copyOfBuilder()
                .name(RandomString.string())
                .build();
        Team10 updatedTeam = team.copyOfBuilder()
                .name(RandomString.string())
                .build();
        saveAndWaitForSync(updatedProject);
        saveAndWaitForSync(updatedTeam);

        // Assert synced
        Project10 remoteProject = api.get(Project10.class, project.getId());
        Project10 localProject = dataStore.get(Project10.class, project.getId());
        assertEquals(localProject, remoteProject);
        assertEquals(updatedProject.getName(), remoteProject.getName());

        Team10 remoteTeam = api.get(Team10.class, team.getId());
        Team10 localTeam = dataStore.get(Team10.class, team.getId());
        assertEquals(localTeam, remoteTeam);
        assertEquals(updatedTeam.getName(), remoteTeam.getName());
    }

    @Test
    public void testDeleteModelWithExplicitBelongsToRelationship() throws AmplifyException {
        // Create project & team
        Project10 project = Project10.builder()
                .name(RandomString.string())
                .build();
        Team10 team = Team10.builder()
                .name(RandomString.string())
                .project(project)
                .build();
        saveAndWaitForSync(project);
        saveAndWaitForSync(team);

        // Delete project
        TestObserver<String> onDelete = subscribeOnDelete(Project10.class, Team10.class);
        deleteAndWaitForPublish(project);

        // Assert synced
        onDelete.awaitCount(2);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(project.getId()));
        assertTrue(deletedIds.contains(team.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Project10.class, project.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Team10.class, team.getId()));
    }

    @Test
    public void testCreateModelWithExplicitBidirectionalBelongsToRelationship() throws AmplifyException {
        // Create post & comment
        Post11 post = Post11.builder()
                .title(RandomString.string())
                .build();
        Comment11 comment = Comment11.builder()
                .content(RandomString.string())
                .post(post)
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Assert synced
        Post11 remotePost = api.get(Post11.class, post.getId());
        Post11 localPost = dataStore.get(Post11.class, post.getId());
        assertEquals(localPost, remotePost);

        Comment11 remoteComment = api.get(Comment11.class, comment.getId());
        Comment11 localComment = dataStore.get(Comment11.class, comment.getId());
        assertEquals(localComment, remoteComment);

        // Assert linked
        assertEquals(Collections.singletonList(remoteComment), remotePost.getComments());
        assertEquals(remotePost, remoteComment.getPost());
    }

    @Test
    public void testUpdateModelWithExplicitBidirectionalBelongsToRelationship() throws AmplifyException {
        // Create post & comment
        Post11 post = Post11.builder()
                .title(RandomString.string())
                .build();
        Comment11 comment = Comment11.builder()
                .content(RandomString.string())
                .post(post)
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Update post & comment
        Post11 updatedPost = post.copyOfBuilder()
                .title(RandomString.string())
                .build();
        Comment11 updatedComment = comment.copyOfBuilder()
                .content(RandomString.string())
                .build();
        saveAndWaitForSync(updatedPost);
        saveAndWaitForSync(updatedComment);

        // Assert synced
        Post11 remotePost = api.get(Post11.class, post.getId());
        Post11 localPost = dataStore.get(Post11.class, post.getId());
        assertEquals(localPost, remotePost);
        assertEquals(updatedPost.getTitle(), remotePost.getTitle());

        Comment11 remoteComment = api.get(Comment11.class, comment.getId());
        Comment11 localComment = dataStore.get(Comment11.class, comment.getId());
        assertEquals(localComment, remoteComment);
        assertEquals(updatedComment.getContent(), remoteComment.getContent());
    }

    @Test
    public void testDeleteModelWithExplicitBidirectionalBelongsToRelationship() throws AmplifyException {
        // Create post & comment
        Post11 post = Post11.builder()
                .title(RandomString.string())
                .build();
        Comment11 comment = Comment11.builder()
                .content(RandomString.string())
                .post(post)
                .build();
        saveAndWaitForSync(post);
        saveAndWaitForSync(comment);

        // Delete post & comment
        TestObserver<String> onDelete = subscribeOnDelete(Post11.class, Comment11.class);
        deleteAndWaitForPublish(post);

        // Assert synced
        onDelete.awaitCount(2);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(post.getId()));
        assertTrue(deletedIds.contains(comment.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Post11.class, post.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Comment11.class, comment.getId()));
    }

    @Test
    public void testCreateModelWithCustomizedBelongsToRelationship() throws AmplifyException {
        // Create blog & post
        CookingBlog12 blog = CookingBlog12.builder()
                .name(RandomString.string())
                .build();
        RecipePost12 post = RecipePost12.builder()
                .title(RandomString.string())
                .blog(blog)
                .build();
        saveAndWaitForSync(blog);
        saveAndWaitForSync(post);

        // Assert synced
        CookingBlog12 remoteBlog = api.get(CookingBlog12.class, blog.getId());
        CookingBlog12 localBlog = dataStore.get(CookingBlog12.class, blog.getId());
        assertEquals(localBlog, remoteBlog);

        RecipePost12 remotePost = api.get(RecipePost12.class, post.getId());
        RecipePost12 localPost = dataStore.get(RecipePost12.class, post.getId());
        assertEquals(localPost, remotePost);

        // Assert linked
        assertEquals(Collections.singletonList(remotePost), remoteBlog.getPosts());
        assertEquals(remoteBlog, remotePost.getBlog());
    }

    @Test
    public void testUpdateModelWithCustomizedBelongsToRelationship() throws AmplifyException {
        // Create blog & post
        CookingBlog12 blog = CookingBlog12.builder()
                .name(RandomString.string())
                .build();
        RecipePost12 post = RecipePost12.builder()
                .title(RandomString.string())
                .blog(blog)
                .build();
        saveAndWaitForSync(blog);
        saveAndWaitForSync(post);

        // Update blog & post
        CookingBlog12 updatedBlog = blog.copyOfBuilder()
                .name(RandomString.string())
                .build();
        RecipePost12 updatedPost = post.copyOfBuilder()
                .title(RandomString.string())
                .build();
        saveAndWaitForSync(updatedBlog);
        saveAndWaitForSync(updatedPost);

        // Assert synced
        CookingBlog12 remoteBlog = api.get(CookingBlog12.class, blog.getId());
        CookingBlog12 localBlog = dataStore.get(CookingBlog12.class, blog.getId());
        assertEquals(localBlog, remoteBlog);
        assertEquals(updatedBlog.getName(), remoteBlog.getName());

        RecipePost12 remotePost = api.get(RecipePost12.class, post.getId());
        RecipePost12 localPost = dataStore.get(RecipePost12.class, post.getId());
        assertEquals(localPost, remotePost);
        assertEquals(updatedPost.getTitle(), remotePost.getTitle());
    }

    @Test
    public void testDeleteModelWithCustomizedBelongsToRelationship() throws AmplifyException {
        // Create blog & post
        CookingBlog12 blog = CookingBlog12.builder()
                .name(RandomString.string())
                .build();
        RecipePost12 post = RecipePost12.builder()
                .title(RandomString.string())
                .blog(blog)
                .build();
        saveAndWaitForSync(blog);
        saveAndWaitForSync(post);

        // Delete blog
        TestObserver<String> onDelete = subscribeOnDelete(CookingBlog12.class, RecipePost12.class);
        deleteAndWaitForPublish(blog);

        // Assert synced
        onDelete.awaitCount(2);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(blog.getId()));
        assertTrue(deletedIds.contains(post.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(CookingBlog12.class, blog.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(RecipePost12.class, post.getId()));
    }

    @Test
    public void testCreateModelWithMultipleBelongsToRelationship() throws AmplifyException {
        // Create meeting & attendee & registration
        Meeting13 meeting = Meeting13.builder()
                .title(RandomString.string())
                .build();
        Attendee13 attendee = Attendee13.builder()
                .build();
        Registration13 registration = Registration13.builder()
                .meeting(meeting)
                .attendee(attendee)
                .build();
        saveAndWaitForSync(meeting);
        saveAndWaitForSync(attendee);
        saveAndWaitForSync(registration);

        // Assert synced
        Meeting13 remoteMeeting = api.get(Meeting13.class, meeting.getId());
        Meeting13 localMeeting = dataStore.get(Meeting13.class, meeting.getId());
        assertEquals(localMeeting, remoteMeeting);

        Attendee13 remoteAttendee = api.get(Attendee13.class, attendee.getId());
        Attendee13 localAttendee = dataStore.get(Attendee13.class, attendee.getId());
        assertEquals(localAttendee, remoteAttendee);

        Registration13 remoteRegistration = api.get(Registration13.class, registration.getId());
        Registration13 localRegistration = dataStore.get(Registration13.class, registration.getId());
        assertEquals(localRegistration, remoteRegistration);

        // Assert linked
        assertEquals(Collections.singletonList(remoteRegistration), remoteMeeting.getAttendees());
        assertEquals(Collections.singletonList(remoteRegistration), remoteAttendee.getMeetings());
        assertEquals(remoteMeeting, remoteRegistration.getMeeting());
        assertEquals(remoteAttendee, remoteRegistration.getAttendee());
    }

    @Test
    public void testUpdateModelWithMultipleBelongsToRelationship() throws AmplifyException {
        // Create meeting & attendee & registration
        Meeting13 meeting = Meeting13.builder()
                .title(RandomString.string())
                .build();
        Attendee13 attendee = Attendee13.builder()
                .build();
        Registration13 registration = Registration13.builder()
                .meeting(meeting)
                .attendee(attendee)
                .build();
        saveAndWaitForSync(meeting);
        saveAndWaitForSync(attendee);
        saveAndWaitForSync(registration);

        // Update meeting
        Meeting13 updatedMeeting = meeting.copyOfBuilder()
                .title(RandomString.string())
                .build();
        saveAndWaitForSync(updatedMeeting);

        // Assert synced
        Meeting13 remoteMeeting = api.get(Meeting13.class, meeting.getId());
        Meeting13 localMeeting = dataStore.get(Meeting13.class, meeting.getId());
        assertEquals(localMeeting, remoteMeeting);
        assertEquals(updatedMeeting.getTitle(), remoteMeeting.getTitle());

        // Assert associated
        assertEquals(attendee.getId(), remoteMeeting.getAttendees().get(0).getAttendee().getId());
    }

    @Test
    public void testDeleteModelWithMultipleBelongsToRelationship() throws AmplifyException {
        // Create meeting & attendee & registration
        Meeting13 meeting = Meeting13.builder()
                .title(RandomString.string())
                .build();
        Attendee13 attendee = Attendee13.builder()
                .build();
        Registration13 registration = Registration13.builder()
                .meeting(meeting)
                .attendee(attendee)
                .build();
        saveAndWaitForSync(meeting);
        saveAndWaitForSync(attendee);
        saveAndWaitForSync(registration);

        // Delete meeting
        TestObserver<String> onDelete = subscribeOnDelete(Meeting13.class, Attendee13.class, Registration13.class);
        deleteAndWaitForPublish(meeting);

        // Assert synced
        onDelete.awaitCount(2);
        List<String> deletedIds = onDelete.values();
        assertTrue(deletedIds.contains(meeting.getId()));
        assertTrue(deletedIds.contains(registration.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Meeting13.class, meeting.getId()));
        assertThrows(NoSuchElementException.class, () -> dataStore.get(Registration13.class, registration.getId()));

        // Assert attendee not deleted
        Attendee13 remoteAttendee = api.get(Attendee13.class, attendee.getId());
        Attendee13 localAttendee = dataStore.get(Attendee13.class, attendee.getId());
        assertEquals(localAttendee, remoteAttendee);
    }

    private <T extends Model> void saveAndWaitForSync(T model) throws AmplifyException {
        HubAccumulator published = HubAccumulator.create(
                HubChannel.DATASTORE,
                publicationOf(model.getModelName(), model.getId()),
                1
        ).start();
        HubAccumulator received = HubAccumulator.create(
                HubChannel.DATASTORE,
                receiptOf(model.getId()),
                1
        ).start();
        dataStore.save(model);
        published.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        received.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private <T extends Model> void deleteAndWaitForPublish(T model) throws AmplifyException {
        HubAccumulator accumulator = HubAccumulator.create(
                HubChannel.DATASTORE,
                publicationOf(model.getModelName(), model.getId()),
                1
        ).start();
        dataStore.delete(model);
        accumulator.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @SafeVarargs
    private final TestObserver<String> subscribeOnDelete(Class<? extends Model>... clazzes) {
        List<Observable<String>> observables = new ArrayList<>();
        for (Class<? extends Model> clazz : clazzes) {
            ModelSchema schema = SchemaRegistry.instance().getModelSchemaForModelClass(clazz);
            observables.add(appSync.onDelete(schema)
                    .filter(response -> Boolean.TRUE.equals(response.getData().getSyncMetadata().isDeleted()))
                    .map(response -> response.getData().getModel().getId()));
        }
        return Observable.merge(observables).test();
    }
}
