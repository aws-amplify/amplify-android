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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Configuration options for the {@link AWSDataStorePlugin}.
 * Contains settings for remote synchronization, if enabled.
 */
public final class AWSDataStorePluginConfiguration {
    /**
     * At most one base sync may be performed within the period (now() - base sync interval).
     * By default, the interval is one day. The user may override this in their config file.
     */
    public static final long DEFAULT_BASE_SYNC_INTERVAL_MS = TimeUnit.DAYS.toMillis(1);
    private static final SyncMode DEFAULT_SYNC_MODE = SyncMode.LOCAL_ONLY;
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final SyncMode syncMode;
    private final long baseSyncIntervalMs;

    private AWSDataStorePluginConfiguration(SyncMode syncMode, long baseSyncIntervalMs) {
        this.syncMode = syncMode;
        this.baseSyncIntervalMs = baseSyncIntervalMs;
    }

    static AWSDataStorePluginConfiguration fromJson(JSONObject pluginJson) throws DataStoreException {
        SyncMode syncMode = DEFAULT_SYNC_MODE;
        long baseSyncIntervalMs = DEFAULT_BASE_SYNC_INTERVAL_MS;

        if (pluginJson == null) {
            return new AWSDataStorePluginConfiguration(syncMode, baseSyncIntervalMs);
        }

        final Iterator<String> jsonKeys = pluginJson.keys();
        while (jsonKeys.hasNext()) {
            final String keyString = jsonKeys.next();
            final ConfigKey configKey;
            try {
                configKey = ConfigKey.fromString(keyString);
            } catch (IllegalArgumentException noSuchConfigKey) {
                throw new DataStoreException(
                    "Saw unexpected config key: " + keyString,
                    "Make sure your amplifyconfiguration.json is valid."
                );
            }
            try {
                switch (configKey) {
                    case SYNC_MODE:
                        String jsonValue = pluginJson.getString(ConfigKey.SYNC_MODE.toString());
                        syncMode = SyncMode.fromJsonPropertyValue(jsonValue);
                        break;
                    case BASE_SYNC_INTERVAL_MS:
                        baseSyncIntervalMs = pluginJson.getLong(ConfigKey.BASE_SYNC_INTERVAL_MS.toString());
                        break;
                    case API_NAME:
                        LOG.warn(
                            "Found " + configKey + " in AWS DataStore Plugin config JSON. " +
                            "However, it currently has no effect / is not used by Amplify."
                        );
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported config key = " + configKey.toString());
                }
            } catch (JSONException jsonException) {
                throw new DataStoreException(
                    "Issue encountered while parsing configuration JSON",
                    jsonException, "Ensure your amplifyconfiguration.json is valid."
                );
            }
        }

        return new AWSDataStorePluginConfiguration(syncMode, baseSyncIntervalMs);
    }

    /**
     * Gets the synchronization mode.
     * @return Synchronization mode
     */
    @NonNull
    SyncMode getSyncMode() {
        return syncMode;
    }

    /**
     * Gets the base sync interval, expressed in a duration of milliseconds.
     * @return Base sync interval in milliseconds
     */
    long getBaseSyncIntervalMs() {
        return baseSyncIntervalMs;
    }

    enum ConfigKey {
        /**
         * Synchronization mode between device and cloud.
         */
        SYNC_MODE("syncMode"),

        /**
         * The name of a configured API endpoint in the {@link ApiCategory}. This name will be
         * used while making network calls. It is used as the first argument to, for example,
         * {@link ApiCategoryBehavior#query(String, Class, Consumer, Consumer)}.
         * This field does NOT need to specified. If it is not, then the first configured API
         * is used, automatically.
         */
        API_NAME("apiName"),

        /**
         * At most one base sync will be performed within this interval of time.
         * The interval is expressed in milliseconds.
         */
        BASE_SYNC_INTERVAL_MS("baseSyncIntervalMs");

        private final String key;

        ConfigKey(String key) {
            this.key = key;
        }

        @NonNull
        @Override
        public String toString() {
            return key;
        }

        /**
         * Lookup a config key by its string value.
         * @param anything Any string found in the key position of the plugin config JSON
         * @return An enumerate config key
         */
        @SuppressWarnings("unused")
        static ConfigKey fromString(@Nullable String anything) {
            for (ConfigKey possibleMatch : values()) {
                if (possibleMatch.toString().equals(anything)) {
                    return possibleMatch;
                }
            }
            throw new IllegalArgumentException(anything + " is not a config key.");
        }
    }

    /**
     * The mode of remote synchronization that is used by the DataStore.
     */
    enum SyncMode {
        /**
         * The DataStore will sync with a remote API.
         * When this value is set, an sibling apiName key is expected.
         */
        SYNC_WITH_API("api"),

        /**
         * No remote synchronization will take place. The DataStore will
         * only be used as a local repository.
         */
        LOCAL_ONLY("none");

        private final String jsonValue;

        SyncMode(String jsonValue) {
            this.jsonValue = jsonValue;
        }

        @NonNull
        public String getJsonValue() {
            return jsonValue;
        }

        @NonNull
        @Override
        public String toString() {
            return getJsonValue();
        }

        /**
         * Enumerate a sync mode from a JSON property value.
         * @param jsonPropertyValue A property value from a JSON config
         * @return An enumerated sync mode
         */
        static SyncMode fromJsonPropertyValue(@Nullable String jsonPropertyValue) throws DataStoreException {
            for (final SyncMode possibleMatch : values()) {
                if (possibleMatch.getJsonValue().equals(jsonPropertyValue)) {
                    return possibleMatch;
                }
            }
            throw new DataStoreException(
                "No sync mode known for value = " + jsonPropertyValue,
                "Try using one of: " + Arrays.toString(values())
            );
        }
    }
}
