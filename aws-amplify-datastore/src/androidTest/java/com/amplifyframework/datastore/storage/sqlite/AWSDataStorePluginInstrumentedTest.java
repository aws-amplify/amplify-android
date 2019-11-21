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

package com.amplifyframework.datastore.storage.sqlite;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.AWSDataStorePlugin;
import com.amplifyframework.testmodels.AmplifyCliGeneratedModelProvider;
import com.amplifyframework.testutils.LatchedResultListener;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Tests the functions of {@link com.amplifyframework.datastore.AWSDataStorePlugin}.
 */
public final class AWSDataStorePluginInstrumentedTest {

    private static final long DATASTORE_OPERATION_TIMEOUT_IN_MILLISECONDS = 1000;

    private static Context context;

    /**
     * Setup the Android application context.
     */
    @BeforeClass
    public static void configureAmplify() {
        context = ApplicationProvider.getApplicationContext();
        AmplifyConfiguration amplifyConfiguration = new AmplifyConfiguration();
        amplifyConfiguration.populateFromConfigFile(context);
        Amplify.addPlugin(new AWSDataStorePlugin());
        Amplify.configure(amplifyConfiguration, context);
    }

    /**
     * Drop all tables and database, close and delete the database.
     */
    @After
    public void tearDown() {
        deleteDatabase();
    }

    /**
     * Test adding, configuring and setting up AWSDataStorePlugin.
     * @throws InterruptedException when setUp times out.
     */
    @Test
    public void testSetUp() throws InterruptedException {
        final LatchedResultListener<List<ModelSchema>> schemaListener =
            new LatchedResultListener<>(DATASTORE_OPERATION_TIMEOUT_IN_MILLISECONDS);

        ModelProvider modelProvider = AmplifyCliGeneratedModelProvider.singletonInstance();

        Amplify.DataStore.setUp(
            context,
            modelProvider,
            schemaListener
        );

        // Await result, and obtain the received list of schema
        final List<ModelSchema> schemaList =
            schemaListener.awaitTerminalEvent().assertNoError().getResult();

        // Prepare a set of the actual model schema names, as string
        Set<String> expectedModelClassNames = new HashSet<>();
        for (ModelSchema actualSchema : schemaList) {
            expectedModelClassNames.add(actualSchema.getName());
        }

        // Ensure that we got a schema for each of the models that we requested.
        for (Class<? extends Model> requestedModel : modelProvider.models()) {
            assertTrue(expectedModelClassNames.contains(requestedModel.getSimpleName()));
        }
    }

    private void deleteDatabase() {
        context.deleteDatabase(SQLiteStorageAdapter.DATABASE_NAME);
    }
}
