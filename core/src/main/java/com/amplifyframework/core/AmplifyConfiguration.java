/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.analytics.AnalyticsCategoryConfiguration;
import com.amplifyframework.api.ApiCategoryConfiguration;
import com.amplifyframework.auth.AuthCategoryConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.category.EmptyCategoryConfiguration;
import com.amplifyframework.datastore.DataStoreCategoryConfiguration;
import com.amplifyframework.geo.GeoCategoryConfiguration;
import com.amplifyframework.hub.HubCategoryConfiguration;
import com.amplifyframework.logging.LoggingCategoryConfiguration;
import com.amplifyframework.notifications.NotificationsCategoryConfiguration;
import com.amplifyframework.predictions.PredictionsCategoryConfiguration;
import com.amplifyframework.storage.StorageCategoryConfiguration;
import com.amplifyframework.util.Immutable;
import com.amplifyframework.util.UserAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AmplifyConfiguration serves as the top-level configuration object for the
 * Amplify framework. It is usually populated from amplifyconfiguration.json.
 * Contains all configurations for all categories and plugins used by system.
 */
public final class AmplifyConfiguration {
    private static final String DEFAULT_IDENTIFIER = "amplifyconfiguration";

    private final Map<String, CategoryConfiguration> categoryConfigurations;
    private final Map<UserAgent.Platform, String> platformVersions;
    private final boolean devMenuEnabled;

    /**
     * Constructs a new AmplifyConfiguration object.
     * @param configs Category configurations
     */
    @VisibleForTesting
    @SuppressWarnings("WeakerAccess") // These are created and accessed as public API
    public AmplifyConfiguration(@NonNull Map<String, CategoryConfiguration> configs) {
        // Dev menu is disabled by default
        this(configs, new LinkedHashMap<>(), false);
    }

    /**
     * Constructs a new AmplifyConfiguration object.
     * @param configs Category configurations
     * @param devMenuEnabled Specifies whether the developer menu should be enabled (debug mode only) or not
     */
    @VisibleForTesting
    @SuppressWarnings("WeakerAccess") // These are created and accessed as public API
    public AmplifyConfiguration(@NonNull Map<String, CategoryConfiguration> configs, boolean devMenuEnabled) {
        this(configs, new LinkedHashMap<>(), devMenuEnabled);
    }

    private AmplifyConfiguration(
            Map<String, CategoryConfiguration> configs,
            Map<UserAgent.Platform, String> platformVersions,
            boolean devMenuEnabled
    ) {
        this.categoryConfigurations = new HashMap<>();
        this.categoryConfigurations.putAll(configs);
        this.platformVersions = platformVersions;
        this.devMenuEnabled = devMenuEnabled;
    }

    /**
     * Build an {@link AmplifyConfiguration} directly from an {@link JSONObject}.
     * Users should prefer loading from resources files via {@link #fromConfigFile(Context)},
     * or {@link #fromConfigFile(Context, int)}.
     * @param json A JSON object
     * @return An AmplifyConfiguration
     * @throws AmplifyException If the JSON does not represent a valid AmplifyConfiguration
     */
    @NonNull
    public static AmplifyConfiguration fromJson(@NonNull JSONObject json) throws AmplifyException {
        return builder(json).build();
    }

    /**
     * Creates an AmplifyConfiguration from an amplifyconfiguration.json file.
     * @param context Context needed for reading JSON file
     * @return An Amplify configuration instance
     * @throws AmplifyException If there is a problem in the config file
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static AmplifyConfiguration fromConfigFile(@NonNull Context context) throws AmplifyException {
        return builder(context).build();
    }

    /**
     * Creates an AmplifyConfiguration from a particular configuration file.
     * @param context Android Context
     * @param configFileResourceId
     *        The Android resource ID of a raw resource which contains
     *        an amplify configuration as JSON
     * @return An Amplify configuration instance
     * @throws AmplifyException If there is a problem in the config file
     */
    @NonNull
    public static AmplifyConfiguration fromConfigFile(
            @NonNull Context context, @RawRes int configFileResourceId) throws AmplifyException {
        return builder(context, configFileResourceId).build();
    }

    /**
     * Obtain a map of additional platforms that are using this library
     * for tracking usage metrics.
     * @return A map of additional platforms and their versions.
     */
    @NonNull
    public Map<UserAgent.Platform, String> getPlatformVersions() {
        return Immutable.of(platformVersions);
    }

    /**
     * Returns true if the developer menu feature is enabled (on debug mode only) and false if it is disabled.
     * @return true if the developer menu feature is enabled (on debug mode only) and false if it is disabled.
     */
    public boolean isDevMenuEnabled() {
        return devMenuEnabled;
    }

    private static Map<String, CategoryConfiguration> configsFromJson(JSONObject json) throws AmplifyException {
        final List<CategoryConfiguration> possibleConfigs = Arrays.asList(
                new AnalyticsCategoryConfiguration(),
                new ApiCategoryConfiguration(),
                new AuthCategoryConfiguration(),
                new DataStoreCategoryConfiguration(),
                new GeoCategoryConfiguration(),
                new HubCategoryConfiguration(),
                new LoggingCategoryConfiguration(),
                new PredictionsCategoryConfiguration(),
                new StorageCategoryConfiguration(),
                new NotificationsCategoryConfiguration()
        );

        final Map<String, CategoryConfiguration> actualConfigs = new HashMap<>();
        try {
            for (CategoryConfiguration possibleConfig : possibleConfigs) {
                String categoryJsonKey = possibleConfig.getCategoryType().getConfigurationKey();
                if (json.has(categoryJsonKey)) {
                    possibleConfig.populateFromJSON(json.getJSONObject(categoryJsonKey));
                    actualConfigs.put(categoryJsonKey, possibleConfig);
                }
            }
        } catch (JSONException error) {
            throw new AmplifyException(
                    "Could not parse amplifyconfiguration.json ",
                    error, "Check any modifications made to the file."
            );
        }
        return Immutable.of(actualConfigs);
    }

    /**
     * Gets the configuration for the specified category type.
     * @param categoryType The category type to return the configuration object for
     * @return Requested category configuration object or an empty CategoryConfiguration
     * object for the given {@link CategoryType}
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public CategoryConfiguration forCategoryType(@NonNull CategoryType categoryType) {
        final CategoryConfiguration categoryConfiguration =
            categoryConfigurations.get(categoryType.getConfigurationKey());

        if (categoryConfiguration == null) {
            return EmptyCategoryConfiguration.forCategoryType(categoryType);
        } else {
            return categoryConfiguration;
        }
    }

    /**
     * Loads a builder for an {@link AmplifyConfiguration} from an amplifyconfiguration.json file.
     *
     * @param context Context needed for reading JSON file
     * @return An Amplify configuration builder instance
     * @throws AmplifyException If there is a problem in the config file
     */
    @NonNull
    public static Builder builder(@NonNull Context context) throws AmplifyException {
        try {
            @RawRes final int resourceId = Resources.getRawResourceId(context, DEFAULT_IDENTIFIER);
            return builder(context, resourceId);
        } catch (Resources.ResourceLoadingException resourceLoadingException) {
            throw new AmplifyException(
                "Failed to load the " + DEFAULT_IDENTIFIER + " configuration file.", resourceLoadingException,
                "Is there an Amplify configuration file present in your app project, under " +
                    "./app/src/main/res/raw/" + DEFAULT_IDENTIFIER + "?"
            );
        }
    }

    /**
     * Loads a builder for an {@link AmplifyConfiguration} from a particular configuration file.
     * @param context Android Context
     * @param configFileResourceId
     *        The Android resource ID of a raw resource which contains
     *        an amplify configuration as JSON
     * @return An Amplify configuration builder instance
     * @throws AmplifyException If there is a problem in the config file
     */
    @NonNull
    public static Builder builder(@NonNull Context context, @RawRes int configFileResourceId)
            throws AmplifyException {
        Objects.requireNonNull(context);
        try {
            return builder(Resources.readJsonResourceFromId(context, configFileResourceId));
        } catch (Resources.ResourceLoadingException resourceLoadingException) {
            throw new AmplifyException(
                "Failed to read JSON from resource = " + configFileResourceId, resourceLoadingException,
                "If you are attempting to load a custom configuration file, please ensure that it exists " +
                    "in your application project under app/src/main/res/raw/<YOUR_CUSTOM_CONFIG_FILE>."
            );
        }
    }

    /**
     * Loads a builder for {@link AmplifyConfiguration} directly from an {@link JSONObject}.
     * Users should prefer loading from resources files via {@link #builder(Context)},
     * or {@link #builder(Context, int)}.
     * @param json A JSON object
     * @return An Amplify configuration builder instance
     * @throws AmplifyException If the JSON does not represent a valid AmplifyConfiguration
     */
    @NonNull
    public static Builder builder(@NonNull JSONObject json) throws AmplifyException {
        return new Builder(configsFromJson(Objects.requireNonNull(json)));
    }

    /**
     * Builder for AmplifyConfiguration with an option to specify additional platforms.
     */
    public static final class Builder {
        private final Map<String, CategoryConfiguration> categoryConfiguration;
        private final Map<UserAgent.Platform, String> platformVersions;
        private boolean devMenuEnabled = false; // Dev menu is disabled by default

        private Builder(Map<String, CategoryConfiguration> categoryConfiguration) {
            this.categoryConfiguration = categoryConfiguration;
            this.platformVersions = new LinkedHashMap<>();
        }

        /**
         * Add an additional platform and its version to be used for tracking
         * usage metrics and return the builder.
         * Note: Do not add "amplify-android", as it is already accounted for.
         * Adding {@link UserAgent.Platform#ANDROID} as platform is a no-op.
         * @param platform Additional platform that uses this library.
         * @param version Version number associated with the additional platform.
         * @return this builder instance.
         */
        @NonNull
        public Builder addPlatform(@NonNull UserAgent.Platform platform, @NonNull String version) {
            // Do not allow user to specify Android platform to prevent redundancy.
            if (!UserAgent.Platform.ANDROID.equals(platform)) {
                this.platformVersions.put(
                        Objects.requireNonNull(platform),
                        Objects.requireNonNull(version)
                );
            }
            return this;
        }

        /**
         * Specifically enable or disable the dev menu. By default it's enabled (debug mode only).
         * @param devMenuEnabled True if you want it enabled, False if you want it disabled.
         * @return this builder instance.
         */
        @NonNull
        public Builder devMenuEnabled(boolean devMenuEnabled) {
            this.devMenuEnabled = devMenuEnabled;
            return this;
        }

        /**
         * Constructs an instance of Amplify configuration object using this builder.
         * @return A fully configured instance of {@link AmplifyConfiguration}.
         */
        @NonNull
        public AmplifyConfiguration build() {
            return new AmplifyConfiguration(
                    categoryConfiguration,
                    platformVersions,
                    devMenuEnabled
            );
        }
    }
}
