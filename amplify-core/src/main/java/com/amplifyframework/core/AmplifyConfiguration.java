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
import com.amplifyframework.hub.HubCategoryConfiguration;
import com.amplifyframework.logging.LoggingCategoryConfiguration;
import com.amplifyframework.storage.StorageCategoryConfiguration;

import org.json.JSONObject;

import java.io.InputStream;
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

    /**
     * Constructs a new AmplifyConfiguration object.
     * @param context The configuration information can be read
     *                from the default amplify configuration file.
     */
    AmplifyConfiguration(Context context) {
        this.analytics = new AnalyticsCategoryConfiguration();
        this.api = new ApiCategoryConfiguration();
        this.hub = new HubCategoryConfiguration();
        this.logging = new LoggingCategoryConfiguration();
        this.storage = new StorageCategoryConfiguration();

        readInputJson(context, getConfigResourceId(context));
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

    private void readInputJson(Context context, int resourceId) {
        try {
            final InputStream inputStream = context.getResources().openRawResource(
                    resourceId);
            final Scanner in = new Scanner(inputStream);
            final StringBuilder sb = new StringBuilder();
            while (in.hasNextLine()) {
                sb.append(in.nextLine());
            }
            in.close();

            JSONObject jsonObject = new JSONObject(sb.toString());
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to read " + DEFAULT_IDENTIFIER + " please check that it is correctly formed.",
                    exception);
        }
    }

    /**
     * Gets the configuration for the storage category.
     * @return Storage category configuration
     */
    public CategoryConfiguration forCategoryType(final CategoryType categoryType) {
        switch (categoryType) {
            case LOGGING:
                return logging;
            case HUB:
                return hub;
            case API:
                return api;
            case ANALYTICS:
                return analytics;
            case STORAGE:
                return storage;
            default:
                throw new ConfigurationException("Uknown/bad category type: " + categoryType);
        }
    }
}

