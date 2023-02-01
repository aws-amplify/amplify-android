/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.DataStoreConfiguration.ConfigKey;
import com.amplifyframework.datastore.DataStoreConflictHandler.AlwaysApplyRemoteHandler;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link DataStoreConfiguration}.
 */
@RunWith(RobolectricTestRunner.class)
public final class DataStoreConfigurationTest {
    /**
     * Validates expectations on what are "default" values for the {@link DataStoreConfiguration},
     * when the user does not specify any others, neither by file nor by passing values into it
     * using {@link DataStoreConfiguration.Builder}.
     * @throws DataStoreException Possible while attempting to create a default configuration
     */
    @Test
    public void testDefaultConfiguration() throws DataStoreException {
        DataStoreConfiguration dataStoreConfiguration = DataStoreConfiguration.defaults();
        assertEquals(TimeUnit.MINUTES.toMillis(DataStoreConfiguration.DEFAULT_SYNC_INTERVAL_MINUTES),
            dataStoreConfiguration.getSyncIntervalMs().longValue());
        assertEquals(DataStoreConfiguration.DEFAULT_SYNC_MAX_RECORDS,
            dataStoreConfiguration.getSyncMaxRecords().intValue());
        assertEquals(DataStoreConfiguration.DEFAULT_SYNC_PAGE_SIZE,
            dataStoreConfiguration.getSyncPageSize().intValue());

        assertTrue(dataStoreConfiguration.getConflictHandler() instanceof AlwaysApplyRemoteHandler);
        assertTrue(dataStoreConfiguration.getErrorHandler() instanceof DefaultDataStoreErrorHandler);
        assertEquals(Collections.emptyMap(), dataStoreConfiguration.getSyncExpressions());
        assertFalse(dataStoreConfiguration.getDoSyncRetry());
    }

    /**
     * When a user supplies some configuration values via the builder, these values should
     * override any that may have been present in the configuration file.
     * @throws JSONException While arranging config file JSON
     * @throws DataStoreException While building a configuration instance
     */
    @Test
    public void testDefaultOverriddenFromConfiguration() throws JSONException, DataStoreException {
        long expectedSyncIntervalMinutes = 6L;
        Long expectedSyncIntervalMs = TimeUnit.MINUTES.toMillis(expectedSyncIntervalMinutes);
        Integer expectedSyncMaxRecords = 3;
        JSONObject jsonConfigFromFile = new JSONObject()
            .put(ConfigKey.SYNC_INTERVAL_IN_MINUTES.toString(), expectedSyncIntervalMinutes)
            .put(ConfigKey.SYNC_MAX_RECORDS.toString(), expectedSyncMaxRecords);
        DataStoreConfiguration dataStoreConfiguration = DataStoreConfiguration.builder(jsonConfigFromFile).build();
        assertEquals(expectedSyncIntervalMs, dataStoreConfiguration.getSyncIntervalMs());
        assertEquals(expectedSyncMaxRecords, dataStoreConfiguration.getSyncMaxRecords());
        assertEquals(DataStoreConfiguration.DEFAULT_SYNC_PAGE_SIZE,
            dataStoreConfiguration.getSyncPageSize().longValue());

        assertTrue(dataStoreConfiguration.getConflictHandler() instanceof AlwaysApplyRemoteHandler);
        assertTrue(dataStoreConfiguration.getErrorHandler() instanceof DefaultDataStoreErrorHandler);
        assertEquals(Collections.emptyMap(), dataStoreConfiguration.getSyncExpressions());
    }

    /**
     * When building a configuration from both a config file and a configuration object,
     * default values should be overridden, and the provided ones shall be used, instead.
     * @throws JSONException While arranging config file JSON
     * @throws AmplifyException While building DataStoreConfiguration instances via build(), or when deriving a
     * {@link ModelSchema} from a {@link Model} class.
     */
    @Test
    public void testDefaultOverriddenFromConfigurationAndObject()
            throws JSONException, AmplifyException {
        long expectedSyncIntervalMinutes = 6L;
        Long expectedSyncIntervalMs = TimeUnit.MINUTES.toMillis(expectedSyncIntervalMinutes);
        Integer expectedSyncMaxRecords = 3;
        DummyConflictHandler dummyConflictHandler = new DummyConflictHandler();
        DataStoreErrorHandler errorHandler = DefaultDataStoreErrorHandler.instance();

        DataStoreSyncExpression ownerSyncExpression = () -> BlogOwner.ID.beginsWith(RandomString.string());
        DataStoreSyncExpression postSyncExpression = () -> Post.ID.beginsWith(RandomString.string());

        @SuppressWarnings("deprecation")
        DataStoreConfiguration configObject = DataStoreConfiguration
            .builder()
            .syncMaxRecords(expectedSyncMaxRecords)
            .conflictHandler(dummyConflictHandler)
            .errorHandler(errorHandler)
            .syncExpression(BlogOwner.class, ownerSyncExpression)
            .syncExpression("Post", postSyncExpression)
                .doSyncRetry(true)
            .build();

        JSONObject jsonConfigFromFile = new JSONObject()
            .put(ConfigKey.SYNC_INTERVAL_IN_MINUTES.toString(), expectedSyncIntervalMinutes);
        DataStoreConfiguration dataStoreConfiguration = DataStoreConfiguration
            .builder(jsonConfigFromFile, configObject)
            .build();

        assertEquals(expectedSyncIntervalMs, dataStoreConfiguration.getSyncIntervalMs());
        assertEquals(expectedSyncMaxRecords, dataStoreConfiguration.getSyncMaxRecords());
        assertEquals(DataStoreConfiguration.DEFAULT_SYNC_PAGE_SIZE,
            dataStoreConfiguration.getSyncPageSize().longValue());
        assertTrue(dataStoreConfiguration.getDoSyncRetry());

        assertEquals(dummyConflictHandler, dataStoreConfiguration.getConflictHandler());
        assertEquals(errorHandler, dataStoreConfiguration.getErrorHandler());

        Map<String, DataStoreSyncExpression> expectedSyncExpressions = new HashMap<>();
        expectedSyncExpressions.put(BlogOwner.class.getSimpleName(), ownerSyncExpression);
        expectedSyncExpressions.put(Post.class.getSimpleName(), postSyncExpression);
        assertEquals(expectedSyncExpressions, dataStoreConfiguration.getSyncExpressions());
    }

    /**
     * If the config file contains an invalid key, the parsing code should through a
     * {@link DataStoreException}, to warn the user.
     * @throws DataStoreException On failure to build a config object
     * @throws JSONException While arranging a JSONObject in our test code
     */
    @Test(expected = DataStoreException.class)
    public void testInvalidKeyThrowsException() throws DataStoreException, JSONException {
        JSONObject jsonConfigFromFile = new JSONObject()
            .put("foo", "bar");
        DataStoreConfiguration
            .builder(jsonConfigFromFile).build();
    }

    private static final class DummyConflictHandler implements DataStoreConflictHandler {
        @Override
        public void onConflictDetected(
                @NonNull ConflictData<? extends Model> conflictData,
                @NonNull Consumer<ConflictResolutionDecision<? extends Model>> onDecision) {
            onDecision.accept(ConflictResolutionDecision.retry(null));
        }
    }
}
