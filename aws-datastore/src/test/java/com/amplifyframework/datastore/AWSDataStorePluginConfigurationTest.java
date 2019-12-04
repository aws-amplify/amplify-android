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

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link AWSDataStorePluginConfiguration}.
 */
@Config(sdk = Build.VERSION_CODES.P, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public final class AWSDataStorePluginConfigurationTest {
    /**
     * If configuration is null, it's okay, sync mode will default to
     * {@link AWSDataStorePluginConfiguration.SyncMode#LOCAL_ONLY}.
     * @throws DataStoreException from DataStore configuration
     */
    @Test
    public void defaultSyncModeUsedWhenJsonIsNull() throws DataStoreException {
        AWSDataStorePluginConfiguration config = AWSDataStorePluginConfiguration.fromJson(null);

        assertEquals(AWSDataStorePluginConfiguration.SyncMode.LOCAL_ONLY, config.getSyncMode());
    }

    /**
     * If no configuration is specified, it's okay, sync mode will default to
     * {@link AWSDataStorePluginConfiguration.SyncMode#LOCAL_ONLY}.
     * @throws DataStoreException from DataStore configuration
     */
    @Test
    public void defaultSyncModeUsedWhenJsonIsEmpty() throws DataStoreException {
        AWSDataStorePluginConfiguration config = AWSDataStorePluginConfiguration.fromJson(new JSONObject());

        assertEquals(AWSDataStorePluginConfiguration.SyncMode.LOCAL_ONLY, config.getSyncMode());
    }

    /**
     * When the user provides a value of "none" for the "syncMode" key, then API sync
     * will be disabled.
     * @throws JSONException Technically possible as part of the method signature
     *                       {@link AWSDataStorePluginConfiguration#fromJson(JSONObject)},
     *                       and also while arranging the {@link JSONObject} input,
     *                       but JSONException is not expected this test, and would constitute a
     *                       test failure.
     * @throws DataStoreException from DataStore configuration
     */
    @Test
    public void remoteSyncDisabledWhenJsonRequestsDisable() throws JSONException, DataStoreException {
        AWSDataStorePluginConfiguration config = AWSDataStorePluginConfiguration.fromJson(new JSONObject()
            .put("syncMode", "none"));

        assertEquals(AWSDataStorePluginConfiguration.SyncMode.LOCAL_ONLY, config.getSyncMode());
    }

    /**
     * When the JSON configuration includes "api" as the value for "syncMode" (that is,
     * the user intent is to use an API for remote synchronization), then the
     * AWSDataStorePluginConfiguration shall be populated successfully.
     * @throws JSONException Technically possible as part of the method signature
     *                       {@link AWSDataStorePluginConfiguration#fromJson(JSONObject)},
     *                       and also while arranging the {@link JSONObject} input,
     *                       but JSONException is not expected this test, and would constitute a
     *                       test failure.
     * @throws DataStoreException from DataStore configuration
     */
    @Test
    public void remoteSyncIsConfiguredWhenApiNameAlsoProvided() throws JSONException, DataStoreException {
        // Arrange some known api name. Configure JSON to include it, and request for API sync mode
        AWSDataStorePluginConfiguration config = AWSDataStorePluginConfiguration.fromJson(new JSONObject()
            .put("syncMode", "api"));

        // Assert: it worked; the sync mode and api name are well configured.
        assertEquals(AWSDataStorePluginConfiguration.SyncMode.SYNC_WITH_API, config.getSyncMode());
    }
}
