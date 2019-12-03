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

import com.amplifyframework.AmplifyException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configuration options for the {@link AWSDataStorePlugin}.
 * Contains settings for remote synchronization, if enabled.
 */
final class AWSDataStorePluginConfiguration {

    private final SyncMode syncMode;

    private AWSDataStorePluginConfiguration(final SyncMode syncMode) {
        this.syncMode = syncMode;
    }

    static AWSDataStorePluginConfiguration fromJson(JSONObject pluginJson) throws DataStoreException {
        // If no sync mode is specified, we just use the default (no sync) and continue
        if (pluginJson == null || !pluginJson.has("syncMode")) {
            return new AWSDataStorePluginConfiguration(SyncMode.LOCAL_ONLY);
        }

        try {
            // If user has specified a sync mode, find out what it was.
            final SyncMode syncMode = SyncMode.fromJsonPropertyValue(pluginJson.getString("syncMode"));

            return new AWSDataStorePluginConfiguration(syncMode);
        } catch (JSONException exception) {
            throw new DataStoreException(
                    "Issue encountered while parsing configuration JSON",
                    exception,
                    "Check the attached exception for more details."
            );
        }

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
        public String jsonValue() {
            return jsonValue;
        }

        @NonNull
        @Override
        public String toString() {
            return jsonValue();
        }

        /**
         * Enumerate a sync mode from a JSON property value.
         * @param jsonPropertyValue A property value from a JSON config
         * @return An enumerated sync mode
         */
        static SyncMode fromJsonPropertyValue(@Nullable String jsonPropertyValue) throws DataStoreException {
            for (final SyncMode possibleMatch : values()) {
                if (possibleMatch.jsonValue().equals(jsonPropertyValue)) {
                    return possibleMatch;
                }
            }
            throw new DataStoreException("No sync mode known for jsonPropertyValue = " + jsonPropertyValue,
                    AmplifyException.TODO_RECOVERY_SUGGESTION);
        }
    }
}
