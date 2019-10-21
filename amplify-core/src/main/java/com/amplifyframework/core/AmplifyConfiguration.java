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

import com.amplifyframework.ConfigurationException;
import com.amplifyframework.analytics.AnalyticsCategoryConfiguration;
import com.amplifyframework.api.ApiCategoryConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.hub.HubCategoryConfiguration;
import com.amplifyframework.logging.LoggingCategoryConfiguration;
import com.amplifyframework.storage.StorageCategoryConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

/**
 * AmplifyConfiguration parses the configuration from the
 * amplifyconfiguration.json file and stores in the in-memory objects
 * for the different Amplify plugins to use.
 */
final class AmplifyConfiguration {

    private static final String DEFAULT_IDENTIFIER = "amplifyconfiguration";

    private final AnalyticsCategoryConfiguration analytics;
    private final ApiCategoryConfiguration api;
    private final HubCategoryConfiguration hub;
    private final LoggingCategoryConfiguration logging;
    private final StorageCategoryConfiguration storage;
    private final HashMap<String, CategoryConfiguration> categoryConfigurations;

    /**
     * Constructs a new AmplifyConfiguration object.
     */
    AmplifyConfiguration() {
        this.analytics = new AnalyticsCategoryConfiguration();
        this.api = new ApiCategoryConfiguration();
        this.hub = new HubCategoryConfiguration();
        this.logging = new LoggingCategoryConfiguration();
        this.storage = new StorageCategoryConfiguration();

        categoryConfigurations = new HashMap<>();
        categoryConfigurations.put(analytics.getCategoryType().getConfigurationKey(), analytics);
        categoryConfigurations.put(api.getCategoryType().getConfigurationKey(), api);
        categoryConfigurations.put(hub.getCategoryType().getConfigurationKey(), hub);
        categoryConfigurations.put(logging.getCategoryType().getConfigurationKey(), logging);
        categoryConfigurations.put(storage.getCategoryType().getConfigurationKey(), storage);
    }

    /**
     * Populates all configuration objects from the amplifyconfiguration.json file.
     * @param context Context needed for reading JSON file
     */
    public void populateFromConfigFile(Context context) {
        JSONObject json = readInputJson(context, getConfigResourceId(context));

        try {
            for (HashMap.Entry<String, CategoryConfiguration> entry : categoryConfigurations.entrySet()) {
                final String categoryJsonKey = entry.getKey();
                final CategoryConfiguration categoryConfig = entry.getValue();

                if (json.has(categoryJsonKey)) {
                    categoryConfig.populateFromJSON(json.getJSONObject(categoryJsonKey));
                }
            }
        } catch (JSONException error) {
            throw new PluginException.PluginConfigurationException(
                    "Could not parse amplifyconfiguration.json - check any modifications made to the file.",
                    error
            );
        }
    }

    private static int getConfigResourceId(Context context) {
        try {
            return context.getResources().getIdentifier(DEFAULT_IDENTIFIER,
                    "raw", context.getPackageName());
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to read " + DEFAULT_IDENTIFIER
                            + " please check that it is correctly formed.",
                    exception);
        }
    }

    private JSONObject readInputJson(Context context, int resourceId) {
        try {
            final InputStream inputStream = context.getResources().openRawResource(
                    resourceId);
            final Scanner in = new Scanner(inputStream);
            final StringBuilder sb = new StringBuilder();
            while (in.hasNextLine()) {
                sb.append(in.nextLine());
            }
            in.close();

            return new JSONObject(sb.toString());
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to read " + DEFAULT_IDENTIFIER + " please check that it is correctly formed.",
                    exception);
        }
    }

    /**
     * Gets the configuration for the specified category type.
     * @param categoryType The category type to return the configuration object for
     * @return Requested category configuration object
     */
    public CategoryConfiguration forCategoryType(final CategoryType categoryType) {
        if (categoryConfigurations.containsKey(categoryType.getConfigurationKey())) {
            return categoryConfigurations.get(categoryType.getConfigurationKey());
        } else {
            throw new ConfigurationException("Unknown/bad category type: " + categoryType);
        }
    }
}

