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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A user-provided configuration for the DataStore.
 */
public final class DataStoreConfiguration {
    static final String PLUGIN_CONFIG_KEY = "awsDataStorePlugin";
    static final long DEFAULT_SYNC_INTERVAL_MINUTES = TimeUnit.DAYS.toMinutes(1);
    static final long DEFAULT_SYNC_INTERVAL_MS = TimeUnit.MINUTES.toMillis(DEFAULT_SYNC_INTERVAL_MINUTES);
    static final int DEFAULT_SYNC_MAX_RECORDS = 10_000;
    static final int DEFAULT_SYNC_PAGE_SIZE = 1_000;

    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final DataStoreErrorHandler dataStoreErrorHandler;
    private final DataStoreConflictHandler dataStoreConflictHandler;
    private final Integer syncMaxRecords;
    private final Integer syncPageSize;
    private Long syncIntervalMs;

    private DataStoreConfiguration(
            DataStoreErrorHandler dataStoreErrorHandler,
            DataStoreConflictHandler dataStoreConflictHandler,
            Long syncIntervalInMinutes,
            Integer syncMaxRecords,
            Integer syncPageSize) {
        this.dataStoreErrorHandler = dataStoreErrorHandler;
        this.dataStoreConflictHandler = dataStoreConflictHandler;
        this.syncMaxRecords = syncMaxRecords;
        this.syncPageSize = syncPageSize;
        if (syncIntervalInMinutes != null) {
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
     * settings from the config file.
     * @param pluginJson DataStore plugin configuration from amplicationconfiguration.json
     * @param userProvidedConfiguration An instance of {@DataStoreConfiguration} with settings specified by the user
     *                                  which will be used as overrides.
     * @return A new builder instance
     * @throws DataStoreException exception thrown if there's an unexpected configuration key or
     * an invalid configuration value
     */
    @NonNull
    public static Builder builder(@NonNull JSONObject pluginJson,
                                  @Nullable DataStoreConfiguration userProvidedConfiguration)
        throws DataStoreException {
        Builder builder = builder(pluginJson);
        if (userProvidedConfiguration != null) {
            builder.syncPageSize(userProvidedConfiguration.getSyncPageSize())
                .syncMaxRecords(userProvidedConfiguration.getSyncMaxRecords())
                .syncIntervalInMinutes(
                    TimeUnit.MILLISECONDS.toMinutes(userProvidedConfiguration.getSyncIntervalMs()))
                .dataStoreErrorHandler(userProvidedConfiguration.getDataStoreErrorHandler())
                .dataStoreConflictHandler(userProvidedConfiguration.getDataStoreConflictHandler());
        }
        return builder;
    }

    /**
     * Begin building a new instance of {@link DataStoreConfiguration}.
     * @param pluginJson The dataStore configuration as a JSONObject
     * @return A new builder instance
     * @throws DataStoreException exception thrown if there's an unexpected configuration key or
     * an invalid configuration value
     */
    @NonNull
    public static Builder builder(@NonNull JSONObject pluginJson) throws DataStoreException {
        final Iterator<String> jsonKeys = pluginJson.keys();
        Builder builder = new Builder();
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
                        builder.syncIntervalInMinutes(pluginJson
                            .getLong(ConfigKey.SYNC_INTERVAL_IN_MINUTES.toString()));
                        break;
                    case SYNC_MAX_RECORDS:
                        builder.syncMaxRecords(pluginJson.getInt(ConfigKey.SYNC_MAX_RECORDS.toString()));
                        break;
                    case SYNC_PAGE_SIZE:
                        builder.syncPageSize(pluginJson.getInt(ConfigKey.SYNC_PAGE_SIZE.toString()));
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

        return builder;
    }

    /**
     * Creates an {@link DataStoreConfiguration} which uses all default values.
     * @return A default {@link DataStoreConfiguration}
     */
    @NonNull
    public static DataStoreConfiguration defaults() {
        return builder()
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
     * Get the sync interval. The sync interval is the amount of time after a base sync, during which
     * the optimized delta-sync may be requested, instead of a full base sync.
     * @return The sync interval
     */
    @IntRange(from = 0)
    public Long getSyncIntervalMs() {
        return this.syncIntervalMs == null ? DEFAULT_SYNC_INTERVAL_MS : this.syncIntervalMs;
    }

    /**
     * Gets the maximum number of records that the client wants to process, while it is requesting
     * a base/delta sync operation from AppSync.
     * @return The max number of records to process from AppSync.
     */
    @IntRange(from = 0)
    public Integer getSyncMaxRecords() {
        return this.syncMaxRecords == null ? DEFAULT_SYNC_MAX_RECORDS : this.syncMaxRecords;
    }

    /**
     * Gets the number of items that should be requested in page, from AppSync, during
     * a sync operation.
     * @return Desired size of a page of results from an AppSync sync response
     */
    @IntRange(from = 0)
    public Integer getSyncPageSize() {
        return this.syncPageSize == null ? DEFAULT_SYNC_PAGE_SIZE : this.syncPageSize;
    }

    /**
     * Builds instances of {@link AWSDataStorePlugin} by providing a variety of
     * configuration methods.
     */
    public static final class Builder {
        private DataStoreErrorHandler dataStoreErrorHandler;
        private DataStoreConflictHandler dataStoreConflictHandler;
        private Long syncIntervalInMinutes;
        private Integer syncMaxRecords;
        private Integer syncPageSize;

        private Builder() {
            this.dataStoreErrorHandler = DefaultDataStoreErrorHandler.instance();
            this.dataStoreConflictHandler = ApplyRemoteConflictHandler.instance(dataStoreErrorHandler);
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
         * Sets the duration of time after which delta syncs will not be preferred over base syncs.
         * @param syncIntervalInMinutes The amount of time that must elapse for delta syncs to not be considered
         * @return Current builder instance
         */
        @NonNull
        public Builder syncIntervalInMinutes(@IntRange(from = 0) Long syncIntervalInMinutes) {
            //Only set this value if the incoming value is null
            if (this.syncIntervalInMinutes == null) {
                this.syncIntervalInMinutes = syncIntervalInMinutes;
            }
            return Builder.this;
        }

        /**
         * Sets the maximum number of records, from the server, to process from a sync operation.
         * @param syncMaxRecords Max number of records client will consumer from server at one time
         * @return Current builder instance
         */
        @NonNull
        public Builder syncMaxRecords(@IntRange(from = 0) Integer syncMaxRecords) {
            //Only set this value if the incoming value is null
            if (this.syncMaxRecords == null) {
                this.syncMaxRecords = syncMaxRecords;
            }
            return Builder.this;
        }

        /**
         * Sets the number of items requested in each page of sync results.
         * @param syncPageSize Number of items requested per page in sync operation
         * @return Current builder
         */
        @NonNull
        public Builder syncPageSize(@IntRange(from = 0) Integer syncPageSize) {
            //Only set this value if the incoming value is null
            if (this.syncPageSize == null) {
                this.syncPageSize = syncPageSize;
            }
            return Builder.this;
        }

        /**
         * Builds a {@link DataStoreConfiguration} from the provided options.
         * @return A new {@link DataStoreConfiguration}.
         */
        @NonNull
        public DataStoreConfiguration build() {
            return new DataStoreConfiguration(
                dataStoreErrorHandler,
                dataStoreConflictHandler,
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
