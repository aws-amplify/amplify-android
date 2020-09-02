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
@SuppressWarnings("WeakerAccess") // This is a public API. Public methods available to user's package(s).
public final class DataStoreConfiguration {
    static final String PLUGIN_CONFIG_KEY = "awsDataStorePlugin";
    @VisibleForTesting
    static final long DEFAULT_SYNC_INTERVAL_MINUTES = TimeUnit.DAYS.toMinutes(1);
    @VisibleForTesting
    static final int DEFAULT_SYNC_MAX_RECORDS = 10_000;
    @VisibleForTesting 
    static final int DEFAULT_SYNC_PAGE_SIZE = 1_000;

    private final DataStoreErrorHandler dataStoreErrorHandler;
    private final DataStoreConflictHandler dataStoreConflictHandler;
    private final String apiName;
    private final Integer syncMaxRecords;
    private final Integer syncPageSize;
    private Long syncIntervalInMinutes;
    private Long syncIntervalMs;

    private DataStoreConfiguration(
            DataStoreErrorHandler dataStoreErrorHandler,
            DataStoreConflictHandler dataStoreConflictHandler,
            String apiName,
            Long syncIntervalInMinutes,
            Integer syncMaxRecords,
            Integer syncPageSize) {
        this.dataStoreErrorHandler = dataStoreErrorHandler;
        this.dataStoreConflictHandler = dataStoreConflictHandler;
        this.apiName = apiName;
        this.syncMaxRecords = syncMaxRecords;
        this.syncPageSize = syncPageSize;
        if (syncIntervalInMinutes != null) {
            this.syncIntervalInMinutes = syncIntervalInMinutes;
            this.syncIntervalMs = TimeUnit.MINUTES.toMillis(syncIntervalInMinutes);
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
        DataStoreErrorHandler dataStoreErrorHandler = DefaultDataStoreErrorHandler.instance();
        return builder()
            .dataStoreErrorHandler(dataStoreErrorHandler)
            .dataStoreConflictHandler(ApplyRemoteConflictHandler.instance(dataStoreErrorHandler))
            .apiName(null)
            .syncIntervalInMinutes(DEFAULT_SYNC_INTERVAL_MINUTES)
            .syncPageSize(DEFAULT_SYNC_PAGE_SIZE)
            .syncMaxRecords(DEFAULT_SYNC_MAX_RECORDS)
            .build();
    }

    /**
     * Gets the data store error handler.
     * @return Data store error handler.
     */
    @NonNull
    public DataStoreErrorHandler getDataStoreErrorHandler() {
        return this.dataStoreErrorHandler;
    }

    /**
     * Gets the data store conflict handler.
     * @return Data store conflict handler
     */
    @NonNull
    public DataStoreConflictHandler getDataStoreConflictHandler() {
        return this.dataStoreConflictHandler;
    }

    /**
     * Gets the name of the API that is used for DataStore synchronization (if any).
     * This is one of the entries the `amplifyconfiguration.json` which specifies
     * a valid AppSync endpoint that has been configured with auto-merge conflict resolution.
     * This field is optional. If not provided, the DataStore tries to use whatever is in the config file.
     * @return the name of an API to use for DataStore configuration, if present
     */
    @Nullable
    public String getApiName() {
        return this.apiName;
    }

    /**
     * Get the sync interval, expressed in milliseconds. The sync interval is the amount of
     * time after a base sync, during which the optimized delta-sync may be requested, instead
     * of a full base sync.
     * @return The sync interval, expressed in milliseconds
     */
    @IntRange(from = 0)
    public Long getSyncIntervalMs() {
        return this.syncIntervalMs;
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
        if (!ObjectsCompat.equals(getDataStoreErrorHandler(), that.getDataStoreConflictHandler())) {
            return false;
        }
        if (!ObjectsCompat.equals(getDataStoreConflictHandler(), that.getDataStoreConflictHandler())) {
            return false;
        }
        if (!ObjectsCompat.equals(getApiName(), that.getApiName())) {
            return false;
        }
        if (!ObjectsCompat.equals(getSyncMaxRecords(), that.getSyncMaxRecords())) {
            return false;
        }
        if (!ObjectsCompat.equals(getSyncPageSize(), that.getSyncPageSize())) {
            return false;
        }
        if (!ObjectsCompat.equals(getSyncIntervalInMinutes(), that.getSyncIntervalInMinutes())) {
            return false;
        }
        return ObjectsCompat.equals(getSyncIntervalMs(), that.getSyncIntervalMs());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public int hashCode() {
        int result = getDataStoreErrorHandler() != null ? getDataStoreErrorHandler().hashCode() : 0;
        result = 31 * result + (getDataStoreConflictHandler() != null ? getDataStoreConflictHandler().hashCode() : 0);
        result = 31 * result + (getApiName() != null ? getApiName().hashCode() : 0);
        result = 31 * result + (getSyncMaxRecords() != null ? getSyncMaxRecords().hashCode() : 0);
        result = 31 * result + (getSyncPageSize() != null ? getSyncPageSize().hashCode() : 0);
        result = 31 * result + (getSyncIntervalInMinutes() != null ? getSyncIntervalInMinutes().hashCode() : 0);
        result = 31 * result + (getSyncIntervalMs() != null ? getSyncIntervalMs().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataStoreConfiguration{" +
            "dataStoreErrorHandler=" + dataStoreErrorHandler +
            ", dataStoreConflictHandler=" + dataStoreConflictHandler +
            ", apiName=" + apiName +
            ", syncMaxRecords=" + syncMaxRecords +
            ", syncPageSize=" + syncPageSize +
            ", syncIntervalInMinutes=" + syncIntervalInMinutes +
            ", syncIntervalMs=" + syncIntervalMs +
            '}';
    }

    /**
     * Builds instances of {@link AWSDataStorePlugin} by providing a variety of
     * configuration methods.
     */
    public static final class Builder {
        private DataStoreErrorHandler dataStoreErrorHandler;
        private DataStoreConflictHandler dataStoreConflictHandler;
        private String apiName;
        private Long syncIntervalInMinutes;
        private Integer syncMaxRecords;
        private Integer syncPageSize;
        private boolean ensureDefaults;
        private JSONObject pluginJson;
        private DataStoreConfiguration userProvidedConfiguration;

        private Builder() {
            this.dataStoreErrorHandler = DefaultDataStoreErrorHandler.instance();
            this.dataStoreConflictHandler = ApplyRemoteConflictHandler.instance(dataStoreErrorHandler);
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
         * @param dataStoreConflictHandler A handler to invoke upon sync conflicts
         * @return Current builder
         */
        @NonNull
        public Builder dataStoreConflictHandler(@NonNull DataStoreConflictHandler dataStoreConflictHandler) {
            this.dataStoreConflictHandler = Objects.requireNonNull(dataStoreConflictHandler);
            return Builder.this;
        }

        /**
         * Sets a handler function to be applied when the DataStore encounters an unrecoverable error
         * in one of its ongoing background operations (model synchronization).
         * @param dataStoreErrorHandler A handler for unrecoverable background errors
         * @return Current builder instance
         */
        @NonNull
        public Builder dataStoreErrorHandler(@NonNull DataStoreErrorHandler dataStoreErrorHandler) {
            this.dataStoreErrorHandler = Objects.requireNonNull(dataStoreErrorHandler);
            return Builder.this;
        }

        /**
         * Sets the name of the API which will be used for model synchronization.
         * This name corresponds to a configured API in the `amplifyconfiguraiton.json`'s API
         * category config. This is an optional configuration value. If it is not provided,
         * and the value is null, then the DataStore will attempt to use the first (and only)
         * configured API in the config file. This option is useful when you have *multiple*
         * APIs specified in the config file, and need to deliberately select one of them, only.
         * @param apiName Name of API to use for model synchronization
         * @return Current builder instance
         */
        @NonNull
        public Builder apiName(@Nullable String apiName) {
            this.apiName = apiName;
            return Builder.this;
        }

        /**
         * Sets the duration of time after which delta syncs will not be preferred over base syncs.
         * @param syncIntervalInMinutes The amount of time that must elapse for delta syncs to not be considered
         * @return Current builder instance
         */
        @NonNull
        public Builder syncIntervalInMinutes(@IntRange(from = 0) Long syncIntervalInMinutes) {
            this.syncIntervalInMinutes = syncIntervalInMinutes;
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
                        case API_NAME:
                            this.apiName(pluginJson.getString(ConfigKey.API_NAME.toString()));
                            break;
                        case SYNC_INTERVAL_IN_MINUTES:
                            this.syncIntervalInMinutes(pluginJson
                                .getLong(ConfigKey.SYNC_INTERVAL_IN_MINUTES.toString()));
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
            dataStoreErrorHandler = userProvidedConfiguration.getDataStoreErrorHandler();
            dataStoreConflictHandler = userProvidedConfiguration.getDataStoreConflictHandler();
            apiName = userProvidedConfiguration.getApiName();
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
                dataStoreErrorHandler = getValueOrDefault(
                    dataStoreErrorHandler,
                    DefaultDataStoreErrorHandler.instance());
                dataStoreConflictHandler = getValueOrDefault(
                    dataStoreConflictHandler,
                    ApplyRemoteConflictHandler.instance(dataStoreErrorHandler));
                apiName = getValueOrDefault(apiName, null);
                syncIntervalInMinutes = getValueOrDefault(syncIntervalInMinutes, DEFAULT_SYNC_INTERVAL_MINUTES);
                syncMaxRecords = getValueOrDefault(syncMaxRecords, DEFAULT_SYNC_MAX_RECORDS);
                syncPageSize = getValueOrDefault(syncPageSize, DEFAULT_SYNC_PAGE_SIZE);
            }
            return new DataStoreConfiguration(
                dataStoreErrorHandler,
                dataStoreConflictHandler,
                apiName,
                syncIntervalInMinutes,
                syncMaxRecords,
                syncPageSize
            );
        }
    }

    enum ConfigKey {
        /**
         * The value associated to this configuration key will contain the name of an API
         * that has been configured for the API category. This API will be used for all
         * model synchronization in the DataStore. It is useful to supply this value in
         * situations where the configuration file specifies multiple APIs. In this case,
         * the DataStore's default behavior (of just picking the first configured API)
         * will be overridden.
         */
        API_NAME("apiName"),
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
