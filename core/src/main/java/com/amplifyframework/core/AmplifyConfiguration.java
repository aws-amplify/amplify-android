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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * AmplifyConfiguration parses the configuration from the
 * amplifyconfiguration.json file and stores in the in-memory objects
 * for the different Amplify plugins to use.
 */
public final class AmplifyConfiguration {

    private static final String DEFAULT_IDENTIFIER = "amplifyconfiguration";

    private final Map<String, CategoryConfiguration> categoryConfigurations;

    /**
     * Constructs a new AmplifyConfiguration object.
     */
    public AmplifyConfiguration() {
        AnalyticsCategoryConfiguration analytics = new AnalyticsCategoryConfiguration();
        ApiCategoryConfiguration api = new ApiCategoryConfiguration();
        DataStoreCategoryConfiguration dataStore = new DataStoreCategoryConfiguration();
        HubCategoryConfiguration hub = new HubCategoryConfiguration();
        LoggingCategoryConfiguration logging = new LoggingCategoryConfiguration();
        StorageCategoryConfiguration storage = new StorageCategoryConfiguration();

        Map<String, CategoryConfiguration> modifiableCategoryConfigurations = new HashMap<>();
        modifiableCategoryConfigurations.put(analytics.getCategoryType().getConfigurationKey(), analytics);
        modifiableCategoryConfigurations.put(api.getCategoryType().getConfigurationKey(), api);
        modifiableCategoryConfigurations.put(dataStore.getCategoryType().getConfigurationKey(), dataStore);
        modifiableCategoryConfigurations.put(hub.getCategoryType().getConfigurationKey(), hub);
        modifiableCategoryConfigurations.put(logging.getCategoryType().getConfigurationKey(), logging);
        modifiableCategoryConfigurations.put(storage.getCategoryType().getConfigurationKey(), storage);
        categoryConfigurations = Immutable.of(modifiableCategoryConfigurations);
    }

    /**
     * Populates all configuration objects from the amplifyconfiguration.json file.
     * @param context Context needed for reading JSON file
     * @throws AmplifyException If there is a problem in the config file
     */
    public void populateFromConfigFile(Context context) throws AmplifyException {
        populateFromConfigFile(context, getConfigResourceId(context));
    }

    /**
     * Populate the the configuration from a particular configuration file.
     * @param context Android Context
     * @param configFileResourceId
     *        The Android resource ID of a raw resource which contains
     *        an amplify configuration as JSON
     * @throws AmplifyException If there is a problem in the config file
     */
    public void populateFromConfigFile(Context context, @RawRes int configFileResourceId) throws AmplifyException {
        JSONObject json = readInputJson(context, configFileResourceId);

        try {
            for (HashMap.Entry<String, CategoryConfiguration> entry : categoryConfigurations.entrySet()) {
                final String categoryJsonKey = entry.getKey();
                final CategoryConfiguration categoryConfig = entry.getValue();

                if (json.has(categoryJsonKey)) {
                    categoryConfig.populateFromJSON(json.getJSONObject(categoryJsonKey));
                } else if (categoryJsonKey.equals(CategoryType.DATASTORE.getConfigurationKey())) {
                    // CLI currently does not generate configuration for DataStore,
                    // so this is a temporary fix to avoid unexpected PluginException.
                    final String defaultConfig = "{\"plugins\": {\"awsDataStorePlugin\": {}}}";
                    categoryConfig.populateFromJSON(new JSONObject(defaultConfig));
                }
            }
        } catch (JSONException error) {
            throw new AmplifyException(
                    "Could not parse amplifyconfiguration.json ",
                    error,
                    "Check any modifications made to the file."
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
     * @throws AmplifyException If there is a problem in the config file
     */
    public CategoryConfiguration forCategoryType(final CategoryType categoryType) throws AmplifyException {
        if (categoryConfigurations.containsKey(categoryType.getConfigurationKey())) {
            return categoryConfigurations.get(categoryType.getConfigurationKey());
        } else {
            throw new AmplifyException("Unknown/bad category type: " + categoryType,
                    "Be sure to use one of the supported Categories in your current version of Amplify");
        }
    }
}

