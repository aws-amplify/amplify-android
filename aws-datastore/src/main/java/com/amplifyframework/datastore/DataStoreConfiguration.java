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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.ObjectsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration options for {@link AWSDataStorePlugin}.
 */
public final class DataStoreConfiguration {
    static final String PLUGIN_CONFIG_KEY = "awsDataStorePlugin";
    @VisibleForTesting
    static final long DEFAULT_SYNC_INTERVAL_MINUTES = TimeUnit.DAYS.toMinutes(1);
    @VisibleForTesting
    static final int DEFAULT_SYNC_MAX_RECORDS = 10_000;
    @VisibleForTesting 
    static final int DEFAULT_SYNC_PAGE_SIZE = 1_000;

    private final DataStoreErrorHandler errorHandler;
    private final DataStoreConflictHandler conflictHandler;
    private final Integer syncMaxRecords;
    private final Integer syncPageSize;
    private Long syncIntervalInMinutes;

    private DataStoreConfiguration(
            DataStoreErrorHandler errorHandler,
            DataStoreConflictHandler conflictHandler,
            Long syncIntervalInMinutes,
            Integer syncMaxRecords,
            Integer syncPageSize) {
        this.errorHandler = errorHandler;
        this.conflictHandler = conflictHandler;
        this.syncMaxRecords = syncMaxRecords;
        this.syncPageSize = syncPageSize;
        if (syncIntervalInMinutes != null) {
            this.syncIntervalInMinutes = syncIntervalInMinutes;
        }
    }

    /**
     * Begin building a new instance of {@link DataStoreConfiguration}.
     * @return A new builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Begin building a new instance of {@link DataStoreConfiguration} by reading DataStore
     * settings from the config file and an optional set of user-provided overrides.
     * @param pluginJson DataStore plugin configuration from amplifyconfiguration.json
     * @param userProvidedConfiguration An instance of {@link DataStoreConfiguration}
     *                                 with settings specified by the user
     *                                  which will be used as overrides.
     * @return A new builder instance
     * an invalid configuration value
     */
    @NonNull
    static Builder builder(@NonNull JSONObject pluginJson,
                           @Nullable DataStoreConfiguration userProvidedConfiguration) {
        return new Builder(pluginJson, userProvidedConfiguration);
    }

    /**
     * Begin building a new instance of {@link DataStoreConfiguration}.
     * @param pluginJson The dataStore configuration as a JSONObject
     * @return A new builder instance
     * an invalid configuration value
     */
    @NonNull
    static Builder builder(@NonNull JSONObject pluginJson) {
        return builder(pluginJson, null);
    }

    /**
     * Creates an {@link DataStoreConfiguration} which uses all default values.
     * @return A default {@link DataStoreConfiguration}
     * @throws DataStoreException exception thrown if there's an unexpected configuration key or
     * an invalid configuration value
     */
    @NonNull
    public static DataStoreConfiguration defaults() throws DataStoreException {
        DataStoreErrorHandler errorHandler = DefaultDataStoreErrorHandler.instance();
        return builder()
            .errorHandler(errorHandler)
            .conflictHandler(ApplyRemoteConflictHandler.instance(errorHandler))
            .syncInterval(DEFAULT_SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .syncPageSize(DEFAULT_SYNC_PAGE_SIZE)
            .syncMaxRecords(DEFAULT_SYNC_MAX_RECORDS)
            .build();
    }

    /**
     * Gets the data store error handler.
     * @return Data store error handler.
     */
    @NonNull
    public DataStoreErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    /**
     * Gets the data store conflict handler.
     * @return Data store conflict handler
     */
    @NonNull
    public DataStoreConflictHandler getConflictHandler() {
        return this.conflictHandler;
    }

    /**
     * Get the sync interval, expressed in milliseconds. The sync interval is the amount of
     * time after a base sync, during which the optimized delta-sync may be requested, instead
     * of a full base sync.
     * @return The sync interval, expressed in milliseconds
     */
    @IntRange(from = 0)
    public Long getSyncIntervalMs() {
        return TimeUnit.MINUTES.toMillis(syncIntervalInMinutes);
    }

    /**
     * Gets the sync interval, expressed in minutes. This method serves the same purpose
     * as {@link #getSyncIntervalMs()} -- except, for convenience, returns the value in
     * minutes, not milliseconds.
     * @return The sync interval, expressed in minutes
     */
    @IntRange(from = 0)
    public Long getSyncIntervalInMinutes() {
        return this.syncIntervalInMinutes;
    }

    /**
     * Gets the maximum number of records that the client wants to process, while it is requesting
     * a base/delta sync operation from AppSync.
     * @return The max number of records to process from AppSync.
     */
    @IntRange(from = 0)
    public Integer getSyncMaxRecords() {
        return this.syncMaxRecords;
    }

    /**
     * Gets the number of items that should be requested in page, from AppSync, during
     * a sync operation.
     * @return Desired size of a page of results from an AppSync sync response
     */
    @IntRange(from = 0)
    public Integer getSyncPageSize() {
        return this.syncPageSize;
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        DataStoreConfiguration that = (DataStoreConfiguration) thatObject;
        if (!ObjectsCompat.equals(getErrorHandler(), that.getConflictHandler())) {
            return false;
        }
        if (!ObjectsCompat.equals(getConflictHandler(), that.getConflictHandler())) {
            return false;
        }
        if (!ObjectsCompat.equals(getSyncMaxRecords(), that.getSyncMaxRecords())) {
            return false;
        }
        if (!ObjectsCompat.equals(getSyncPageSize(), that.getSyncPageSize())) {
            return false;
        }
        return ObjectsCompat.equals(getSyncIntervalInMinutes(), that.getSyncIntervalInMinutes());
    }

    @Override
    public int hashCode() {
        int result = getErrorHandler() != null ? getErrorHandler().hashCode() : 0;
        result = 31 * result + (getConflictHandler() != null ? getConflictHandler().hashCode() : 0);
        result = 31 * result + (getSyncMaxRecords() != null ? getSyncMaxRecords().hashCode() : 0);
        result = 31 * result + (getSyncPageSize() != null ? getSyncPageSize().hashCode() : 0);
        result = 31 * result + (getSyncIntervalInMinutes() != null ? getSyncIntervalInMinutes().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataStoreConfiguration{" +
            "errorHandler=" + errorHandler +
            ", conflictHandler=" + conflictHandler +
            ", syncMaxRecords=" + syncMaxRecords +
            ", syncPageSize=" + syncPageSize +
            ", syncIntervalInMinutes=" + syncIntervalInMinutes +
            '}';
    }

    /**
     * Builds instances of {@link AWSDataStorePlugin} by providing a variety of
     * configuration methods.
     */
    public static final class Builder {
        private DataStoreErrorHandler errorHandler;
        private DataStoreConflictHandler conflictHandler;
        private Long syncIntervalInMinutes;
        private Integer syncMaxRecords;
        private Integer syncPageSize;
        private boolean ensureDefaults;
        private JSONObject pluginJson;
        private DataStoreConfiguration userProvidedConfiguration;

        private Builder() {
            this.errorHandler = DefaultDataStoreErrorHandler.instance();
            this.conflictHandler = ApplyRemoteConflictHandler.instance(errorHandler);
            this.ensureDefaults = false;
        }

        private Builder(JSONObject pluginJson, DataStoreConfiguration userProvidedConfiguration) {
            this();
            this.pluginJson = pluginJson;
            this.userProvidedConfiguration = userProvidedConfiguration;
            this.ensureDefaults = true;
        }

        /**
         * A handler that will be invoked whenever there is a conflict between two model instances,
         * one in the local store, and one from the remote server, as received from a sync operation.
         * @param conflictHandler A handler to invoke upon sync conflicts
         * @return Current builder
         */
        @NonNull
        public Builder conflictHandler(@NonNull DataStoreConflictHandler conflictHandler) {
            this.conflictHandler = Objects.requireNonNull(conflictHandler);
            return Builder.this;
        }

        /**
         * Sets a handler function to be applied when the DataStore encounters an unrecoverable error
         * in one of its ongoing background operations (model synchronization).
         * @param errorHandler A handler for unrecoverable background errors
         * @return Current builder instance
         */
        @NonNull
        public Builder errorHandler(@NonNull DataStoreErrorHandler errorHandler) {
            this.errorHandler = Objects.requireNonNull(errorHandler);
            return Builder.this;
        }

        /**
         * Sets the duration of time after which delta syncs will not be preferred over base syncs.
         * @param duration The amount of time that must elapse for delta syncs to not be considered
         * @param timeUnit The time unit of the duration field
         * @return Current builder instance
         */
        @NonNull
        public Builder syncInterval(@IntRange(from = 0) long duration, TimeUnit timeUnit) {
            this.syncIntervalInMinutes = timeUnit.toMinutes(duration);
            return Builder.this;
        }

        /**
         * Sets the maximum number of records, from the server, to process from a sync operation.
         * @param syncMaxRecords Max number of records client will consumer from server at one time
         * @return Current builder instance
         */
        @NonNull
        public Builder syncMaxRecords(@IntRange(from = 0) Integer syncMaxRecords) {
            this.syncMaxRecords = syncMaxRecords;
            return Builder.this;
        }

        /**
         * Sets the number of items requested in each page of sync results.
         * @param syncPageSize Number of items requested per page in sync operation
         * @return Current builder
         */
        @NonNull
        public Builder syncPageSize(@IntRange(from = 0) Integer syncPageSize) {
            this.syncPageSize = syncPageSize;
            return Builder.this;
        }

        private void populateSettingsFromJson() throws DataStoreException {
            if (pluginJson == null) {
                return;
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
                        case SYNC_INTERVAL_IN_MINUTES:
                            long duration = pluginJson.getLong(ConfigKey.SYNC_INTERVAL_IN_MINUTES.toString());
                            this.syncInterval(duration, TimeUnit.MINUTES);
                            break;
                        case SYNC_MAX_RECORDS:
                            this.syncMaxRecords(pluginJson.getInt(ConfigKey.SYNC_MAX_RECORDS.toString()));
                            break;
                        case SYNC_PAGE_SIZE:
                            this.syncPageSize(pluginJson.getInt(ConfigKey.SYNC_PAGE_SIZE.toString()));
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
        }

        private void applyUserProvidedConfiguration() {
            if (userProvidedConfiguration == null) {
                return;
            }
            errorHandler = userProvidedConfiguration.getErrorHandler();
            conflictHandler = userProvidedConfiguration.getConflictHandler();
            syncIntervalInMinutes = getValueOrDefault(
                userProvidedConfiguration.getSyncIntervalInMinutes(),
                syncIntervalInMinutes);
            syncMaxRecords = getValueOrDefault(userProvidedConfiguration.getSyncMaxRecords(), syncMaxRecords);
            syncPageSize = getValueOrDefault(userProvidedConfiguration.getSyncPageSize(), syncPageSize);
        }

        private static <T> T getValueOrDefault(T value, T defaultValue) {
            return value == null ? defaultValue : value;
        }

        /**
         * Builds a {@link DataStoreConfiguration} from the provided options.
         * @return A new {@link DataStoreConfiguration}.
         * @throws DataStoreException thrown if it's unable to parse the provided JSON or
         * an invalid value if provided.
         */
        @NonNull
        public DataStoreConfiguration build() throws DataStoreException {
            populateSettingsFromJson();
            applyUserProvidedConfiguration();
            if (ensureDefaults) {
                errorHandler = getValueOrDefault(
                    errorHandler,
                    DefaultDataStoreErrorHandler.instance());
                conflictHandler = getValueOrDefault(
                    conflictHandler,
                    ApplyRemoteConflictHandler.instance(errorHandler));
                syncIntervalInMinutes = getValueOrDefault(syncIntervalInMinutes, DEFAULT_SYNC_INTERVAL_MINUTES);
                syncMaxRecords = getValueOrDefault(syncMaxRecords, DEFAULT_SYNC_MAX_RECORDS);
                syncPageSize = getValueOrDefault(syncPageSize, DEFAULT_SYNC_PAGE_SIZE);
            }
            return new DataStoreConfiguration(
                errorHandler,
                conflictHandler,
                syncIntervalInMinutes,
                syncMaxRecords,
                syncPageSize
            );
        }
    }

    enum ConfigKey {
        /**
         * At most one base sync will be performed within this interval of time.
         * The interval is expressed in milliseconds.
         */
        SYNC_INTERVAL_IN_MINUTES("syncIntervalInMinutes"),
        /**
         * Number of items requested per page in sync operation.
         */
        SYNC_PAGE_SIZE("syncPageSize"),
        /**
         * Number of records that the client wants to process, while it is requesting
         * a base/delta sync operation from AppSync.
         */
        SYNC_MAX_RECORDS("syncMaxRecords");

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
        static ConfigKey fromString(@Nullable String anything) {
            for (ConfigKey possibleMatch : values()) {
                if (possibleMatch.toString().equals(anything)) {
                    return possibleMatch;
                }
            }
            throw new IllegalArgumentException(anything + " is not a config key.");
        }
    }
}
