/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.content.Context;
import android.os.StrictMode;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.testmodels.commentsblog.Blog;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.EmptyConsumer;
import com.amplifyframework.testutils.LatchedConsumer;
import com.amplifyframework.testutils.LatchedResponseConsumer;
import com.amplifyframework.testutils.Sleep;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Tests the functions of {@link com.amplifyframework.datastore.AWSDataStorePlugin}.
 * This test expects a backend API that has support for the {@link Blog} family of models,
 * which were defined by the schema in:
 * testmodels/src/main/java/com/amplifyframework/testmodels/commentsblog/schema.graphql.
 */
public final class AWSDataStorePluginInstrumentedTest {
    private static final String DATABASE_NAME = "AmplifyDatastore.db";
    private static final long DATA_STORE_OP_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);
    private static final long NETWORK_OP_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);
    private static Context context;
    private static AWSDataStorePlugin awsDataStorePlugin;

    /**
     * Enable strict mode for catching SQLite leaks.
     */
    @BeforeClass
    public static void enableStrictMode() {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .penaltyDeath()
            .build());
    }

    /**
     * Setup the Android application context.
     * @throws AmplifyException from Amplify configuration
     */
    @BeforeClass
    public static void configureAmplify() throws AmplifyException {
        final TestConfiguration testConfig = TestConfiguration.configureIfNotConfigured();
        awsDataStorePlugin = testConfig.plugin();
        context = ApplicationProvider.getApplicationContext();
    }

    /**
     * Save a BlogOwner via DataStore, wait a bit, check API to see if the BlogOwner is there, remotely.
     */
    @Test
    public void blogOwnerSavedIntoDataStoreIsThenQueriableInRemoteAppSyncApi() {
        // Save Charley Crockett, a guy who has a blog, into the DataStore.
        BlogOwner localCharley = BlogOwner.builder()
            .name("Charley Crockett")
            .build();
        saveLocal(localCharley);

        // Wait a bit. TODO: this is lame; how to tell deterministically when sync engine has sync'd?
        Sleep.milliseconds(NETWORK_OP_TIMEOUT_MS + DATA_STORE_OP_TIMEOUT_MS);

        // Try to get Charley from the backend.
        BlogOwner remoteCharley = getRemote(BlogOwner.class, localCharley.getId());

        // A Charley is a Charley is a Charley, right?
        assertEquals(localCharley.getId(), remoteCharley.getId());
        assertEquals(localCharley.getName(), remoteCharley.getName());
    }

    /**
     * The sync engine should receive mutations for its managed models, through its
     * subscriptions. When we change a model remotely, the sync engine should respond
     * by processing the subscription event and saving the model locally.
     */
    @Ignore(
        "This test is broken, until support for _version is added. " +
        "The local version will be the original created version, not the " +
        "updated version, since the client is not passing _version right nowl."
    )
    @Test
    public void blogOwnerCreatedAndUpdatedRemotelyIsFoundLocally() {
        // Create a record for a blog owner, with a misspelling in the last name
        BlogOwner remoteOwner = BlogOwner.builder()
            .name("Jameson Willlllliams")
            .build();
        createRemote(remoteOwner);

        // Update the record to fix the last name
        updateRemote(remoteOwner.copyOfBuilder()
            // This uses the same record ID
            .name("Jameson Williams")
            .build());

        // Wait for sync. TODO: super lame. Get a deterministic event-driven hook for this.
        Sleep.milliseconds(NETWORK_OP_TIMEOUT_MS + DATA_STORE_OP_TIMEOUT_MS);

        // Jameson should be in the local DataStore, and last name should be updated.
        BlogOwner localOwner = getLocal(BlogOwner.class, remoteOwner.getId());
        assertEquals("Jameson Williams", localOwner.getName());
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     * @throws DataStoreException from terminate if anything goes wrong
     */
    @AfterClass
    public static void tearDown() throws DataStoreException {
        awsDataStorePlugin.terminate();
        context.deleteDatabase(DATABASE_NAME);
    }

    private <T extends Model> void saveLocal(T item) {
        LatchedConsumer<DataStoreItemChange<T>> saveConsumer =
            LatchedConsumer.instance(DATA_STORE_OP_TIMEOUT_MS);
        ResultListener<DataStoreItemChange<T>> resultListener =
            ResultListener.instance(saveConsumer, EmptyConsumer.of(Throwable.class));
        Amplify.DataStore.save(item, resultListener);
        saveConsumer.awaitValue();
    }

    /**
     * Note: at the time this method was written, there was not version of DataStore()
     * implemented yet which supported predicates. When there is, use them, instead of querying
     * all and then filtering the results after the iterator.
     * @param clazz Class of item being accessed
     * @param itemId Unique ID of the item being accessed
     * @param <T> The type of item being accessed
     * @return An item with the provided class and ID, if present in DataStore
     * @throws NoSuchElementException If there is no matching item in the DataStore
     */
    private <T extends Model> T getLocal(
            @SuppressWarnings("SameParameterValue") Class<T> clazz, String itemId) {
        LatchedConsumer<Iterator<T>> queryConsumer =
            LatchedConsumer.instance(DATA_STORE_OP_TIMEOUT_MS);
        ResultListener<Iterator<T>> resultListener =
            ResultListener.instance(queryConsumer, EmptyConsumer.of(Throwable.class));
        Amplify.DataStore.query(clazz, resultListener);

        final Iterator<T> iterator = queryConsumer.awaitValue();
        while (iterator.hasNext()) {
            T value = iterator.next();
            if (value.getId().equals(itemId)) {
                return value;
            }
        }

        throw new NoSuchElementException("No item in DataStore with class = " + clazz + " and id = " + itemId);
    }

    private <T extends Model> T getRemote(
            @SuppressWarnings("SameParameterValue") Class<T> clazz, String itemId) {
        LatchedResponseConsumer<T> queryConsumer =
            LatchedResponseConsumer.instance(NETWORK_OP_TIMEOUT_MS);
        ResultListener<GraphQLResponse<T>> responseListener =
            ResultListener.instance(queryConsumer, EmptyConsumer.of(Throwable.class));
        Amplify.API.query(clazz, itemId, responseListener);
        return queryConsumer.awaitResponseData();
    }

    private <T extends Model> void createRemote(T item) {
        LatchedResponseConsumer<T> createConsumer =
            LatchedResponseConsumer.instance(NETWORK_OP_TIMEOUT_MS);
        ResultListener<GraphQLResponse<T>> responseListener =
            ResultListener.instance(createConsumer, EmptyConsumer.of(Throwable.class));
        Amplify.API.mutate(item, MutationType.CREATE, responseListener);
        createConsumer.awaitResponseData();
    }

    private <T extends Model> void updateRemote(T item) {
        LatchedResponseConsumer<T> updateConsumer =
            LatchedResponseConsumer.instance(NETWORK_OP_TIMEOUT_MS);
        ResultListener<GraphQLResponse<T>> responseListener =
            ResultListener.instance(updateConsumer, EmptyConsumer.of(Throwable.class));
        Amplify.API.mutate(item, MutationType.UPDATE, responseListener);
        updateConsumer.awaitResponseData();
    }
}
