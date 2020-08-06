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

package com.amplifyframework.core;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.Platform;
import com.amplifyframework.analytics.AnalyticsCategoryConfiguration;
import com.amplifyframework.api.ApiCategoryConfiguration;
import com.amplifyframework.auth.AuthCategoryConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.category.EmptyCategoryConfiguration;
import com.amplifyframework.datastore.DataStoreCategoryConfiguration;
import com.amplifyframework.hub.HubCategoryConfiguration;
import com.amplifyframework.logging.LoggingCategoryConfiguration;
import com.amplifyframework.predictions.PredictionsCategoryConfiguration;
import com.amplifyframework.storage.StorageCategoryConfiguration;
import com.amplifyframework.util.Immutable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

/**
 * AmplifyConfiguration serves as the top-level configuration object for the
 * Amplify framework. It is usually populated from amplifyconfiguration.json.
 * Contains all configurations for all categories and plugins used by system.
 */
public final class AmplifyConfiguration {
    private static final String DEFAULT_IDENTIFIER = "amplifyconfiguration";

    private final Map<String, CategoryConfiguration> categoryConfigurations;
    private final Map<Platform, String> additionalPlatforms;

    /**
     * Constructs a new AmplifyConfiguration object.
     * @param configs Category configurations
     * @deprecated Construction should be done with a builder.
     */
    @Deprecated
    @SuppressWarnings("WeakerAccess") // These are created and accessed as public API
    public AmplifyConfiguration(@NonNull Map<String, CategoryConfiguration> configs) {
        this(configs, new LinkedHashMap<>());
    }

    private AmplifyConfiguration(
            Map<String, CategoryConfiguration> configs,
            Map<Platform, String> additionalPlatforms
    ) {
        this.categoryConfigurations = new HashMap<>();
        this.categoryConfigurations.putAll(configs);
        this.additionalPlatforms = additionalPlatforms;
    }

    /**
     * Build an {@link AmplifyConfiguration} directly from an {@link JSONObject}.
     *
     * @param json A JSON object
     * @return An AmplifyConfiguration
     * @throws AmplifyException If the JSON does not represent a valid AmplifyConfiguration
     * @deprecated This method is now deprecated in favor of {@link #loadJson(JSONObject)}.
     */
    @Deprecated
    @NonNull
    public static AmplifyConfiguration fromJson(@NonNull JSONObject json) throws AmplifyException {
        return loadJson(json).build();
    }

    /**
     * Creates an AmplifyConfiguration from an amplifyconfiguration.json file.
     *
     * @param context Context needed for reading JSON file
     * @return An Amplify configuration instance
     * @throws AmplifyException If there is a problem in the config file
     * @deprecated This method is now deprecated in favor of
     *          {@link AmplifyConfiguration#loadConfigFile(Context)}.
     */
    @SuppressWarnings("WeakerAccess")
    @Deprecated
    @NonNull
    public static AmplifyConfiguration fromConfigFile(@NonNull Context context) throws AmplifyException {
        return loadConfigFile(context, getConfigResourceId(context)).build();
    }

    /**
     * Creates an AmplifyConfiguration from a particular configuration file.
     *
     * @param context Android Context
     * @param configFileResourceId
     *        The Android resource ID of a raw resource which contains
     *        an amplify configuration as JSON
     * @return An Amplify configuration instance
     * @throws AmplifyException If there is a problem in the config file
     * @deprecated This method is now deprecated in favor of
     *          {@link AmplifyConfiguration#loadConfigFile(Context, int)}.
     */
    @Deprecated
    @NonNull
    public static AmplifyConfiguration fromConfigFile(
            @NonNull Context context, @RawRes int configFileResourceId) throws AmplifyException {
        return loadConfigFile(context, configFileResourceId).build();
    }

    /**
     * Obtain a map of additional platforms that are using this library
     * for tracking usage metrics.
     * @return A map of additional platforms and their versions.
     */
    @NonNull
    public Map<Platform, String> getAdditionalPlatforms() {
        return Immutable.of(additionalPlatforms);
    }

    private static Map<String, CategoryConfiguration> configsFromJson(JSONObject json) throws AmplifyException {
        final List<CategoryConfiguration> possibleConfigs = Arrays.asList(
                new AnalyticsCategoryConfiguration(),
                new ApiCategoryConfiguration(),
                new AuthCategoryConfiguration(),
                new DataStoreCategoryConfiguration(),
                new HubCategoryConfiguration(),
                new LoggingCategoryConfiguration(),
                new PredictionsCategoryConfiguration(),
                new StorageCategoryConfiguration()
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

    private static int getConfigResourceId(Context context) throws AmplifyException {
        try {
            return context.getResources()
                .getIdentifier(DEFAULT_IDENTIFIER, "raw", context.getPackageName());
        } catch (Exception exception) {
            throw new AmplifyException(
                "Failed to read " + DEFAULT_IDENTIFIER + ".",
                exception, "Please check that it is correctly formed."
            );
        }
    }

    private static JSONObject readInputJson(Context context, int resourceId) throws AmplifyException {
        InputStream inputStream;

        try {
            inputStream = context.getResources().openRawResource(resourceId);
        } catch (Resources.NotFoundException exception) {
            throw new AmplifyException(
                    "Failed to find " + DEFAULT_IDENTIFIER + ".",
                    exception, "Please check that it has been created."
            );
        }

        final Scanner in = new Scanner(inputStream);
        final StringBuilder sb = new StringBuilder();
        while (in.hasNextLine()) {
            sb.append(in.nextLine());
        }
        in.close();

        try {
            return new JSONObject(sb.toString());
        } catch (JSONException jsonError) {
            throw new AmplifyException(
                "Failed to read " + DEFAULT_IDENTIFIER + ".",
                jsonError, "Please check that it is correctly formed."
            );
        }
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
    public static Builder loadConfigFile(@NonNull Context context) throws AmplifyException {
        return loadConfigFile(context, getConfigResourceId(context));
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
    public static Builder loadConfigFile(
            @NonNull Context context,
            @RawRes int configFileResourceId
    ) throws AmplifyException {
        return loadJson(readInputJson(Objects.requireNonNull(context), configFileResourceId));
    }

    /**
     * Loads a builder for {@link AmplifyConfiguration} directly from an {@link JSONObject}.
     * Users should prefer loading from resources files via {@link #loadConfigFile(Context)},
     * or {@link #loadConfigFile(Context, int)}.
     * @param json A JSON object
     * @return An Amplify configuration builder instance
     * @throws AmplifyException If the JSON does not represent a valid AmplifyConfiguration
     */
    @NonNull
    public static Builder loadJson(@NonNull JSONObject json) throws AmplifyException {
        return loadCategoryConfigs(configsFromJson(Objects.requireNonNull(json)));
    }

    /**
     * Loads a builder for {@link AmplifyConfiguration} directly from a map of
     * {@link CategoryConfiguration}.
     * @param configs A map of category configurations by category name.
     * @return An Amplify configuration builder instance
     */
    @VisibleForTesting
    @NonNull
    public static Builder loadCategoryConfigs(Map<String, CategoryConfiguration> configs) {
        return new Builder(configs);
    }

    /**
     * Builder for AmplifyConfiguration with an option to specify additional platforms.
     */
    public static final class Builder {
        private final Map<String, CategoryConfiguration> categoryConfiguration;
        private final Map<Platform, String> additionalPlatforms;

        private Builder(Map<String, CategoryConfiguration> categoryConfiguration) {
            this.categoryConfiguration = categoryConfiguration;
            this.additionalPlatforms = new LinkedHashMap<>();
        }

        /**
         * Add an additional platform and its version to be used for tracking
         * usage metrics and return the builder.
         * Note: Do not add "amplify-android", as it is already accounted for.
         * @param platform Additional platform that uses this library.
         * @param version Version number associated with the additional platform.
         * @return this builder instance.
         */
        @NonNull
        public Builder withAdditionalPlatform(@NonNull Platform platform, @NonNull String version) {
            this.additionalPlatforms.put(
                    Objects.requireNonNull(platform),
                    Objects.requireNonNull(version)
            );
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
                    additionalPlatforms
            );
        }
    }
}

