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

import com.amplifyframework.testutils.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link AWSDataStorePluginConfiguration}.
 */
@Config(sdk = Build.VERSION_CODES.P, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public final class AWSDataStorePluginConfigurationTest {
    /**
     * If no configuration is specified, it's okay, sync mode will default to
     * {@link AWSDataStorePluginConfiguration.SyncMode#LOCAL_ONLY}.
     * @throws JSONException Technically possible as part of the method signature
     *                       {@link AWSDataStorePluginConfiguration#fromJson(JSONObject)},
     *                       and also while arranging the {@link JSONObject} input,
     *                       but JSONException is not expected this test, and would constitute a
     *                       test failure.
     */
    @Test
    public void defaultSyncModeUsedWhenJsonIsEmpty() throws DataStoreException {
        AWSDataStorePluginConfiguration config = AWSDataStorePluginConfiguration.fromJson(new JSONObject());
        assertEquals(AWSDataStorePluginConfiguration.SyncMode.LOCAL_ONLY, config.getSyncMode());
        assertFalse(config.hasApiName());
    }

    /**
     * When the user provides a value of "none" for the "syncMode" key, then API sync
     * will be disabled. No "apiName" property is required. It may or may not be provided.
     * @throws JSONException Technically possible as part of the method signature
     *                       {@link AWSDataStorePluginConfiguration#fromJson(JSONObject)},
     *                       and also while arranging the {@link JSONObject} input,
     *                       but JSONException is not expected this test, and would constitute a
     *                       test failure.
     */
    @Test
    public void remoteSyncDisabledWhenJsonRequestsDisable() throws JSONException, DataStoreException {
        AWSDataStorePluginConfiguration config = AWSDataStorePluginConfiguration.fromJson(new JSONObject()
            .put("syncMode", "none"));

        assertEquals(AWSDataStorePluginConfiguration.SyncMode.LOCAL_ONLY, config.getSyncMode());
        assertFalse(config.hasApiName());
    }

    /**
     * When configured for local-only operation (no synchronization), and if the "apiName"
     * key had not been configured, it is possible to construct a valid config object. But,
     * in this situation, the
     * {@link AWSDataStorePluginConfiguration#getSyncMode()} getter should throw an
     * {@link DataStoreException} instead of returning a null-value. The getter behaves
     * this way so that it may always return non-null. A caller may check for the presence of the
     * value by using {@link AWSDataStorePluginConfiguration#hasApiName()}.
     * @throws JSONException Technically possible as part of the method signature
     *                       {@link AWSDataStorePluginConfiguration#fromJson(JSONObject)},
     *                       and also while arranging the {@link JSONObject} input,
     *                       but JSONException is not expected this test, and would constitute a
     *                       test failure.
     *
     */
    @Test(expected = DataStoreException.class)
    public void getSyncModeThrowsConfigExceptionWhenItWasNotConfigured() throws JSONException, DataStoreException {
        String expectedApiName = RandomString.string();
        AWSDataStorePluginConfiguration config = AWSDataStorePluginConfiguration.fromJson(new JSONObject()
            .put("syncMode", "none")
            // .put("apiName", expectedApiName) - this is an optional field when syncMode == none
        );

        assertFalse(config.hasApiName()); // So, it's fine not to be here.
        config.getApiName(); // But you can't ask to get it, since it'd be null. This throws by design.
    }

    /**
     * If the user requests "none" for the value of "syncMode",
     * then remote synchronization shall be disabled in the config.
     * The user may populate an apiName and this will not override
     * the sync mode.
     * @throws JSONException Technically possible as part of the method signature
     *                       {@link AWSDataStorePluginConfiguration#fromJson(JSONObject)},
     *                       and also while arranging the {@link JSONObject} input,
     *                       but JSONException is not expected this test, and would constitute a
     *                       test failure.
     */
    @Test
    public void remoteSyncDisabledWhenJsonRequestsDisabledEvenThoughApiNameSpecified()
            throws JSONException, DataStoreException {
        String expectedApiName = RandomString.string();
        AWSDataStorePluginConfiguration config = AWSDataStorePluginConfiguration.fromJson(new JSONObject()
            .put("syncMode", "none")
            .put("apiName", expectedApiName));

        assertEquals(AWSDataStorePluginConfiguration.SyncMode.LOCAL_ONLY, config.getSyncMode());
        assertTrue(config.hasApiName());
        assertEquals(expectedApiName, config.getApiName());
    }

    /**
     * If API sync mode is requested, but the "apiName" key is not present in the
     * config, then an {@link DataStoreException} shall be thrown.
     * @throws JSONException Technically possible as part of the method signature
     *                       {@link AWSDataStorePluginConfiguration#fromJson(JSONObject)},
     *                       and also while arranging the {@link JSONObject} input,
     *                       but JSONException is not expected this test, and would constitute a
     *                       test failure.
     */
    @Test(expected = DataStoreException.class)
    public void throwsConfigurationExceptionWhenNoApiSpecifiedForRemoteSync()
            throws JSONException, DataStoreException {
        AWSDataStorePluginConfiguration.fromJson(new JSONObject()
            .put("syncMode", "api")
            // .put("apiName", "non-empty") cause of exception, this is not here
        );
    }

    /**
     * If API sync mode is requested, but the value for "apiName" is blank,
     * an {@link DataStoreException} shall be raised.
     * @throws JSONException Technically possible as part of the method signature
     *                       {@link AWSDataStorePluginConfiguration#fromJson(JSONObject)},
     *                       and also while arranging the {@link JSONObject} input,
     *                       but JSONException is not expected this test, and would constitute a
     *                       test failure.
     */
    @Test(expected = DataStoreException.class)
    public void throwsConfigurationExceptionWhenApiNameIsBlankForRemoteSync()
            throws JSONException, DataStoreException {
        AWSDataStorePluginConfiguration.fromJson(new JSONObject()
            .put("syncMode", "api")
            .put("apiName", "")); // cause of exception, has blank apiName, ""
    }

    /**
     * When the JSON configuration includes "api" as the value for "syncMode" (that is,
     * the user intent is to use an API for remote synchronization), and when the user
     * supplies a value for the "apiName" property, then the AWSDataStorePluginConfiguration
     * shall be populated successfully.
     * @throws JSONException Technically possible as part of the method signature
     *                       {@link AWSDataStorePluginConfiguration#fromJson(JSONObject)},
     *                       and also while arranging the {@link JSONObject} input,
     *                       but JSONException is not expected this test, and would constitute a
     *                       test failure.
     */
    @Test
    public void remoteSyncIsConfiguredWhenApiNameAlsoProvided() throws JSONException, DataStoreException {
        // Arrange some known api name. Configure JSON to include it, and request for API sync mode
        String expectedApiName = RandomString.string();
        AWSDataStorePluginConfiguration config = AWSDataStorePluginConfiguration.fromJson(new JSONObject()
            .put("syncMode", "api")
            .put("apiName", expectedApiName));

        // Assert: it worked; the sync mode and api name are well configured.
        assertEquals(AWSDataStorePluginConfiguration.SyncMode.SYNC_WITH_API, config.getSyncMode());
        assertTrue(config.hasApiName());
        assertEquals(expectedApiName, config.getApiName());
    }
}
