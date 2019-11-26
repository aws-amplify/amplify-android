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
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.test.R;
import com.amplifyframework.testmodels.AmplifyCliGeneratedModelProvider;
import com.amplifyframework.testmodels.Person;
import com.amplifyframework.testutils.LatchedResultListener;
import com.amplifyframework.testutils.Sleep;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Tests the functions of {@link com.amplifyframework.datastore.AWSDataStorePlugin}.
 */
public final class AWSDataStorePluginInstrumentedTest {
    private static final String DATABASE_NAME = "AmplifyDatastore.db";
    private static final long OPERATION_TIMEOUT_MS = 1000;
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
        final String expectedId = UUID.randomUUID().toString();
        Person charleyCrockett = Person.builder()
            .firstName("Charley")
            .lastName("Crockett")
            .id(expectedId)
            .build();
        LatchedResultListener<DataStoreItemChange<Person>> charleySavedListener =
            LatchedResultListener.waitFor(OPERATION_TIMEOUT_MS);
        Amplify.DataStore.save(charleyCrockett, charleySavedListener);
        charleySavedListener.awaitTerminalEvent().assertResult().assertNoError();

        // Wait a bit. TODO: this is lame; how to tell deterministically when sync engine has sync'd?
        Sleep.milliseconds(1_000);

        // Check the API category to see if there is an entry for Charley, remotely, now
        LatchedResultListener<GraphQLResponse<Person>> queryListener =
            LatchedResultListener.waitFor(OPERATION_TIMEOUT_MS);
        Amplify.API.query(
            apiName,
            Person.class,
            expectedId,
            queryListener
        );
        queryListener.awaitTerminalEvent().assertNoError().assertResult();

        // Validate the response
        final Person charleyViaApi = queryListener.getResult().getData();
        assertEquals(expectedId, charleyViaApi.getId());
    }

    /**
     * Drop all tables and database, terminate and delete the database.
     */
    @After
    public void tearDown() {
        awsDataStorePlugin.terminate();
        context.deleteDatabase(DATABASE_NAME);
    }
}
