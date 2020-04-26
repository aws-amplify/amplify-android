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

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public final class DataStoreConfigurationTest {

    @Test
    public void testDefaultConfiguration() {
        DataStoreConfiguration dataStoreConfiguration = DataStoreConfiguration.defaults();
        assertEquals(TimeUnit.MINUTES.toMillis(DataStoreConfiguration.DEFAULT_SYNC_INTERVAL_MINUTES),
            dataStoreConfiguration.getSyncIntervalMs());
        assertEquals(DataStoreConfiguration.DEFAULT_SYNC_MAX_RECORDS,
            dataStoreConfiguration.getSyncMaxRecords());
        assertEquals(DataStoreConfiguration.DEFAULT_SYNC_PAGE_SIZE,
            dataStoreConfiguration.getSyncPageSize());

        assertTrue(dataStoreConfiguration.getDataStoreConflictHandler() instanceof ApplyRemoteConflictHandler);
        assertTrue(dataStoreConfiguration.getDataStoreErrorHandler() instanceof DefaultDataStoreErrorHandler);
    }

    @Test
    public void testDefaultOverridenFromConfiguration() throws JSONException, DataStoreException {
        long expectedSyncIntervalMinutes = 6;
        long expectedSyncIntervalMs = TimeUnit.MINUTES.toMillis(expectedSyncIntervalMinutes);
        int expectedSyncMaxRecords = 3;
        JSONObject jsonConfigFromFile = new JSONObject()
            .put(DataStoreConfiguration.ConfigKey.SYNC_INTERVAL_IN_MINUTES.toString(), expectedSyncIntervalMinutes)
            .put(DataStoreConfiguration.ConfigKey.SYNC_MAX_RECORDS.toString(), expectedSyncMaxRecords);
        DataStoreConfiguration dataStoreConfiguration = DataStoreConfiguration.builder(jsonConfigFromFile).build();
        assertEquals(expectedSyncIntervalMs, dataStoreConfiguration.getSyncIntervalMs());
        assertEquals(expectedSyncMaxRecords, dataStoreConfiguration.getSyncMaxRecords());
        assertEquals(DataStoreConfiguration.DEFAULT_SYNC_PAGE_SIZE, dataStoreConfiguration.getSyncPageSize());

        assertTrue(dataStoreConfiguration.getDataStoreConflictHandler() instanceof ApplyRemoteConflictHandler);
        assertTrue(dataStoreConfiguration.getDataStoreErrorHandler() instanceof DefaultDataStoreErrorHandler);
    }

    @Test
    public void testDefaultOverridenFromConfigurationAndObject() throws DataStoreException, JSONException {
        long expectedSyncIntervalMinutes = 6;
        long expectedSyncIntervalMs = TimeUnit.MINUTES.toMillis(expectedSyncIntervalMinutes);
        int expectedSyncMaxRecords = 3;
        DummyConflictHandler dummyConflictHandler = new DummyConflictHandler();
        DataStoreErrorHandler errorHandler = DefaultDataStoreErrorHandler.instance();

        DataStoreConfiguration configObject = DataStoreConfiguration
            .builder()
            .syncMaxRecords(expectedSyncMaxRecords)
            .dataStoreConflictHandler(dummyConflictHandler)
            .dataStoreErrorHandler(errorHandler)
            .build();

        JSONObject jsonConfigFromFile = new JSONObject()
            .put(DataStoreConfiguration.ConfigKey.SYNC_INTERVAL_IN_MINUTES.toString(), expectedSyncIntervalMinutes);
        DataStoreConfiguration dataStoreConfiguration = DataStoreConfiguration
            .builder(jsonConfigFromFile, configObject)
            .build();

        assertEquals(expectedSyncIntervalMs, dataStoreConfiguration.getSyncIntervalMs());
        assertEquals(expectedSyncMaxRecords, dataStoreConfiguration.getSyncMaxRecords());
        assertEquals(DataStoreConfiguration.DEFAULT_SYNC_PAGE_SIZE, dataStoreConfiguration.getSyncPageSize());

        assertEquals(dummyConflictHandler, dataStoreConfiguration.getDataStoreConflictHandler());
        assertEquals(errorHandler, dataStoreConfiguration.getDataStoreErrorHandler());
    }

    @Test(expected = DataStoreException.class)
    public void testInvalidKeyThrowsException() throws DataStoreException, JSONException {
        JSONObject jsonConfigFromFile = new JSONObject()
            .put("foo", "bar");
        DataStoreConfiguration
            .builder(jsonConfigFromFile).build();
    }

    private static final class DummyConflictHandler implements DataStoreConflictHandler {
        @Override
        public <T extends Model> void resolveConflict(@NonNull DataStoreConflictData<T> conflictData,
                                                      @NonNull Consumer<DataStoreConflictHandlerResult> onResult) {
            onResult.accept(DataStoreConflictHandlerResult.RETRY);
        }
    }
}
