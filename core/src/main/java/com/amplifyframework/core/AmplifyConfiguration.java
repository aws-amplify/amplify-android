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
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.analytics.AnalyticsCategoryConfiguration;
import com.amplifyframework.api.ApiCategoryConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.datastore.DataStoreCategoryConfiguration;
import com.amplifyframework.hub.HubCategoryConfiguration;
import com.amplifyframework.logging.LoggingCategoryConfiguration;
import com.amplifyframework.storage.StorageCategoryConfiguration;
import com.amplifyframework.util.Immutable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * AmplifyConfiguration serves as the top-level configuration object for the
 * Amplify framework. It is usually populated from amplifyconfiguration.json.
 * Contains all configurations for all categories and plugins used by system.
 */
public final class AmplifyConfiguration {
    private static final String DEFAULT_IDENTIFIER = "amplifyconfiguration";

    private final Map<String, CategoryConfiguration> categoryConfigurations;

    /**
     * Constructs a new AmplifyConfiguration object.
     * @param configs Category configurations
     */
    @SuppressWarnings("WeakerAccess") // These are created and accessed as public API
    public AmplifyConfiguration(@NonNull Map<String, CategoryConfiguration> configs) {
        this.categoryConfigurations = new HashMap<>();
        this.categoryConfigurations.putAll(configs);
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
        final List<CategoryConfiguration> possibleConfigs = Arrays.asList(
            new AnalyticsCategoryConfiguration(),
            new ApiCategoryConfiguration(),
            new DataStoreCategoryConfiguration(),
            new HubCategoryConfiguration(),
            new LoggingCategoryConfiguration(),
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
        return new AmplifyConfiguration(Immutable.of(actualConfigs));
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
        return fromConfigFile(context, getConfigResourceId(context));
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
        return fromJson(readInputJson(context, configFileResourceId));
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
        final InputStream inputStream =
            context.getResources().openRawResource(resourceId);
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
     * @return Requested category configuration object
     * @throws AmplifyException If there is a problem in the config file
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public CategoryConfiguration forCategoryType(@NonNull CategoryType categoryType) throws AmplifyException {
        final CategoryConfiguration categoryConfiguration =
            categoryConfigurations.get(categoryType.getConfigurationKey());
        if (categoryConfiguration == null) {
            throw new AmplifyException(
                "Unknown/bad category type: " + categoryType,
                "Be sure to use one of the supported Categories in your current version of Amplify"
            );
        }
        return categoryConfiguration;
    }
}

