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

import android.text.TextUtils;
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
    private final String apiName;

    private AWSDataStorePluginConfiguration(final SyncMode syncMode, final String apiName) {
        this.syncMode = syncMode;
        this.apiName = apiName;
    }

    static AWSDataStorePluginConfiguration fromJson(JSONObject pluginJson) throws DataStoreException {
        // If no sync mode is specified, we just use the default (no sync) and continue
        if (!pluginJson.has("syncMode")) {
            return new AWSDataStorePluginConfiguration(SyncMode.LOCAL_ONLY, null);
        }

        try {
            // If user has specified a sync mode, find out what it was.
            final SyncMode syncMode = SyncMode.fromJsonPropertyValue(pluginJson.getString("syncMode"));

            // If user has provided an API name, obtain it.
            String apiName = null;
            if (pluginJson.has("apiName")) {
                apiName = pluginJson.getString("apiName");
            }

            // If he user wanted remote sync, a non-empty value associated to the apiName key is required.
            if (SyncMode.SYNC_WITH_API.equals(syncMode) && TextUtils.isEmpty(apiName)) {
                throw new DataStoreException(
                        "Requested SyncMode of " + SyncMode.SYNC_WITH_API.jsonValue() +
                                " but required key \"apiName\" was missing/empty.",
                        "Check your amplifyconfiguration.json file to ensure the field is present."
                );
            }

            return new AWSDataStorePluginConfiguration(syncMode, apiName);
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
     * Gets the name of the API to use when the {@link #getSyncMode()} is
     * {@link SyncMode#SYNC_WITH_API}. If the Value of {@link #getSyncMode()} is
     * anything else, then this will throw an {@link DataStoreException}.
     * @return The name of the API to use, if {@link #getSyncMode()} returns
     *         {@link SyncMode#SYNC_WITH_API}.
     * @throws DataStoreException If there is no API name to return
     */
    @NonNull
    String getApiName() throws DataStoreException {
        if (apiName == null) {
            throw new DataStoreException("No API name was specified.",
                    AmplifyException.TODO_RECOVERY_SUGGESTION);
        }
        return apiName;
    }

    /**
     * Returns true if there is a configuration available for the apiName property;
     * false, otherwise. Use this to inspect the config prior to invoking
     * {@link #getApiName()}.
     * @return True if the config specifies an API name. This may be true even if
     *         {@link #getSyncMode()} may return {@link SyncMode#LOCAL_ONLY}.
     */
    boolean hasApiName() {
        return apiName != null;
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
