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

import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.test.R;
import com.amplifyframework.testmodels.AmplifyCliGeneratedModelProvider;
import com.amplifyframework.testmodels.Car;
import com.amplifyframework.testmodels.MaritalStatus;
import com.amplifyframework.testmodels.Person;
import com.amplifyframework.testutils.LatchedResultListener;
import com.amplifyframework.testutils.Sleep;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Tests the functions of {@link com.amplifyframework.datastore.AWSDataStorePlugin}.
 * This test expects a backend API that has support for the {@link Person} and {@link Car}
 * models.
 */
public final class AWSDataStorePluginInstrumentedTest {
    private static final String DATABASE_NAME = "AmplifyDatastore.db";
    private static final long DATA_STORE_OP_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);
    private static final long NETWORK_OP_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);
    private static Context context;
    private static AWSDataStorePlugin awsDataStorePlugin;
    private static String apiName;

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
     */
    @BeforeClass
    public static void configureAmplify() {
        context = ApplicationProvider.getApplicationContext();

        ModelProvider modelProvider = AmplifyCliGeneratedModelProvider.singletonInstance();
        awsDataStorePlugin = AWSDataStorePlugin.singleton(modelProvider);
        Amplify.addPlugin(awsDataStorePlugin);

        // We need to use an API plugin, so that we can validate remote sync.
        Amplify.addPlugin(new AWSApiPlugin());

        AmplifyConfiguration amplifyConfiguration = new AmplifyConfiguration();
        amplifyConfiguration.populateFromConfigFile(context, R.raw.amplifyconfiguration);
        Amplify.configure(amplifyConfiguration, context);

        apiName = firstApiIn(amplifyConfiguration);
    }

    private static String firstApiIn(AmplifyConfiguration amplifyConfiguration) {
        return amplifyConfiguration.forCategoryType(CategoryType.API)
            .getPluginConfig("AWSAPIPlugin")
            .keys()
            .next();
    }

    /**
     * Save a person via DataStore, wait a bit, check API to see if the person is there, remotely.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void personSavedIntoDataStoreIsThenQueriableInRemoteAppSyncApi() {
        // Save Charley Crocket the the DataStore.
        Person localCharley = Person.builder()
            .firstName("Charley")
            .lastName("Crocket")
            .build();
        saveLocal(localCharley);

        // Wait a bit. TODO: this is lame; how to tell deterministically when sync engine has sync'd?
        Sleep.milliseconds(NETWORK_OP_TIMEOUT_MS + DATA_STORE_OP_TIMEOUT_MS);

        // Try to get Charley from the backend.
        Person remoteCharley = getRemote(Person.class, localCharley.getId());

        // A Charley is a Charley is a Charley, right?
        assertEquals(localCharley.getId(), remoteCharley.getId());
        assertEquals(localCharley.getFirstName(), remoteCharley.getFirstName());
        assertEquals(localCharley.getLastName(), remoteCharley.getLastName());
    }

    /**
     * The sync engine should receive mutations for its managed models, through its
     * subscriptions. When we change a model remotely, the sync engine should respond
     * by processing the subscription event and saving the model locally.
     */
    // "deprecation" -> Date(year, month, day) is a readable API! Disagree-with-IDE, and git-commit.
    // "checkstyle:MagicNumber" -> Dates and times of historical events are kinda magic.
    @SuppressWarnings({"deprecation", "checkstyle:MagicNumber"})
    @Test
    public void personCreatedAndUpdatedRemotelyIsFoundLocally() {
        // Create a record for Hank Williams, before he gets married to Audrey
        Person remoteHank = Person.builder()
            .firstName("Hank")
            .lastName("Williams")
            .relationship(MaritalStatus.single)
            .age(20)
            .dob(new Date(1923, 9, 17))
            .build();
        createRemote(remoteHank);

        // Update the record when Hank marries Audrey
        updateRemote(remoteHank.newBuilder()
            .relationship(MaritalStatus.married)
            .age(21)
            .build());

        // Wait for sync. TODO: super lame. Get a deterministic event-driven hook for this.
        Sleep.milliseconds(NETWORK_OP_TIMEOUT_MS + DATA_STORE_OP_TIMEOUT_MS);

        // Hank should be in the local DataStore, and we should see that he's married, now.
        Person localHank = getLocal(Person.class, remoteHank.getId());
        assertEquals(MaritalStatus.married, localHank.getRelationship());
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     */
    @AfterClass
    public static void tearDown() {
        awsDataStorePlugin.terminate();
        context.deleteDatabase(DATABASE_NAME);
    }

    private <T extends Model> void saveLocal(T item) {
        LatchedResultListener<DataStoreItemChange<T>> saveListener =
            LatchedResultListener.waitFor(DATA_STORE_OP_TIMEOUT_MS);
        Amplify.DataStore.save(item, saveListener);
        saveListener.awaitTerminalEvent().assertResult().assertNoError();
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
        LatchedResultListener<Iterator<T>> queryResultsListener =
            LatchedResultListener.waitFor(DATA_STORE_OP_TIMEOUT_MS);
        Amplify.DataStore.query(clazz, queryResultsListener);

        final Iterator<T> iterator = queryResultsListener
            .awaitTerminalEvent()
            .assertNoError()
            .assertResult()
            .getResult();

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
        LatchedResultListener<GraphQLResponse<T>> queryListener =
            LatchedResultListener.waitFor(NETWORK_OP_TIMEOUT_MS);
        Amplify.API.query(apiName, clazz, itemId, queryListener);
        return queryListener.awaitTerminalEvent().assertNoError().assertResult().getResult().getData();
    }

    private <T extends Model> void createRemote(T item) {
        LatchedResultListener<GraphQLResponse<T>> createListener =
            LatchedResultListener.waitFor(NETWORK_OP_TIMEOUT_MS);
        Amplify.API.mutate(apiName, item, MutationType.CREATE, createListener);
        createListener.awaitTerminalEvent().assertNoError().assertResult();
    }

    private <T extends Model> void updateRemote(T item) {
        LatchedResultListener<GraphQLResponse<T>> updateListener =
            LatchedResultListener.waitFor(NETWORK_OP_TIMEOUT_MS);
        Amplify.API.mutate(apiName, item, MutationType.UPDATE, updateListener);
        updateListener.awaitTerminalEvent().assertNoError().assertResult();
    }
}
