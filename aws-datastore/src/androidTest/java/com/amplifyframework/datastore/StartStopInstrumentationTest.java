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
import androidx.annotation.RawRes;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests running DataStore.stop() and then calling DataStore.start() from within the stop() callback.
 * This is the recommended method for resetting sync expressions in the Amplify documentation.
 */
public final class StartStopInstrumentationTest {
    private static final int TIMEOUT_SECONDS = 60;
    private static SynchronousDataStore dataStore;
    private static DataStoreCategory dataStoreCategory;

    /**
     * Set up Datastore plugin for testing.
     * @throws AmplifyException On failure to read config, setup API or DataStore categories
     */
    @BeforeClass
    public static void setup() throws AmplifyException {
        Amplify.addPlugin(new AndroidLoggingPlugin(LogLevel.VERBOSE));

        StrictMode.enable();
        Context context = ApplicationProvider.getApplicationContext();
        @RawRes int configResourceId = Resources.getRawResourceId(context, "amplifyconfigurationupdated");

        ApiCategory apiCategory = new ApiCategory();

        dataStoreCategory = DataStoreCategoryConfigurator.begin()
                .api(apiCategory)
                .clearDatabase(true)
                .context(context)
                .modelProvider(AmplifyModelProvider.getInstance())
                .resourceId(configResourceId)
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .finish();
        dataStore = SynchronousDataStore.delegatingTo(dataStoreCategory);
    }

    /**
     * Clear DataStore after testing to prevent conflict with any other test.
     */
    @AfterClass
    public static void teardown() {
        if (dataStore != null) {
            try {
                dataStore.clear();
            } catch (Exception error) {
                // ok to ignore since problem encountered during tear down of the test.
            }
        }
    }

    /**
     * Tests running DataStore.stop() and then calling DataStore.start() from within the stop() callback.
     * This is the recommended method for resetting sync expressions in the Amplify documentation.
     * @throws InterruptedException when interruption occurs during await().
     */
    @Test
    public void testStartStop() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        dataStoreCategory.stop(() -> {
                latch.countDown();
                dataStoreCategory.start(
                    latch::countDown,
                    (error) -> {
                        throw new RuntimeException(error);
                    }
                );
            }, (error) -> {
                throw new RuntimeException(error);
            }
        );
        latch.await(10, TimeUnit.SECONDS);
    }
}
